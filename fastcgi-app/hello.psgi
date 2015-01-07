use Plack::Handler::FCGI;
use Plack::Handler::Starman;
use JSON::PP qw ( encode_json );
my $app = sub {
    my $env   = shift;
    my $coder = JSON::PP->new->utf8->pretty->allow_nonref->allow_blessed;
    #return [
    #    200,
    #    [ 'Content-Type' => 'application/json' ],
    #    [ $coder->encode($env) ]
    #];

       return [ 200, [ 'Content-Type' => 'text/plain' ], [ 'Hello World' ] ];
};

my $server = Plack::Handler::FCGI->new(
    'nproc' => 8,
    listen  => [':8090'],
    detach  => 0,
);
$server->run($app);
