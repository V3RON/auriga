package pl.aitwar.auriga;

import com.google.inject.Guice;

public class App {
    public static void main(String[] args) {
        var injector = Guice.createInjector(AppModule.create());
        injector.getInstance(Startup.class).boot(args);
    }
}
