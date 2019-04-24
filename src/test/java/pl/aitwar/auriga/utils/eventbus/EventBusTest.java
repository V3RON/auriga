package pl.aitwar.auriga.utils.eventbus;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.function.Consumer;

import static org.junit.Assert.*;

public class EventBusTest {
    private static EventBus eventBus;

    @BeforeClass
    public static void setUp() {
        eventBus = new EventBus();
    }

    @Before
    public void cleanUp() {
        eventBus.clear();
    }

    @Test
    public void shouldInformListenerAboutEvent() {
        eventBus.listen(Event.values()[0], payload -> {
            assertNotNull(payload);
            assertEquals(payload, "TEST");
        });

        eventBus.listen(Event.values()[1], payload -> {
            fail();
        });

        eventBus.publish(Event.values()[0], "TEST");
    }

    @Test
    public void shouldClearItself() {
        eventBus.listen(Event.values()[0], payload -> fail());
        eventBus.listen(Event.values()[1], payload -> fail());

        eventBus.clear();

        eventBus.publish(Event.values()[0], "TEST");
        eventBus.publish(Event.values()[1], "TEST2");
    }

    @Test
    public void shouldBeForgottenOnRequest() {
        Consumer<Object> consumer = payload -> fail();

        eventBus.listen(Event.values()[0], consumer);
        eventBus.forget(Event.values()[0], consumer);
        eventBus.publish(Event.values()[0], "TEST");
    }
}
