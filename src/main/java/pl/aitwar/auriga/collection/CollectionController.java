package pl.aitwar.auriga.collection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.javalin.Context;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.aitwar.auriga.collection.model.exceptions.CollectionBlockedException;
import pl.aitwar.auriga.collection.model.exceptions.UnknownCollectionException;
import pl.aitwar.auriga.nodes.model.exceptions.NoFreeNodeException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Singleton
public class CollectionController {
    private static final Logger logger = LoggerFactory.getLogger(CollectionController.class);

    private final CollectionService collectionService;
    private final ObjectMapper objectMapper;

    @Inject
    public CollectionController(CollectionService collectionService, ObjectMapper objectMapper) {
        this.collectionService = collectionService;
        this.objectMapper = objectMapper;
    }

    public void create(@NotNull Context context, @NotNull String collectionName) {
        try {
            objectMapper.readTree(context.body());
        } catch (JsonProcessingException ex) {
            context.status(400);
        } catch (IOException e) {
            context.status(500);
        }

        context.status(201);

        String replicationLevelParam = context.queryParam("replication");
        Integer replicationLevel = 1;

        if (replicationLevelParam != null) {
            try {
                replicationLevel = Integer.valueOf(replicationLevelParam);
            } catch (Exception e) {
                context.status(400);
                return;
            }
        }

        collectionService.putDocument(collectionName, context.body(), replicationLevel)
                .exceptionally(ex -> {
                    if (ex.getCause() instanceof NoFreeNodeException) {
                        context.status(503);
                    }
                    return null;
                });
    }

    public void delete(@NotNull Context context, @NotNull String s) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    public void getAll(@NotNull Context context) {
        try {
            String responseBody = objectMapper.writeValueAsString(collectionService.getCollections());
            context.result(responseBody);
            context.status(200);
        } catch (JsonProcessingException e) {
            // Eat it!
            context.status(500);
        }
    }

    public void getOne(@NotNull Context context, @NotNull String collectionName) {
        try {
            collectionService.getCollectionAddress(collectionName)
                    .thenAccept(context::redirect)
                    .exceptionally(exp -> {
                        if (exp.getCause() instanceof UnknownCollectionException) {
                            context.status(404);
                        } else if (exp.getCause() instanceof CollectionBlockedException) {
                            logger.warn("Collection '{}' is being fetched at the moment", collectionName);
                            context.status(503);
                        } else {
                            context.status(500);
                        }
                        return null;
                    }).get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Collection locating interrupted '{}'", e.getMessage());
        }
    }

    public void update(@NotNull Context context, @NotNull String s) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }
}
