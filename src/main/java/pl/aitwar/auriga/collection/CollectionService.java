package pl.aitwar.auriga.collection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.aitwar.auriga.collection.model.CollectionDescriptor;
import pl.aitwar.auriga.collection.model.exceptions.CollectionAlreadyExistsException;
import pl.aitwar.auriga.collection.model.exceptions.DocumentAllocationException;
import pl.aitwar.auriga.collection.model.exceptions.UnknownCollectionException;
import pl.aitwar.auriga.nodes.NodesService;
import pl.aitwar.auriga.nodes.model.Node;
import pl.aitwar.auriga.nodes.model.NodeUsageMetric;
import pl.aitwar.auriga.utils.eventbus.Event;
import pl.aitwar.auriga.utils.eventbus.EventBus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Singleton
public class CollectionService {
    private static final Logger logger = LoggerFactory.getLogger(NodesService.class);
    private final NodesService nodesService;
    private final EventBus eventBus;
    private Map<String, CollectionDescriptor> collectionDescriptors = new HashMap<>();

    @Inject
    public CollectionService(NodesService nodesService, EventBus eventBus) {
        this.nodesService = nodesService;
        this.eventBus = eventBus;

        setUp();
    }

    public Collection<CollectionDescriptor> getCollections() {
        return collectionDescriptors.values();
    }

    public void createCollection(final String collectionName, final int replication) throws CollectionAlreadyExistsException {
        logger.info("Creating collection '{}' with replication level '{}'", collectionName, replication);
        Objects.requireNonNull(collectionName);

        if (replication == 0) {
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
    public CompletableFuture<Set<String>> putDocument(final String collectionName, final String document) {
        logger.info("Putting document in collection '{}'", collectionName);
        Objects.requireNonNull(collectionName);
        Objects.requireNonNull(document);

        if (!collectionDescriptors.containsKey(collectionName)) {
            logger.warn("Collection '{}' not found", collectionName);
            try {
                createCollection(collectionName, 1);
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

            return nodesService.getFreeNode()
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

        final Set<String> nodesNames = collectionDescriptors.get(collectionName).getContainingNodesNames();

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
                        .map(address -> "http://" + address + ":7000/collections/" + collectionName)
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
                .uri(URI.create("http://" + address + ":7000/collections/" + collectionName))
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

    private void setUp() {
        eventBus.listen(Event.NODE_REM, payload -> {
            String nodeName = (String) payload;

            collectionDescriptors.values().forEach(descriptor -> {
                if (descriptor.removeNode(nodeName)) {
                    descriptor.setCurrentReplicationLevel(descriptor.getCurrentReplicationLevel() - 1);
                }
            });
        });
    }
}
