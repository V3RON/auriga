package pl.aitwar.auriga.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import pl.aitwar.auriga.utils.Routing;

public class ConfigurationModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ConfigurationController.class);
        Multibinder.newSetBinder(binder(), Routing.class).addBinding().to(ConfigurationRouting.class);
    }
}
