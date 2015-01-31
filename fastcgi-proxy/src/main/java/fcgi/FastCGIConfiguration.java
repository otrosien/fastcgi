package fcgi;

import java.net.URI;

public interface FastCGIConfiguration {

    String getScriptRoot();

    URI getProxyTo();
}
