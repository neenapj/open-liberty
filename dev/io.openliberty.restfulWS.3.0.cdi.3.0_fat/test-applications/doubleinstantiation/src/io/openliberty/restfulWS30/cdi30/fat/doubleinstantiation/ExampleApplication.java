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
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Test application to reproduce double instantiation issue when both
 * restfulWS-3.0 and cdi-3.0 features are enabled.
 * 
 * This application extends jakarta.ws.rs.core.Application and should only
 * be instantiated once, but with both features enabled, it gets instantiated
 * twice - once as a CDI proxy and once as the actual bean instance.
 */
@ApplicationScoped
@ApplicationPath("/")
public class ExampleApplication extends Application {
    
    private static int instanceCount = 0;
    
    public ExampleApplication() {
        instanceCount++;
        System.out.println("=== ExampleApplication Constructor Called ===");
        System.out.println("Instance count: " + instanceCount);
        System.out.println("Instance hashCode: " + this.hashCode());
        System.out.println("Instance class: " + this.getClass().getName());
        System.out.println("Stack trace:");
        
        // Print stack trace to identify the instantiation path
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < Math.min(stackTrace.length, 20); i++) {
            System.out.println("  at " + stackTrace[i]);
        }
        System.out.println("==========================================");
    }
    
    /**
     * Get the total number of times this Application class has been instantiated.
     * Should be 1, but will be 2 if the bug is present.
     */
    public static int getInstanceCount() {
        return instanceCount;
    }
    
    /**
     * Reset the instance count (for testing purposes).
     */
    public static void resetInstanceCount() {
        instanceCount = 0;
    }
}

// Made with Bob
