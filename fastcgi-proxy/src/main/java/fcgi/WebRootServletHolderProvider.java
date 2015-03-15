package fcgi;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.jetty.servlet.ServletHolder;

import com.epages.server.DefaultServletHolderBuilder;

final class WebRootServletHolderProvider implements Provider<ServletHolder> {

    private final DefaultServletHolderBuilder builder;

    @Inject
    public WebRootServletHolderProvider(DefaultServletHolderBuilder builder) {
        this.builder = builder;
    }

    @Override
    public ServletHolder get() {
        builder.withContextPath("/WebRoot/*");
        builder.withResourceBase("/srv/epages/eproot/Shared/WebRoot");
        builder.withDirectoryListed(false);
        return builder.build();
    }

}
