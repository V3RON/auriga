package pl.aitwar.auriga.collection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.javalin.Javalin;
import pl.aitwar.auriga.utils.Routing;

import static io.javalin.apibuilder.ApiBuilder.*;

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
            get("collections", ctx -> getController().getAll(ctx));
            get("collections/:collection-name", ctx -> getController().getOne(ctx, ctx.pathParam("collection-name")));
            post("collections/:collection-name", ctx -> getController().create(ctx, ctx.pathParam("collection-name")));
            delete("collections/:collection-name", ctx -> getController().delete(ctx, ctx.pathParam("collection-name")));
        });
    }
}
