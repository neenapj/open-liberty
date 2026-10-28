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
import javax.ws.rs.Path;
import javax.ejb.LocalBean;


import com.ibm.ws.jaxrs.fat.ejbinjection.interfaces.VersionEJBInterface;

/**
 * Stateless EJB that declares @Local(VersionEJBInterface.class) but does NOT
 * implement the interface (matching the customer's WatsonV01ApiRsBean pattern).
 *
 * VersionEJBInterface only declares getVersion(). The no-argument resource method
 * getInfo() exists only on the bean class with no counterpart on any @Local interface.
 *
 * Expected behaviour:
 *   GET noargclassonlymethod/version -> 200 "1.0"      (method IS on @Local interface)
 *   GET noargclassonlymethod/info    -> 200 "Hello!"   (method is NOT on @Local interface - reproduces RESTEASY003020)
 */
@Stateless
@Local(VersionEJBInterface.class)
@LocalBean
@Path("noargclassonlymethod")
public class EjbInjectionNoArgClassOnlyMethodResource {

    @Resource
    SessionContext ctx;

    @GET
    @Path("version")
    public String getVersion() {
        return "1.0";
    }

    @GET
    @Path("info")
    @Consumes({ "application/json", "application/xml", "text/plain" })
    public String getInfo() {
        if (ctx == null) {
            return "ctx is null";
        }
        return "Hello!";
    }
}
