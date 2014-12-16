use Plack::Handler::FCGI;
use Plack::Handler::Standalone;
use DE_EPAGES::WebInterface::API::ApplicationServer;
use DE_EPAGES::Core::API::Script qw ( RunScript );

RunScript(
    Sub => sub {
        $App =
          DE_EPAGES::WebInterface::API::ApplicationServer->new(
            Address => '127.0.0.1', );

        my $Server;

        no warnings 'once';
        if ( defined($DB::sub) ) {
            # start standalone for debugging.
            $Server = Plack::Handler::Standalone->new(
                nproc   => 1,
                port    => '8091',
                detach  => 0,
            );
        }
        else {
            # start FastCGI
            $Server = Plack::Handler::FCGI->new(
                nproc   => 8,
                port    => '8090',
                detach  => 0,
            );
        }
        use warnings;
        eval { $Server->run( $App->to_app ); };
        $App->shutdown;
    }
);