package fcgi;

import javax.inject.Inject;

import org.apache.commons.configuration.Configuration;

import com.epages.configuration.file.EPagesJDirConfiguration;
import com.epages.server.config.AbstractBaseServerConfiguration;

class FastCGIServerConfiguration extends AbstractBaseServerConfiguration {

    private static final String SERVER_SSLPORT = "Server.sslPort";
    private final Configuration config;

    @Inject
    public FastCGIServerConfiguration(Configuration config, EPagesJDirConfiguration dirConfig) {
        super(config, dirConfig);
        this.config = config;
    }

    @Override
    protected String getAppName() {
        return "FastCGI";
    }

    public int getSSLPort() {
        return config.getInt(getAppName() + SERVER_SSLPORT);
    }

}
