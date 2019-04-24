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
        System.out.println(
                "\u001b[32m\n    _           _           \n" +
                        "   /_\\ _  _ _ _(_)__ _ __ _ \n" +
                        "  / _ \\ || | '_| / _` / _` |\n" +
                        " /_/ \\_\\_,_|_| |_\\__, \\__,_|\n" +
                        "                 |___/      \n\u001b[0m"
        );

        bindRoutes();
        app.port(8000);
        app.start();
    }

    private void bindRoutes() {
        routes.forEach(Routing::bindRoutes);
    }
}
