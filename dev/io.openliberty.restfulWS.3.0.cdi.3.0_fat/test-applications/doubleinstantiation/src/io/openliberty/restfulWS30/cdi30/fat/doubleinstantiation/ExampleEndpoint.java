/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.cdi30.fat.doubleinstantiation;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/hello")
public class ExampleEndpoint {
    
    private final ExampleService service;

    @Inject
    public ExampleEndpoint(final ExampleService service) {
        System.out.println("ExampleEndpoint instantiating; instance=" + this.hashCode());
        this.service = service;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response hello() {
        System.out.println("ExampleEndpoint executing hello(); instance=" + this.hashCode());
        String whatever = service.whatever();
        String message = String.format("Hello, %s!", whatever);
        return Response.ok(message).build();
    }
}

// Made with Bob
