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
import javax.ejb.LocalBean;
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
 * Same as EjbInjectionWatsonStyleResource but with @LocalBean added.
 *
 * @LocalBean makes the bean available as a no-interface local view in addition
 * to its @Local interface view. CDI/Weld then includes the bean class in the
 * type closure, so RESTEasy can obtain a proxy typed as the bean class and
 * invoke bean-class-only methods (e.g. getInfo()) without error.
 *
 * Expected behaviour on ALL EE levels (EE7/EE8 CXF and EE9/EE10/EE11 RESTEasy):
 *   POST watsonlocalbean/writemessage -> 200 "echo: <input>"
 *   GET  watsonlocalbean/info         -> 200 "Hello, World!"
 */
@Stateless
@Local(WatsonStyleInterface.class)
@LocalBean
@Path("watsonlocalbean")
public class EjbInjectionWatsonStyleLocalBeanResource {

    @Resource
    SessionContext ctx;

    /**
     * Parameterised endpoint — declared on WatsonStyleInterface.
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
     * With @LocalBean this should work on all EE levels including RESTEasy.
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
