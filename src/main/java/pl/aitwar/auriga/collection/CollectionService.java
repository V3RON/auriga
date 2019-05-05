package pl.aitwar.auriga.collection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.aitwar.auriga.collection.model.CollectionCopyRequest;
import pl.aitwar.auriga.collection.model.CollectionDescriptor;
import pl.aitwar.auriga.collection.model.exceptions.CollectionAlreadyExistsException;
import pl.aitwar.auriga.collection.model.exceptions.CollectionBlockedException;
import pl.aitwar.auriga.collection.model.exceptions.DocumentAllocationException;
import pl.aitwar.auriga.collection.model.exceptions.UnknownCollectionException;
import pl.aitwar.auriga.nodes.NodesService;
import pl.aitwar.auriga.nodes.model.Node;
import pl.aitwar.auriga.nodes.model.NodeUsageMetric;
import pl.aitwar.auriga.utils.eventbus.Event;
import pl.aitwar.auriga.utils.eventbus.EventBus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Singleton
public class CollectionService {
    private static final Logger logger = LoggerFactory.getLogger(NodesService.class);
    private final NodesService nodesService;
    private final EventBus eventBus;
    private final ObjectMapper objectMapper;
    private Map<String, CollectionDescriptor> collectionDescriptors = new HashMap<>();

    @Inject
    public CollectionService(NodesService nodesService, EventBus eventBus, ObjectMapper objectMapper) {
        this.nodesService = nodesService;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;

        setUp();
    }

    public Collection<CollectionDescriptor> getCollections() {
        return collectionDescriptors.values();
    }

    public void createCollection(final String collectionName, final int replication) throws CollectionAlreadyExistsException {
        logger.info("Creating collection '{}' with replication level '{}'", collectionName, replication);
        Objects.requireNonNull(collectionName);

        if (replication <= 0) {
            throw new IllegalArgumentException("Replication level must be greater or equal to 1");
        }

        if (collectionDescriptors.containsKey(collectionName)) {
            logger.warn("Collection '{}' already exists", collectionName);
            throw new CollectionAlreadyExistsException(collectionName);
        }

        CollectionDescriptor descriptor = CollectionDescriptor
                .builder()
                .name(collectionName)
                .currentReplicationLevel(0)
                .containingNodesNames(new HashSet<>())
                .targetReplicationLevel(replication)
                .build();

        collectionDescriptors.put(collectionName, descriptor);
    }

    @NotNull
    public CompletableFuture<Set<String>> putDocument(final String collectionName, final String document, Integer replication) {
        logger.info("Putting document in collection '{}'", collectionName);
        Objects.requireNonNull(collectionName);
        Objects.requireNonNull(document);

        if (!collectionDescriptors.containsKey(collectionName)) {
            logger.warn("Collection '{}' not found", collectionName);
            try {
                createCollection(collectionName, replication);
            } catch (CollectionAlreadyExistsException e) {
                // Eat it!
            }
        }

        final CollectionDescriptor descriptor = collectionDescriptors.get(collectionName);
        final Set<Node> containingNodes = descriptor.getContainingNodesNames()
                .stream()
                .map(nodesService::getNode)
                .collect(Collectors.toSet());

        if (containingNodes.isEmpty()) {
            logger.info("Putting collection '{}' in first free node", collectionName);

            return nodesService.getFreeNode(null)
                    .thenCompose(nodeUsageMetric -> putDocumentInNode(nodesService.getNode(nodeUsageMetric.getName()), collectionName, document))
                    .thenApply(Set::of);
        }

        logger.info("Putting collection '{}' in all containing nodes", collectionName);

        final CompletableFuture[] nodesFutures = containingNodes
                .stream()
                .map(node -> putDocumentInNode(node, collectionName, document))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(nodesFutures)
                .thenApply(future -> Arrays.stream(nodesFutures).map(CompletableFuture<String>::join).collect(Collectors.toSet()));
    }

