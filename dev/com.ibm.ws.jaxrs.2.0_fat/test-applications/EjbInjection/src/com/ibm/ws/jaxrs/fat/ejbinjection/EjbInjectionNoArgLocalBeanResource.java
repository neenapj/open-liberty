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
import javax.ws.rs.Path;

import com.ibm.ws.jaxrs.fat.ejbinjection.interfaces.VersionEJBInterface;

/**
 * Same as EjbInjectionNoArgClassOnlyMethodResource but with @LocalBean added.
 *
 * @LocalBean makes the bean available as a no-interface local view in addition
 * to its @Local interface view. CDI/Weld then includes the bean class in the
 * type closure, so RESTEasy can obtain a proxy typed as the bean class and
 * invoke bean-class-only methods (e.g. getInfo()) without error.
 *
 * Expected behaviour on ALL EE levels (EE7/EE8 CXF and EE9/EE10/EE11 RESTEasy):
 *   GET noarglocalbean/version -> 200 "1.0"
 *   GET noarglocalbean/info    -> 200 "Hello!"
 */
@Stateless
@Local(VersionEJBInterface.class)
@LocalBean
@Path("noarglocalbean")
public class EjbInjectionNoArgLocalBeanResource {

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
