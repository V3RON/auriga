package pl.aitwar.auriga.collection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.javalin.Javalin;
import pl.aitwar.auriga.utils.Routing;

@Singleton
public class CollectionRouting extends Routing<CollectionController> {
    private final Javalin javalin;

    @Inject
    public CollectionRouting(Javalin javalin) {
        this.javalin = javalin;
    }

    @Override
    public void bindRoutes() {
        javalin.routes(() -> {
        });
    }
}
