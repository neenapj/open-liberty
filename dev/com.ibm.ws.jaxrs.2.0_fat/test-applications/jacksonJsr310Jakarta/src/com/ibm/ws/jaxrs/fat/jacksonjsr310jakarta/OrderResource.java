/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.jacksonjsr310jakarta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Jakarta REST resource that accepts and returns {@link OrderDto} objects.
 * Used to verify that the application-packaged Jackson provider correctly
 * serializes and deserializes {@code java.time.LocalDate} fields under
 * restfulWS-3.1, overriding the container JSON-B provider.
 */
@Path("/orders")
public class OrderResource {

    /** Returns a single order — drives serialization (write path). */
    @GET
    @Path("/single")
    @Produces(MediaType.APPLICATION_JSON)
    public OrderDto getOrder() {
        OrderDto o = new OrderDto();
        o.setId("ORD-001");
        o.setOrderDate(LocalDate.of(2026, 8, 25));
        return o;
    }

    /** Echoes back whatever the client POSTs — drives deserialization (read path). */
    @POST
    @Path("/single")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public OrderDto postOrder(OrderDto order) {
        return order;
    }

    /** Returns a list of orders — drives collection serialization. */
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OrderDto> getOrders() {
        List<OrderDto> orders = new ArrayList<>();
        OrderDto o1 = new OrderDto();
        o1.setId("ORD-001");
        o1.setOrderDate(LocalDate.of(2026, 8, 25));
        orders.add(o1);
        OrderDto o2 = new OrderDto();
        o2.setId("ORD-002");
        o2.setOrderDate(LocalDate.of(2026, 8, 26));
        orders.add(o2);
        return orders;
    }

    /** Echoes back a posted list — drives collection deserialization. */
    @POST
    @Path("/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<OrderDto> postOrders(List<OrderDto> orders) {
        return orders;
    }
}
