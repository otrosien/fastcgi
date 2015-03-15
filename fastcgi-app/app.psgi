use Plack::Handler::FCGI;
use Plack::Handler::Standalone;
use Plack::Builder;
use DE_EPAGES::WebInterface::API::ApplicationServer;
use DE_EPAGES::Core::API::Script qw ( RunScript );
use Plack::Middleware::EpagesHeaders;
use Getopt::Long;

RunScript(
	Sub => sub {

		my $Standalone;
		my $Profile;

		GetOptions(
			'standalone' => \$Standalone,
			'profile'    => \$Profile,
		);

		my $builder = Plack::Builder->new;

		$App =
		  DE_EPAGES::WebInterface::API::ApplicationServer->new(
			Address => '127.0.0.1', );

		my $Server;

		if ($Standalone) {
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

			my %info = (
				port        => '8090',
				nproc       => 8,
				die_timeout => 5,
				detach      => 0,
				# TODO check if this gives noticeable effect.
				#'psgix.harakiri.commit' => 1,
			);
		    no warnings 'once'; ## no critic (ProhibitNoWarnings)
		    $info{manager} = undef if (defined $DB::sub);
		    use warnings;

			$Server = Plack::Handler::FCGI->new(
				%info
			);
		}

		if ($Profile) {
			$builder->add_middleware(
				'Debug',
				panels => [
					[
						'Profiler::NYTProf',
						base_URL => 'https://otrosien/WebRoot/_MONITOR_/nytprof',
						root =>
						  '/srv/epages/eproot/Shared/WebRoot/_MONITOR_/nytprof',
						minimal => 1,
					]
				]
			);
		}

		#  $builder->add_middleware('Refresh',  cooldown => '3');
		#  $builder->add_middleware('Runtime');

		$Server->run( $builder->wrap( $App->to_app ) );
		$App->shutdown;
	}
);
