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

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Jakarta REST {@link Application} for the {@code /jackson/*} path.
 *
 * <p>Registers {@link JakartaJacksonProvider} explicitly so that provider
 * selection is deterministic: the container JSON-B provider should never be
 * invoked for {@code application/json} requests to this application.
 */
@ApplicationPath("/jackson")
public class JakartaJacksonApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(OrderResource.class);
        classes.add(JakartaJacksonProvider.class);
        return classes;
    }
}
