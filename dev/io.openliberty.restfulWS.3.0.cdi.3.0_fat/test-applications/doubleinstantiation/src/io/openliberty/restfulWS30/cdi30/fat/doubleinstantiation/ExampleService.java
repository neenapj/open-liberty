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

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Simple CDI service for testing.
 */
@ApplicationScoped
public class ExampleService {
    
    public ExampleService() {
        System.out.println("ExampleService instantiating; instance=" + this.hashCode());
    }

    public String whatever() {
        System.out.println("ExampleService executing whatever(); instance=" + this.hashCode());
        return "Whatever";
    }
}

// Made with Bob
