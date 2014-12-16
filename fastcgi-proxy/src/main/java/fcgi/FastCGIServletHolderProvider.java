package fcgi;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.jetty.fcgi.server.proxy.FastCGIProxyServlet;
import org.eclipse.jetty.servlet.ServletHolder;

class FastCGIServletHolderProvider implements Provider<ServletHolder> {

    private final FastCGIConfiguration config;

	@Inject
    public FastCGIServletHolderProvider(FastCGIConfiguration config) {
    	this.config = config;
    }

    @Override
    public ServletHolder get() {
        ServletHolder sh = new ServletHolder("FastCGI", FastCGIServlet.class);
    	sh.setInitParameter(FastCGIProxyServlet.SCRIPT_ROOT_INIT_PARAM, config.getScriptRoot());
    	sh.setInitParameter("contextPath", "/*");
    	sh.setInitParameter("proxyTo", "http://localhost:8090/");
    	sh.setInitParameter("prefix", "/");
    	sh.setInitParameter(FastCGIProxyServlet.SCRIPT_PATTERN_INIT_PARAM, "(^/epages/\\w+\\.\\w+)(.*)");
    	return sh;
    }

}
