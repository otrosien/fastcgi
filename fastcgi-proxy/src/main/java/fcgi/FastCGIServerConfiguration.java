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

    public boolean getSpdyEnabled() {
        return config.getBoolean(getAppName() + "Server.spdyEnabled");
    }

    public boolean getHttp2Enabled() {
        return config.getBoolean(getAppName() + "Server.http2Enabled");
    }

    public boolean getPushEnabled() {
        return config.getBoolean(getAppName() + "Server.pushEnabled");
    }

    public int getPushAssociatePeriodMs() {
        return config.getInt(getAppName() + "Server.pushAssociatePeriodMs");
    }

    public String getSslKeystore() {
        return config.getString(getAppName() + "Server.sslKeystore");
    }

    public String getSslKeystorePassword() {
        return config.getString(getAppName() + "Server.sslKeystorePassword");
    }

    public String getSslKeyManagerPassword() {
        return config.getString(getAppName() + "Server.sslKeyManagerPassword");
    }
}
