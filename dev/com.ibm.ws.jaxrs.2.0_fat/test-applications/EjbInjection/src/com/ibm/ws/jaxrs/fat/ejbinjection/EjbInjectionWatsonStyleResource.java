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
package com.ibm.ws.jaxrs.fat.ejbinjection;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.ibm.ws.jaxrs.fat.ejbinjection.interfaces.WatsonStyleInterface;

/**
 * Mirrors the customer's WatsonV01ApiRsBean structure exactly:
 *
 *   - @Stateless @Local(WatsonStyleInterface.class) but does NOT implement the interface
 *   - One parameterised endpoint declared on the @Local interface  -> works (OLGH34687 fix)
 *   - One no-arg endpoint NOT declared on the @Local interface    -> fails with RESTEASY003020
 *
 * This reproducer is intentionally expected to FAIL on the /info endpoint
 * until the follow-on fix for OLGH34687 is applied.
 */
@Stateless
@Local(WatsonStyleInterface.class)
@Path("watsonstyle")
public class EjbInjectionWatsonStyleResource {

    @Resource
    SessionContext ctx;

    /**
     * Parameterised endpoint — declared on WatsonStyleInterface.
     * Should work with the OLGH34687 fix already in place.
     */
    @POST
    @Path("writemessage")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String writeMessage(String input) {
        return "echo: " + input;
    }

    /**
     * No-arg endpoint — NOT declared on WatsonStyleInterface.
     * Reproduces RESTEASY003020: "Bad arguments passed to ... getInfo() ( )"
     * java.lang.IllegalArgumentException: object is not an instance of declaring class
     */
    @GET
    @Path("info")
    @Consumes({ "application/json", "application/xml", "text/plain" })
    @Produces("text/plain")
    public String getInfo() {
        if (ctx == null) {
            return "ctx is null";
        }
        return "Hello, World!";
    }
}
