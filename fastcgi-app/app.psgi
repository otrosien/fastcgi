use Plack::Handler::FCGI;
use Plack::Handler::Standalone;
use Plack::Builder;
use DE_EPAGES::WebInterface::API::ApplicationServer;
use DE_EPAGES::Core::API::Script qw ( RunScript );
use Plack::Middleware::EpagesHeaders;

RunScript(
	Sub => sub {

		my $builder = Plack::Builder->new;

		$App =
		  DE_EPAGES::WebInterface::API::ApplicationServer->new(
			Address => '127.0.0.1', );

		my $Server;

		no warnings 'once';
		if ( defined($DB::sub) ) {

			# start standalone for debugging.
			$Server = Plack::Handler::Standalone->new(
				nproc  => 1,
				port   => '8089',
				detach => 0,
			);
			# need to fetch Storename and GUID in this case
			$builder->add_middleware('Plack::Middleware::EpagesHeaders');
		}
		else {

			# start FastCGI
			$Server = Plack::Handler::FCGI->new(
				nproc       => 6,
				port        => '8090',
				die_timeout => 5,
				detach      => 0,

				# TODO check if this gives noticeable effect.
				'psgix.harakiri' => 1,
			);
		}
		use warnings;

		my $wrap = $App->to_app;

		if ( $_[1] eq '-profile' ) {
			$builder->add_middleware(
				'Debug',
				panels => [
					[
						'Profiler::NYTProf',
						base_URL =>
						  'http://otrosien:8089/WebRoot/_MONITOR_/nytprof',
						root =>
						  '/srv/epages/eproot/Shared/WebRoot/_MONITOR_/nytprof',
					]
				]
			);
		}

		#  $builder->add_middleware('Refresh',  cooldown => '3');
		#  $builder->add_middleware('Runtime');
		$wrap = $builder->wrap($wrap);

		eval { $Server->run($wrap); };
		$App->shutdown;
	}
);
