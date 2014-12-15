package fcgi;

import javax.inject.Inject;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epages.server.BaseEmbeddedServer;
import com.epages.server.config.ServerConfiguration;

class FastCGIServer extends BaseEmbeddedServer {

	private static final Logger log = LoggerFactory.getLogger(FastCGIServer.class);
	
	@Inject
	public FastCGIServer(Server server, ServerConfiguration config) {
		super(server, config);
	}

	@Override
	public void start() {
		log.info("starting server");
		super.start();
	}

	@Override
	public void stop() {
		log.info("stopping server");
		super.stop();
	}
}
