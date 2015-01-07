package Plack::Middleware::EpagesHeaders;

use strict;

use base qw (Plack::Middleware);

use DE_EPAGES::WebInterface::API::ASPoolDBCacheServer;

sub prepare_app {
	my $self = shift;
	$self->{ascLookup} = DE_EPAGES::WebInterface::API::ASPoolDBCacheServer->new();
}

sub call
{
        my ($self, $env) = @_;
        if(!exists($env->{'HTTP_X_EPAGES_SITE'}) || !exists($env->{'HTTP_X_EPAGES_STORE'})) {
        	# TODO
        	my $hLookup = $self->{ascLookup}->lookup('otrosien','/epages/DemoShop.', 'sf');
        	$env->{'HTTP_X_EPAGES_SITE'}  = $hLookup->{SiteGUID};
        	$env->{'HTTP_X_EPAGES_STORE'} = $hLookup->{Storename};
        	$env->{'SCRIPT_NAME'} = '/epages/DemoShop.sf';
        }
        $self->app->($env);
}

__PACKAGE__
__END__