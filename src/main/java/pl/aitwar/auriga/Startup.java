package pl.aitwar.auriga;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.javalin.Javalin;
import pl.aitwar.auriga.utils.Routing;

import java.util.Collections;
import java.util.Set;

@Singleton
public class Startup {
    private final Javalin app;

    @Inject(optional = true)
    private Set<Routing> routes = Collections.emptySet();

    @Inject
    public Startup(Javalin app) {
        this.app = app;
    }

    public void boot(String[] args) {
        bindRoutes();
        app.port(7000);
        app.start();
    }

    private void bindRoutes() {
        routes.forEach(Routing::bindRoutes);
    }
}
