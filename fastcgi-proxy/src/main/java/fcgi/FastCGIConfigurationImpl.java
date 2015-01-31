package fcgi;

import java.net.URI;

import javax.inject.Inject;

import org.apache.commons.configuration.Configuration;

final class FastCGIConfigurationImpl implements FastCGIConfiguration {

    private Configuration config;

    @Inject
    public FastCGIConfigurationImpl(Configuration config) {
        this.config = config;
    }

    @Override
    public String getScriptRoot() {
        return "/";
    }

    @Override
    public URI getProxyTo() {
        return URI.create(config.getString("FastCGI.proxyTo"));
    }
}
