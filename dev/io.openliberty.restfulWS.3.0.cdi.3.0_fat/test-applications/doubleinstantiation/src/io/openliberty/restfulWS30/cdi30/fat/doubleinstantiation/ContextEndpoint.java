package io.openliberty.restfulWS30.cdi30.fat.doubleinstantiation;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/context-hello")
public class ContextEndpoint {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response hello() {
        return Response.ok("Hello from context endpoint").build();
    }
}

// Made with Bob
