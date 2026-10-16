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

import com.ibm.ws.jaxrs.fat.ejbinjection.interfaces.InfoEJBInterface;

/**
 * Stateless EJB whose JAX-RS annotations are on the bean class and whose
 * no-argument resource method is declared on the (unannotated) @Local interface.
 * The method has no parameters and returns a String.
 */
@Stateless
@Local(InfoEJBInterface.class)
@Path("noarginterfacemethod")
public class EjbInjectionNoArgInterfaceMethodResource implements InfoEJBInterface {

    @Resource
    SessionContext ctx;

    @Override
    @GET
    @Path("info")
    @Consumes({ "application/json", "application/xml", "text/plain" })
    public String getInfo() {
        if (ctx == null) {
            return "ctx is null";
        }
        return "Hello, World!";
    }
}
