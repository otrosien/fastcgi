package fcgi;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.fcgi.server.proxy.FastCGIProxyServlet;
import org.eclipse.jetty.http.HttpFields;

public class FastCGIServlet extends FastCGIProxyServlet {

	public FastCGIServlet() {
		super();
	}

	@Override
	protected void customizeFastCGIHeaders(Request proxyRequest,
			HttpFields fastCGIHeaders) {
		if(proxyRequest.getPath().contains("/epages/DemoShop.")) {
			// DemoShop
			fastCGIHeaders.add("HTTP_X_EPAGES_STORE", "Store");
			fastCGIHeaders.add("HTTP_X_EPAGES_SITE", "548E19B8-1614-142C-7C9F-AC1545224A28");
		} else if (proxyRequest.getPath().contains("/epages/Store.")) {
			// Store
			fastCGIHeaders.add("HTTP_X_EPAGES_STORE", "Store");
			fastCGIHeaders.add("HTTP_X_EPAGES_SITE", "548E1929-FEEC-DCB5-857C-AC154522B9EF");
		} else {
			// Distributor.. not 100% correct either :)
			fastCGIHeaders.add("HTTP_X_EPAGES_STORE", "Site");
			fastCGIHeaders.add("HTTP_X_EPAGES_SITE", "548E19A8-24C7-65B6-D80D-AC15452251C3");
		}
		// pass authorization header.
		if(proxyRequest.getHeaders().containsKey("Authorization")) {
			fastCGIHeaders.add("HTTP_AUTHORIZATION", proxyRequest.getHeaders().get("Authorization"));
		}
		super.customizeFastCGIHeaders(proxyRequest, fastCGIHeaders);
	}
}
