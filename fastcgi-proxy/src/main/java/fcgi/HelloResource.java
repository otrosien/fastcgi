package fcgi;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/epages/DemoShop.sf")
public class HelloResource {

    @Path("hello")
    public Response getHello() {
        return Response.ok().entity("Hello World!").build();
    }
}
