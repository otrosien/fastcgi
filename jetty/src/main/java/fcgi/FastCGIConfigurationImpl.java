package fcgi;

final class FastCGIConfigurationImpl implements FastCGIConfiguration {

	public FastCGIConfigurationImpl() {
	}
	
	@Override
	public String getScriptRoot() {
		return "/epages";
	}
}
