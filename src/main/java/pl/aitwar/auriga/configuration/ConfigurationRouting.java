package pl.aitwar.auriga.configuration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.javalin.Javalin;
import pl.aitwar.auriga.utils.Routing;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

@Singleton
public class ConfigurationRouting extends Routing<ConfigurationController> {
    private final Javalin javalin;

    @Inject
    public ConfigurationRouting(Javalin javalin) {
        this.javalin = javalin;
    }

    @Override
    public void bindRoutes() {
        javalin.routes(() -> {
            path("", () -> get(ctx -> getController().heartbeat(ctx)));
        });
    }
}
