package fcgi;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.jetty.alpn.ALPN;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.spdy.api.SPDY;
import org.eclipse.jetty.spdy.server.http.HTTPSPDYServerConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the web.xml equivalent
 *
 */
class FastCGIServerProvider implements Provider<Server> {

    private static final Logger log = LoggerFactory.getLogger(FastCGIServerProvider.class);

    private final FastCGIServerConfiguration config;

    private final ThreadPool threadPool;

    private final Set<ServletHolder> servletHolders;

    // private final RequestLogHandlerProvider requestLoggerProvider;

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

        ConnectionFactory[] factories = getConectionFactories();
        ServerConnector connector = new ServerConnector(server, factories);
        connector.setHost(config.getHost());
        connector.setPort(config.getSSLPort());

        server.addConnector(connector);
        server.setStopAtShutdown(config.getStopAtShutdown());

        ServletContextHandler rootContextHandler = new ServletContextHandler();
        //
        if (config.followSymLinks()) {
            rootContextHandler.addAliasCheck(new AllowSymLinkAliasChecker());
        }
        //
        for (ServletHolder sh : this.servletHolders) {
            log.info(sh.getName());
            rootContextHandler.addServlet(sh, sh.getInitParameter("contextPath"));
        }
        //
        // final ServletHolder loggerServletHolder = new
        // ServletHolder("Logback servlet",
        // ch.qos.logback.classic.ViewStatusMessagesServlet.class);
        // rootContextHandler.addServlet(loggerServletHolder, "/logger" );
        // // rootContextHandler.addFilter(PushCacheFilter.class, "/*", null);
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

    private ConnectionFactory[] getConectionFactories() {

        final HttpConfiguration httpConfig = getHttpConfig();

        SslContextFactory sslContextFactory = new SslContextFactory("/etc/pki/java/keystore");
        sslContextFactory.setKeyStorePassword("qwert6");
        sslContextFactory.setKeyManagerPassword("qwert6");

        SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, "alpn");
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory("spdy/3", "h2-14", "http/1.1");
        alpn.setDefaultProtocol("http/1.1");
        HTTPSPDYServerConnectionFactory spdy = new HTTPSPDYServerConnectionFactory(SPDY.V3, httpConfig);
        HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpConfig);

        HttpConnectionFactory http = new HttpConnectionFactory(httpConfig);
        NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable();

        ConnectionFactory[] factories = new ConnectionFactory[] { ssl, alpn, spdy, h2, http };
        return factories;
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
        httpConfig.setSecurePort(config.getSSLPort());
        return httpConfig;
    }
}