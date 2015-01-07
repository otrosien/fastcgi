package fcgi;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.ws.rs.NotFoundException;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.fcgi.server.proxy.FastCGIProxyServlet;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpScheme;

import com.epages.aspooldbcache.ShopInfo;
import com.epages.aspooldbcache.ShopInfoService;
import com.epages.commons.Closeable;
import com.epages.commons.cache.Cache;
import com.epages.commons.lifecycle.EPJLifeCycle;

public class FastCGIServlet extends FastCGIProxyServlet {

    private static final long serialVersionUID = 1L;

    private static final Pattern pattern = Pattern.compile("/epages/(\\w+)\\.");

    private final ShopInfoService shopInfoService;

    public FastCGIServlet(ShopInfoService shopInfoService) {
        super();
        this.shopInfoService = shopInfoService;
    }

    @Override
    public void init() throws ServletException {
        super.init();
    }

    @Override
    protected void customizeFastCGIHeaders(Request proxyRequest, HttpFields fastCGIHeaders) {
        Matcher matcher = pattern.matcher(proxyRequest.getPath());
        if (!matcher.find()) {
            throw new NotFoundException(proxyRequest.getPath());
        }
        String publicId = matcher.group(1);
        try {
            ShopInfo shopInfo = shopInfoService.get(fastCGIHeaders.get("HTTP_HOST"), publicId);
            fastCGIHeaders.add("HTTP_X_EPAGES_STORE", shopInfo.getStorename());
            fastCGIHeaders.add("HTTP_X_EPAGES_SITE", shopInfo.getGuid().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // pass authorization header.
        if (proxyRequest.getHeaders().containsKey("Authorization")) {
            fastCGIHeaders.add("HTTP_AUTHORIZATION", proxyRequest.getHeaders().get("Authorization"));
        }
        if (HttpScheme.HTTPS.is((String) proxyRequest.getAttributes().get(FastCGIProxyServlet.class.getName() + ".scheme"))) {
            fastCGIHeaders.add("SERVER_PORT_SECURE", "1");
        }

        super.customizeFastCGIHeaders(proxyRequest, fastCGIHeaders);
    }

}
