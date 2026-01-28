package io.openliberty.restfulWS30.cdi30.fat.doubleinstantiation;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

/**
 * Similar to upstream RESTEasy's ApplicationInjection test.
 * This Application class has @Context injection which requires JAX-RS property injection.
 * Tests whether single instantiation is maintained even with @Context fields.
 */
//@Provider
//@ApplicationPath("/context")
public class ApplicationWithContextInjection /*extends Application*/ {
    
    /*public static volatile List<ApplicationWithContextInjection> instances = new ArrayList<>();
    
    @Context
    private Application app;
    
    public Application getApp() {
        return app;
    }
    
    public ApplicationWithContextInjection() {
        instances.add(this);
        System.out.println("ApplicationWithContextInjection constructor called. Instance count: " + instances.size());
        System.out.println("  Instance hashCode: " + System.identityHashCode(this));
        System.out.println("  Instance class: " + this.getClass().getName());
        
        // Print stack trace to see where instantiation is coming from
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        System.out.println("  Stack trace (first 20 frames):");
        for (int i = 0; i < Math.min(20, stackTrace.length); i++) {
            System.out.println("    " + stackTrace[i]);
        }
    }*/
}

// Made with Bob
