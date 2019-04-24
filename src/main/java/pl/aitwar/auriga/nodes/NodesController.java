package pl.aitwar.auriga.nodes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.javalin.Context;
import io.javalin.apibuilder.CrudHandler;
import org.jetbrains.annotations.NotNull;
import pl.aitwar.auriga.nodes.model.NodeRegistration;
import pl.aitwar.auriga.nodes.model.exceptions.NodeAlreadyRegisteredException;
import pl.aitwar.auriga.nodes.model.exceptions.UnknownNodeException;

import java.io.IOException;

@Singleton
public class NodesController implements CrudHandler {
    private final NodesService nodesService;
    private final ObjectMapper objectMapper;

    @Inject
    public NodesController(NodesService nodesService, ObjectMapper objectMapper) {
        this.nodesService = nodesService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void create(@NotNull Context context) {
        try {
            NodeRegistration registration = objectMapper.readValue(context.body(), NodeRegistration.class);
            nodesService.registerNode(registration);
            context.status(200);
        } catch (IOException e) {
            context.status(400);
        } catch (NodeAlreadyRegisteredException e) {
            context.status(409);
        }
    }

    @Override
    public void delete(@NotNull Context context, @NotNull String nodeName) {
        try {
            nodesService.forgetNode(nodeName);
            context.status(200);
        } catch (UnknownNodeException e) {
            context.status(400);
        }
    }

    @Override
    public void getAll(@NotNull Context context) {
        try {
            String responseBody = objectMapper.writeValueAsString(nodesService.getNodes());
            context.result(responseBody);
            context.status(200);
        } catch (JsonProcessingException e) {
            // Eat it!
            context.status(500);
        }
    }

    @Override
    public void getOne(@NotNull Context context, @NotNull String s) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public void update(@NotNull Context context, @NotNull String nodeName) {
        try {
            NodeRegistration registration = objectMapper.readValue(context.body(), NodeRegistration.class);

            if (!nodeName.equals(registration.getName())) {
                // UNCOSISTENCY in naming!
                context.status(400);
                return;
            }

            nodesService.updateNode(registration);
            context.status(200);
        } catch (IOException e) {
            context.status(400);
        } catch (UnknownNodeException e) {
            context.status(404);
        }
    }
}
