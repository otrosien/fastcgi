package fcgi;

import java.util.EnumSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.DispatcherType;

import org.eclipse.jetty.fcgi.server.proxy.TryFilesFilter;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epages.server.config.ServerConfiguration;
import com.epages.server.inject.RequestLogHandlerProvider;

/**
 * This is the web.xml equivalent
 *
 */
class FastCGIServerProvider implements Provider<Server> {

    private static final Logger log = LoggerFactory.getLogger(FastCGIServerProvider.class);

    private final ServerConfiguration config;

    private final ThreadPool threadPool;

    private final Set<ServletHolder> servletHolders;

    private final RequestLogHandlerProvider requestLoggerProvider;

    @Inject
    public FastCGIServerProvider(ServerConfiguration config, ThreadPool threadPool, Set<ServletHolder> servletHolders) {
        this.config = config;
        this.threadPool = threadPool;
        this.servletHolders = servletHolders;
        this.requestLoggerProvider = new RequestLogHandlerProvider(config);
    }

    @Override
    public Server get() {
        System.setProperty("org.eclipse.jetty.servlet.LEVEL", "DEBUG");

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendDateHeader(config.getSendDateHeader());
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());

        httpConfig.setHeaderCacheSize(config.getHeaderCacheSize());
        httpConfig.setRequestHeaderSize(config.getRequestHeaderSize());
        httpConfig.setResponseHeaderSize(config.getResponseHeaderSize());
        httpConfig.setOutputBufferSize(config.getResponseBufferSize());

        final Server server = new Server(this.threadPool);

        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        connector.setHost(config.getHost());
        connector.setPort(config.getPort());

        server.setStopAtShutdown(config.getStopAtShutdown());
        server.addConnector(connector);

        ServletContextHandler rootContextHandler = new ServletContextHandler();
        
        if (config.followSymLinks()) {
            rootContextHandler.addAliasCheck(new AllowSymLinkAliasChecker());
        }

        rootContextHandler.addFilter(TryFilesFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST)).setInitParameter("files", "$path $path");
        
        for (ServletHolder sh : this.servletHolders) {
            log.info(sh.getName());
            rootContextHandler.addServlet(sh, sh.getInitParameter("contextPath"));
        }

        final ServletHolder loggerServletHolder = new ServletHolder("Logback servlet", ch.qos.logback.classic.ViewStatusMessagesServlet.class);
        rootContextHandler.addServlet(loggerServletHolder, "/logger" );

        rootContextHandler.setWelcomeFiles(new String[] { config.getContextPath() + "/", "index.html" });
        ServletHolder defaultServletHolder = new ServletHolder("default", DefaultServlet.class);
        defaultServletHolder.setInitParameter("resourceBase", "."); //TODO find out best practice
        defaultServletHolder.setInitParameter("welcomeServlets", "true");
        rootContextHandler.addServlet(defaultServletHolder,"/");

        RequestLogHandler logger = requestLoggerProvider.get();
        if(logger != null) {
            rootContextHandler.setHandler(logger);
        }
        server.setHandler(rootContextHandler);

        return server;
    }
}