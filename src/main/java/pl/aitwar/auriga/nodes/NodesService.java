package pl.aitwar.auriga.nodes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.aitwar.auriga.nodes.model.NodeRegistration;
import pl.aitwar.auriga.nodes.model.NodeUsageMetric;
import pl.aitwar.auriga.nodes.model.exceptions.NoFreeNodeException;
import pl.aitwar.auriga.nodes.model.exceptions.NodeAlreadyRegisteredException;
import pl.aitwar.auriga.nodes.model.exceptions.UnknownNodeException;
import pl.aitwar.auriga.utils.eventbus.Event;
import pl.aitwar.auriga.utils.eventbus.EventBus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Singleton
public class NodesService {
    private static final Logger logger = LoggerFactory.getLogger(NodesService.class);
    private final Map<String, String> nodeAddresses = new HashMap<>();
    private final ObjectMapper objectMapper;
    private final EventBus eventBus;

    @Inject
    public NodesService(ObjectMapper objectMapper, EventBus eventBus) {
        this.objectMapper = objectMapper;
        this.eventBus = eventBus;

        setUp();
    }

    public Set<String> getNodes() {
        return this.nodeAddresses.keySet();
    }

    @NotNull
    public String getNodeAddress(String nodeName) {
        return nodeAddresses.get(nodeName);
    }

    @NotNull
    public CompletableFuture<NodeUsageMetric> getNodeUsage(final String name) {
        logger.info("Getting usage metric of '{}' node", name);
        Objects.requireNonNull(name);
        String address = nodeAddresses.get(name);

        if (address == null) {
            return CompletableFuture.failedFuture(new UnknownNodeException(name));
        }

        HttpClient client = HttpClient.newBuilder()
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(address + "/status"))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return objectMapper.readValue(body, NodeUsageMetric.class);
                    } catch (IOException e) {
                        throw new CompletionException(e.getCause());
                    }
                }).thenApply(metric -> {
                    logger.debug("Received '{}' metric", metric);
                    return metric;
                });
    }

    public void registerNode(final NodeRegistration nodeRegistration) throws NodeAlreadyRegisteredException {
        final String name = nodeRegistration.getName();
        final String address = nodeRegistration.getAddress();

        logger.info("Registering '{}' node", name);
        Objects.requireNonNull(name);
        Objects.requireNonNull(address);

        if (nodeAddresses.containsKey(name)) {
            logger.warn("Node already registered exception");
            throw new NodeAlreadyRegisteredException(name);
        }

        nodeAddresses.put(name, address);
        eventBus.publish(Event.NODE_ADD, name);
    }

    public void updateNode(final NodeRegistration nodeRegistration) throws UnknownNodeException {
        final String name = nodeRegistration.getName();
        final String address = nodeRegistration.getAddress();

        logger.info("Updating '{}' node", name);
        Objects.requireNonNull(name);
        Objects.requireNonNull(address);

        if (!nodeAddresses.containsKey(name)) {
            logger.warn("Unknown node exception");
            throw new UnknownNodeException(name);
        }

        nodeAddresses.put(name, address);
    }

    @NotNull
    public CompletableFuture<NodeUsageMetric> getFreeNode() {
        logger.info("Searching for empty node");

        return nodeAddresses.keySet()
                .stream()
                .map(this::getNodeUsage)
                .map(CompletableFuture::join)
                .filter(NodeUsageMetric::isEmpty)
                .min(Comparator.comparingDouble(NodeUsageMetric::getLoad))
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> CompletableFuture.failedFuture(new NoFreeNodeException()));
    }

    public void forgetNode(final String name) throws UnknownNodeException {
        logger.info("Unregistering '{}' node", name);
        Objects.requireNonNull(name);

        if (!nodeAddresses.containsKey(name)) {
            throw new UnknownNodeException(name);
        }

        nodeAddresses.remove(name);
        eventBus.publish(Event.NODE_REM, name);
    }

    @NotNull
    public CompletableFuture<Void> checkNodes() {
        logger.info("Checking nodes status");

        return CompletableFuture.runAsync(() -> {
            Set<String> livingOnes = nodeAddresses.keySet()
                    .stream()
                    .map(address -> {
                        try {
                            return getNodeUsage(address).get();
                        } catch (Exception ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(NodeUsageMetric::getName)
                    .collect(Collectors.toSet());

            logger.info("{} nodes found dead", nodeAddresses.size() - livingOnes.size());

            nodeAddresses.keySet()
                    .stream()
                    .filter(Predicate.not(livingOnes::contains))
                    .forEach(nodeName -> {
                        try {
                            forgetNode(nodeName);
                        } catch (UnknownNodeException e) {
                            // Eat it!
                        }
                    });
        });
    }

    private void setUp() {
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
        ex.scheduleAtFixedRate(this::checkNodes, 0, 60, TimeUnit.SECONDS);
    }
}
