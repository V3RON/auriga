package pl.aitwar.auriga.configuration;

import com.google.inject.Singleton;
import io.javalin.Context;

@Singleton
public class ConfigurationController {
    public void heartbeat(Context context) {
        context.status(200);
    }
}
