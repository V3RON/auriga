package pl.aitwar.auriga.collection;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import pl.aitwar.auriga.utils.Routing;

public class CollectionModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CollectionController.class);
        bind(CollectionService.class);
        Multibinder.newSetBinder(binder(), Routing.class).addBinding().to(CollectionRouting.class);
    }
}
