# ePages via FastCGI

This is a prototype, to find out how much we can benefit from new protocols like ``SPDY`` and ``http/2``,
from going with standard protocols, like `FastCGI`` and ``PSGI``, or just putting the
page cache outside of epages with varnish. Another novelty here is to pre-fork the appserver processes
to benefit from copy-on-write memory sharing in Linux.

Beware, there might be dragons on your journey.

## How do we achieve this?

Instead of launching a separate process foreach application server, we create a pre-forking daemon, which
spawns a number of child processes, that connect to the message center. The FastCGI proxy (jetty)
connects to this daemon (the "manager"), and it will delegate the work to its children.

So finally, we get the following communication setup:

* HTTP: `` :80 (iptables)  ->  :6081 (varnish) -> :8089 (jetty) -> :8090 (Plack)``
* HTTPS: `` :443 (iptables) -> :8089 (jetty) -> :8090 (Plack)``

### How does SPDY push work

Jetty has a Push Cache by looking at the Referrer header from the browser's requests. If there are subsequent requests coming from the same referrer within 8 seconds,
Jetty will add these to a mapping list, so that next time it can offer pushing these resources directly to the browser.

## Installing

Be brave, there is some work to do...

1. Import this project into Eclipse
* Install some modules from CPAN (Plack, Plack::Handler::FastCGI, Starman (if you like))
* Install varnish (optional) there is a sample config in ``conf/varnish/default.vcl``
* Make sure you have SSL setup on ePages (``$PERL set.pl -storename Store -path / HasSSLCertificate=1 ``)
* Stop all epages services
* Change ServerConfig.xml: ``<appserver host="127.0.0.1" ports="10045-14049" maxmemory="300" />`` (No, we're not going to start 4000 appserver processes, but with prefork we need to randomly generate a port number to register a unique process at the MessageCenter) - **be sure to revert this change, when switching back again!**
* Install iptables for port-forwarding ports 80 and 443, see ``conf/etc/iptables``
* switch branch of your Cartridges repo to ``feature/psgi``
* start some of the services again: ``service epagse6 start_asc`` and ``service epagse6 start_rr`` (needed only for MessageCenter) ``service mysqld start``
* Setup an SSL keystore for jetty, default is ``/etc/pki/java/keystore`` ([see here](https://wiki.eclipse.org/Jetty/Howto/Configure_SSL))
* Run ``FastCGIStart`` from eclipse
* For testing the connectivity, run ``hello.psgi`` from eclipse
* For launching epages, run ``app.psgi`` from eclipse.

## Load-Testing

I recommend using the load-testing tool ``wrk``, which gives a good impression of Requests/s and latency numbers, for example:
  ``wrk -c 40 -t 5 --latency http://otrosien/epages/DemoShop.sf``

## How can I test ``http/2``?

SPDY should be supported in both Firefox and Chrome. For http/2, current versions of Firefox ship with http/2, which is disabled by default, so you just need to enable it (in about:config)

### Is the cache useful?

Yes, epages sets a cookie called ShopInit=1 with the initial request, which defies all caching, but we can set to ignore this in varnish.
Only if there is more than just the ShopInit cookie, we bypass varnish. But caching outside of epages brings the problem of cache invalidation.
This is a delicate subject, as there a multiple ways of accessing a resource in epages (ObjectPath, ObjectID, with provider domain, with shop domain,
using the Shop.sf or even using Store.sf in the URL....), so it would be insane to know outside of epages when to invalide all these copies.

Thus you should only cache requests for the shop domain, and invalidate by domain name to clean the cache. In most cases this should be good enough.

BTW, you still need to enable epages PageCache in order for epages to set the necessary caching headers for varnish. For the results, see below...
  
## Some first impressions

There is a **huge** benefit of running ePages behind varnish (100 Req/s -> 30.000 Req/s on my laptop).

The main problem remains: ePages triggers too many requests after the intial page load (javascript, images etc.)

For this we investigate using SPDY and HTTP/2. For a locally connected server, the new protocols do not offer significant speed improvements. They become more interesting, as soon
as there is a bigger latency between client and server. You can simulate this in Linux with the tool "tc":
   ``tc qdisc add dev $NETWORK_INTERFACE root netem delay 200ms``   , or removing it again:
   ``tc qdisc del dev $NETWORK_INTERFACE root``

SPDY and http/2 Push is enabled by default, but I don't see it kicking off enough yet. The browser still launches too many requests. This might be due to the way we asynchronusly load javascript, but I haven't checked in detail yet. Of course with browser cache turned on all subsequent pages are much faster, as these resources should be in the browser cache by now.

## Things not working

* The FastCGI connection between Jetty and Plack is sometimes flaky (you'll get 502 Errors) I haven't had the chance to find out what's wrong.
* Cache invalidation for varnish needs to be plugged in.
* We could get the pool name from the ASPoolCacheDaemon, but there is no pooling via FastCGI yet. Idea would be to have several FastCGI connections per pool, and load-balance among these.

## Some more toys to play with

* You can try using Starman and connect directly via HTTP to your application servers. (still needs some work setting up CGI variables, like SCRIPT_NAME) Try to enable a static handler for WebRoot for more fun with it.
* You can enable some Plack Middleware (direct profiling ePages via NYTProf is cool :))
* Debugging preforked processes via Eclipse does not seem to be possible. Try to run app.psgi with "-stanalone" option. You'll get it listening for HTTP on port 8089 in this case.
* Play with cache invalidation protocol for varnish. I would recommend to only use varnish on shops that have their own domain, so you can invalidate the domain's HTML cache.
* Try load-balancing multiple FastCGI connections for "ASPooling"
* Try out the FastCGI module from NGinX... 

etc. 