package pl.aitwar.auriga.nodes;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import pl.aitwar.auriga.utils.Routing;

public class NodesModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(NodesController.class);
        bind(NodesService.class);
        Multibinder.newSetBinder(binder(), Routing.class).addBinding().to(NodesRouting.class);
    }
}
