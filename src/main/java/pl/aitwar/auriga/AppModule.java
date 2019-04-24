package pl.aitwar.auriga;

import com.google.inject.AbstractModule;
import io.javalin.Javalin;
import org.jetbrains.annotations.NotNull;
import pl.aitwar.auriga.collection.CollectionModule;
import pl.aitwar.auriga.configuration.ConfigurationModule;
import pl.aitwar.auriga.nodes.NodesModule;
import pl.aitwar.auriga.utils.UtilsModule;

public class AppModule extends AbstractModule {
    private final Javalin app;

    private AppModule(Javalin app) {
        this.app = app;
    }

    @NotNull
    public static AppModule create() {
        Javalin app = Javalin.create()
                .defaultContentType("application/json");

        return new AppModule(app);
    }

    protected void configure() {
        bind(Javalin.class).toInstance(app);
        bind(Startup.class);
        install(new UtilsModule());
        install(new CollectionModule());
        install(new NodesModule());
        install(new ConfigurationModule());
    }
}
