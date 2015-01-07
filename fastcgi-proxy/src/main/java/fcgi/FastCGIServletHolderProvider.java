package fcgi;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.jetty.fcgi.server.proxy.FastCGIProxyServlet;
import org.eclipse.jetty.servlet.ServletHolder;

import com.epages.aspooldbcache.ShopInfoService;

class FastCGIServletHolderProvider implements Provider<ServletHolder> {

    private final FastCGIConfiguration config;

    private final ShopInfoService shopInfoService;

    @Inject
    public FastCGIServletHolderProvider(FastCGIConfiguration config, Provider<ShopInfoService> shopInfoService) {
        this.config = config;
        this.shopInfoService = shopInfoService.get();
    }

    @Override
    public ServletHolder get() {
        ServletHolder sh = new ServletHolder("FastCGI", new FastCGIServlet(shopInfoService));
        sh.setInitParameter(FastCGIProxyServlet.SCRIPT_ROOT_INIT_PARAM, config.getScriptRoot());
        sh.setInitParameter("contextPath", "/*");
        sh.setInitParameter("proxyTo", "http://localhost:8090/");
        sh.setInitParameter("prefix", "/");
        sh.setInitParameter(FastCGIProxyServlet.SCRIPT_PATTERN_INIT_PARAM, "(^/epages/\\w+\\.\\w+)(.*)");
        return sh;
    }

}
