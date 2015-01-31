package fcgi;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.DispatcherType;

import org.eclipse.jetty.alpn.ALPN;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.fcgi.server.ServerFCGIConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.PushCacheFilter;
import org.eclipse.jetty.spdy.api.SPDY;
import org.eclipse.jetty.spdy.server.http.HTTPSPDYServerConnectionFactory;
import org.eclipse.jetty.spdy.server.http.PushStrategy;
import org.eclipse.jetty.spdy.server.http.ReferrerPushStrategy;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.servlet.GuiceFilter;

/**
 * This is the web.xml equivalent
 *
 */
class FastCGIServerProvider implements Provider<Server> {

    private static final Logger log = LoggerFactory.getLogger(FastCGIServerProvider.class);

    private final FastCGIServerConfiguration config;

    private final ThreadPool threadPool;

    private final Set<ServletHolder> servletHolders;

    @Inject
    public FastCGIServerProvider(FastCGIServerConfiguration config, ThreadPool threadPool, Set<ServletHolder> servletHolders) {
        this.config = config;
        this.threadPool = threadPool;
        this.servletHolders = servletHolders;
        // this.requestLoggerProvider = new RequestLogHandlerProvider(config);
        ALPN.debug = true;
    }

    @Override
    public Server get() {
        System.setProperty("org.eclipse.jetty.servlet.LEVEL", "DEBUG");
        final Server server = new Server(this.threadPool);

        // HTTP
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(getHttpConfig()));
        connector.setHost(config.getHost());
        connector.setPort(config.getPort());
        server.addConnector(connector);

        // HTTPS
        ConnectionFactory[] factories = getSslConectionFactories();
        ServerConnector sslConnector = new ServerConnector(server, factories);
        sslConnector.setHost(config.getHost());
        sslConnector.setPort(config.getSSLPort());
        server.addConnector(sslConnector);

        // FCGI
        ServerConnector fcgiConnector = new ServerConnector(server, getFcgiConnectionFactory());
        fcgiConnector.setHost(config.getHost());
        fcgiConnector.setPort(8092);
        server.addConnector(fcgiConnector);

        server.setStopAtShutdown(config.getStopAtShutdown());
        ServletContextHandler rootContextHandler = new ServletContextHandler();

        if (config.followSymLinks()) {
            rootContextHandler.addAliasCheck(new AllowSymLinkAliasChecker());
        }

        rootContextHandler.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));

        for (ServletHolder sh : this.servletHolders) {
            log.info(sh.getName());
            rootContextHandler.addServlet(sh, sh.getInitParameter("contextPath"));
        }

        // HTTP/2 Push support.
        if(config.getPushEnabled()) {
            rootContextHandler.addFilter(getPushFilterHolder(), "/WebRoot/*", null);
        }

        //
        // final ServletHolder loggerServletHolder = new
        // ServletHolder("Logback servlet",
        // ch.qos.logback.classic.ViewStatusMessagesServlet.class);
        // rootContextHandler.addServlet(loggerServletHolder, "/logger" );
        // rootContextHandler.setWelcomeFiles(new String[] {
        // config.getContextPath() + "/", "index.html" });
        ServletHolder defaultServletHolder = new ServletHolder("default", DefaultServlet.class);
        defaultServletHolder.setInitParameter("resourceBase", "."); // TODO find
                                                                    // out best
                                                                    // practice
        defaultServletHolder.setInitParameter("welcomeServlets", "true");
        rootContextHandler.addServlet(defaultServletHolder, "/");
        //
        // RequestLogHandler logger = requestLoggerProvider.get();
        // if(logger != null) {
        // rootContextHandler.setHandler(logger);
        // }
        server.setHandler(rootContextHandler);

        return server;
    }

    private ConnectionFactory getFcgiConnectionFactory() {
        return new ServerFCGIConnectionFactory(getHttpConfig());
    }

    private ConnectionFactory[] getSslConectionFactories() {

        final HttpConfiguration httpConfig = getHttpsConfig();

        SslContextFactory sslContextFactory = new SslContextFactory(config.getSslKeystore());
        sslContextFactory.setKeyStorePassword(config.getSslKeystorePassword());
        sslContextFactory.setKeyManagerPassword(config.getSslKeyManagerPassword());

        ArrayList<String> supportedProtocols = new ArrayList<>();
        supportedProtocols.add("http/1.1");
        ArrayList<ConnectionFactory> connectionFactories = new ArrayList<>();

        connectionFactories.add(new SslConnectionFactory(sslContextFactory, "alpn"));

        if(config.getSpdyEnabled()) {
            PushStrategy pushStrategy;
            // SPDY Push support
            if(config.getPushEnabled()) {
                pushStrategy = new ReferrerPushStrategy();
                ((ReferrerPushStrategy)pushStrategy).setMaxAssociatedResources(120);
                ((ReferrerPushStrategy)pushStrategy).setReferrerPushPeriod(config.getPushAssociatePeriodMs());
            }
            else {
                pushStrategy = new PushStrategy.None();
            }

            connectionFactories.add(new HTTPSPDYServerConnectionFactory(SPDY.V3, httpConfig, pushStrategy));
            supportedProtocols.add(0, "spdy/3");
        }

        if(config.getHttp2Enabled()) {
            connectionFactories.add(new HTTP2ServerConnectionFactory(httpConfig));
            supportedProtocols.add(0, "h2-14");
        }

        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory(supportedProtocols.toArray(new String[0]));
        alpn.setDefaultProtocol("http/1.1");
        NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable();
        connectionFactories.add(1, alpn);

        connectionFactories.add(new HttpConnectionFactory(httpConfig));

        return connectionFactories.toArray(new ConnectionFactory[0]);
    }

    private FilterHolder getPushFilterHolder() {
        FilterHolder holder = new FilterHolder(PushCacheFilter.class);
        holder.setInitParameter("associatePeriod", "8000");
        return holder;
    }

    private HttpConfiguration getHttpConfig() {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendDateHeader(config.getSendDateHeader());
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());
        httpConfig.setHeaderCacheSize(config.getHeaderCacheSize());
        httpConfig.setRequestHeaderSize(config.getRequestHeaderSize());
        httpConfig.setResponseHeaderSize(config.getResponseHeaderSize());
        httpConfig.setOutputBufferSize(config.getResponseBufferSize());
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(443);
        return httpConfig;
    }

    private HttpConfiguration getHttpsConfig() {
        HttpConfiguration httpConfig = getHttpConfig();
        httpConfig.addCustomizer(new SecureRequestCustomizer());
        return httpConfig;
    }
}