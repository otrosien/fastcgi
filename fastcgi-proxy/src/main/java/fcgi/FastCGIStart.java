package fcgi;

import com.epages.plugin.PluginServiceLoaderFactory;
import com.epages.server.EmbeddedServer;
import com.google.inject.Injector;

public class FastCGIStart {

    public static void main(String[] args) {
        Injector injector = PluginServiceLoaderFactory.getInjector();

        // Server
        EmbeddedServer server = injector.getInstance(EmbeddedServer.class);
        server.start();
    }

}
