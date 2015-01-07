package fcgi;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;

import com.epages.epagesj.EPagesJServerBasePlugin;
import com.epages.plugin.ServletPlugin;
import com.epages.server.EmbeddedServer;
import com.epages.server.config.ServerConfiguration;
import com.google.inject.multibindings.Multibinder;

public class FastCGIPlugin extends ServletPlugin {

    @Override
    public void configure() {
        // server config
        bind(EmbeddedServer.class).to(FastCGIServer.class);
        bind(ServerConfiguration.class).to(FastCGIServerConfiguration.class);
        bind(FastCGIConfiguration.class).to(FastCGIConfigurationImpl.class);
        bindToDefaultConfig("conf/fcgi-default.conf");

        Multibinder<ServletHolder> servletHolderMultibinder = Multibinder.newSetBinder(binder(), ServletHolder.class);
        servletHolderMultibinder.addBinding().toProvider(FastCGIServletHolderProvider.class);

        // install basic bindings
        install(new EPagesJServerBasePlugin());
        bind(Server.class).toProvider(FastCGIServerProvider.class);

    }

}
