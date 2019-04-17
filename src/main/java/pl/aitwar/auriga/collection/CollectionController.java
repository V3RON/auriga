package pl.aitwar.auriga.collection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.javalin.Context;

@Singleton
public class CollectionController {
    private final CollectionLocator collectionLocator;

    @Inject
    public CollectionController(CollectionLocator collectionLocator) {
        this.collectionLocator = collectionLocator;
    }

    public void index(Context context) {
        context.result("Test");
    }
}
