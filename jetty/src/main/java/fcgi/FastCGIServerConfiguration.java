package fcgi;

import javax.inject.Inject;

import org.apache.commons.configuration.Configuration;

import com.epages.configuration.file.EPagesJDirConfiguration;
import com.epages.server.config.AbstractBaseServerConfiguration;

class FastCGIServerConfiguration extends AbstractBaseServerConfiguration {

	@Inject
	public FastCGIServerConfiguration(Configuration config,
			EPagesJDirConfiguration dirConfig) {
		super(config, dirConfig);
	}

	@Override
	protected String getAppName() {
		return "FastCGI";
	}

}