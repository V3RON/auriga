package pl.aitwar.auriga;

import com.google.inject.AbstractModule;
import io.javalin.Javalin;
import org.jetbrains.annotations.NotNull;
import pl.aitwar.auriga.collection.CollectionModule;
import pl.aitwar.auriga.configuration.ConfigurationModule;

public class AppModule extends AbstractModule {
    private final Javalin app;

    private AppModule(Javalin app) {
        this.app = app;
    }

    @NotNull
    public static AppModule create() {
        return new AppModule(Javalin.create());
    }

    protected void configure() {
        bind(Javalin.class).toInstance(app);
        bind(Startup.class);
        install(new CollectionModule());
        install(new ConfigurationModule());
    }
}
