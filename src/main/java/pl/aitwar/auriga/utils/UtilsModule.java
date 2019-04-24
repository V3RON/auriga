package pl.aitwar.auriga.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import pl.aitwar.auriga.utils.eventbus.EventBus;

public class UtilsModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class).in(Singleton.class);
        bind(EventBus.class);
    }
}