    @NotNull
    public CompletableFuture<String> getCollectionAddress(final String collectionName) {
        if (!collectionDescriptors.containsKey(collectionName)) {
            return CompletableFuture.failedFuture(new UnknownCollectionException(collectionName));
        }

        final CollectionDescriptor descriptor = collectionDescriptors.get(collectionName);
        if (descriptor.isBlocked()) {
            return CompletableFuture.failedFuture(new CollectionBlockedException(collectionName));
        }

        final Set<String> nodesNames = descriptor.getContainingNodesNames();

        @SuppressWarnings("unchecked") final CompletableFuture<NodeUsageMetric>[] futures = nodesNames
                .stream()
                .map(nodesService::getNodeUsage)
                .distinct()
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures)
                .thenApply(future -> Arrays.stream(futures).map(CompletableFuture::join).collect(Collectors.toSet()))
                .thenApply(results -> results.stream().min(Comparator.comparingDouble(NodeUsageMetric::getLoad)))
                .thenCompose(candidate -> candidate
                        .map(NodeUsageMetric::getAddress)
                        .map(address -> "http://" + address + "/collections/" + collectionName)
                        .map(CompletableFuture::completedFuture)
                        .orElseGet(() -> CompletableFuture.failedFuture(new UnknownCollectionException(collectionName)))
                );
    }

    @NotNull
    private CompletableFuture<String> putDocumentInNode(final Node node, final String collectionName, final String document) {
        logger.info("Putting document of collection '{}' in node '{}'", collectionName, node.getName());
        Objects.requireNonNull(node);
        Objects.requireNonNull(collectionName);
        Objects.requireNonNull(document);

        final String address = node.getAddress();
        final String nodeName = node.getName();

        HttpClient client = HttpClient.newBuilder()
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + address + "/collections/" + collectionName))
                .POST(HttpRequest.BodyPublishers.ofString(document))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() != 201) {
                        return CompletableFuture.failedFuture(new DocumentAllocationException(collectionName, nodeName));
                    }

                    return CompletableFuture.completedFuture(nodeName);
                }).thenApply(stub -> {
                    CollectionDescriptor descriptor = collectionDescriptors.get(collectionName);
                    if (!descriptor.getContainingNodesNames().contains(nodeName)) {
                        descriptor.setCurrentReplicationLevel(descriptor.getCurrentReplicationLevel() + 1);
                        descriptor.getContainingNodesNames().add(nodeName);
                    }
                    return stub;
                });
    }

    @NotNull
    public CompletableFuture<Void> deleteCollection(final String collectionName) {
        logger.info("Deleting collection '{}'", collectionName);
        Objects.requireNonNull(collectionName);

        if (!collectionDescriptors.containsKey(collectionName)) {
            logger.warn("Collection '{}' not found", collectionName);
        }

        Set<Node> containingNodes = collectionDescriptors.get(collectionName).getContainingNodesNames()
                .stream()
                .map(nodesService::getNode)
                .collect(Collectors.toSet());

        final CompletableFuture[] nodesFutures = containingNodes
                .stream()
                .map(node -> deleteCollectionFromNode(collectionName, node))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(nodesFutures)
                .thenAccept(future -> Arrays.stream(nodesFutures).forEach(CompletableFuture<String>::join))
                .thenAccept(nothing -> collectionDescriptors.remove(collectionName));
    }

    @NotNull
    public CompletableFuture<Void> copyCollectionToNode(final String collectionName, final String nodeName) {
        logger.info("Copying collection '{}' to node '{}'", collectionName, nodeName);
        Objects.requireNonNull(collectionName);
        Objects.requireNonNull(nodeName);

        if (!collectionDescriptors.containsKey(collectionName)) {
            logger.warn("Collection '{}' not found", collectionName);
        }

        final CollectionDescriptor descriptor = collectionDescriptors.get(collectionName);
        final Node node = nodesService.getNode(nodeName);
        final Node origin = nodesService.getNode(descriptor.getContainingNodesNames().iterator().next());

        final CollectionCopyRequest copyRequest = new CollectionCopyRequest(collectionName, "http://" + origin.getAddress());

        String body = "";
        try {
            body = objectMapper.writeValueAsString(copyRequest);
        } catch (JsonProcessingException e) {
            // Eat it!
        }

        HttpClient client = HttpClient.newBuilder()
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + node.getAddress() + "/collections/" + collectionName + "/copy"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        descriptor.setBlocked(true);

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    logger.info("Collection '{}' successfully copied to node '{}'", collectionName, nodeName);
                    descriptor.setBlocked(false);
                    descriptor.getContainingNodesNames().add(node.getName());
                    descriptor.setCurrentReplicationLevel(descriptor.getCurrentReplicationLevel() + 1);
                    return null;
                });
    }

    @NotNull
    private CompletableFuture<Void> deleteCollectionFromNode(final String collectionName, final Node node) {
        logger.info("Deleting collection '{}' from node '{}'", collectionName, node.getName());
        Objects.requireNonNull(collectionName);
        Objects.requireNonNull(node);

        if (!collectionDescriptors.containsKey(collectionName)) {
            logger.warn("Collection '{}' not found", collectionName);
        }

        final String address = node.getAddress();

        HttpClient client = HttpClient.newBuilder()
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + address + "/collections/" + collectionName))
                .DELETE()
                .build();

        // TODO: Check for failures
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(req -> {
        });
    }

    private void checkReplicationStatus() {
        collectionDescriptors
                .values()
                .stream()
                .filter(Predicate.not(CollectionDescriptor::isFullyReplicated))
                .forEach(descriptor -> {
                    logger.info("Collection '{}' is trying to be replicated", descriptor.getName());
                    nodesService.getFreeNode(descriptor.getContainingNodesNames())
                            .thenAccept(freeNodeUsage -> copyCollectionToNode(descriptor.getName(), freeNodeUsage.getName()));
                });
    }

    private void loadCollectionDatabase() {
        logger.info("Reading collection database from 'collections.json' file");
        try {
            collectionDescriptors = objectMapper.readValue(new File("collections.json"), new TypeReference<Map<String, CollectionDescriptor>>() {
            });
        } catch (IOException e) {
            logger.error("Failed to read collection database");
        }
    }

    private void saveCollectionDatabase() {
        logger.info("Saving collection database to 'collections.json' file");
        try {
            FileWriter fileWriter = new FileWriter("collections.json");
            String fileBody = objectMapper.writeValueAsString(collectionDescriptors);
            fileWriter.write(fileBody);
            fileWriter.close();
        } catch (Exception e) {
            logger.error("Failed to save collection database");
        }
    }

    private void setUp() {
        eventBus.listen(Event.NODE_REM, payload -> {
            String nodeName = (String) payload;

            collectionDescriptors.values().forEach(descriptor -> {
                if (descriptor.removeNode(nodeName)) {
                    descriptor.setCurrentReplicationLevel(descriptor.getCurrentReplicationLevel() - 1);
                    logger.info("Collection '{}' current replication level dropped to '{}'", descriptor.getName(),
                            descriptor.getCurrentReplicationLevel());
                }
            });
        });

        loadCollectionDatabase();

        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
        ex.scheduleAtFixedRate(this::checkReplicationStatus, 20, 20, TimeUnit.SECONDS);
        ex.scheduleAtFixedRate(this::saveCollectionDatabase, 20, 20, TimeUnit.SECONDS);
    }
}
