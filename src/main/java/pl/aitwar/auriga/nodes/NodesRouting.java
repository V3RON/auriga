package pl.aitwar.auriga.nodes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.javalin.Javalin;
import pl.aitwar.auriga.utils.Routing;

import static io.javalin.apibuilder.ApiBuilder.crud;

@Singleton
public class NodesRouting extends Routing<NodesController> {
    private final Javalin javalin;

    @Inject
    public NodesRouting(Javalin javalin) {
        this.javalin = javalin;
    }

    @Override
    public void bindRoutes() {
        javalin.routes(() -> {
            crud("nodes/:node-id", getController());
        });
    }
}
