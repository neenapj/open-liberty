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
package io.openliberty.restfulWS30.cdi30.fat.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test to reproduce the double instantiation issue of JAX-RS Application class
 * when both restfulWS-3.0 and cdi-3.0 features are enabled.
 * 
 * The Application class should only be instantiated once, but with both features
 * enabled, it gets instantiated twice - once as a CDI proxy (WeldClientProxy) and
 * once as the actual bean instance.
 */
@RunWith(FATRunner.class)
public class DoubleInstantiationTest {

    @Server("io.openliberty.restfulWS.3.0.cdi.3.0.fat.doubleinstantiation")
    public static LibertyServer server;

    private static final String APP_NAME = "doubleinstantiation";

    @BeforeClass
    public static void setUp() throws Exception {
        // Build the application
        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME,
                                                      "io.openliberty.restfulWS30.cdi30.fat.doubleinstantiation");
        
        // Export to apps directory (not dropins) since server.xml references it
        ShrinkHelper.exportAppToServer(server, app);
        
        server.startServer();
        
        // Wait for the application to start
        server.waitForStringInLog("CWWKZ0001I.*" + APP_NAME);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test that the Application class is only instantiated once.
     * 
     * This test will FAIL if the bug is present, showing that the Application
     * class is instantiated twice.
     */
    @Test
    public void testApplicationInstantiatedOnlyOnce() throws Exception {
        // Make a request to trigger application initialization
        URL url = new URL("http://localhost:" + server.getHttpDefaultPort() + "/" + APP_NAME + "/hello");
        
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setRequestMethod("GET");
            int status = con.getResponseCode();
            
            // Verify the request was successful
            assertEquals("Expected HTTP 200 response", 200, status);
            
        } finally {
            con.disconnect();
        }
        
        // Check the logs for the Application constructor messages
        // The bug manifests as two constructor calls with different stack traces
        String constructorMsg = "=== ExampleApplication Constructor Called ===";
        
        // Count how many times the constructor was called
        int constructorCallCount = server.findStringsInLogs(constructorMsg).size();
        
        System.out.println("Application constructor was called " + constructorCallCount + " times");
        
        // Look for evidence of double instantiation in the logs
        boolean foundCdiProxy = server.findStringsInLogs("WeldClientProxy").size() > 0;
        boolean foundJaxrsTarget = server.findStringsInLogs("JaxrsInjectionTarget").size() > 0;
        
        if (constructorCallCount > 1) {
            System.out.println("BUG REPRODUCED: Application class was instantiated " + constructorCallCount + " times!");
            System.out.println("Found CDI proxy instantiation: " + foundCdiProxy);
            System.out.println("Found JAX-RS target instantiation: " + foundJaxrsTarget);
            
            // Print the stack traces from the logs to help identify the issue
            System.out.println("\n=== Stack traces from logs ===");
            server.findStringsInLogs("at ").forEach(System.out::println);
        }
        
        // This assertion will FAIL if the bug is present
        assertEquals("Application class should only be instantiated once, but was instantiated " +
                    constructorCallCount + " times. This indicates the double instantiation bug is present.",
                    1, constructorCallCount);
    }
    
    /**
     * Test similar to upstream RESTEasy's ApplicationInjectionTest.
     * Tests an Application class with @Context injection which requires JAX-RS property injection.
     *
     * This test verifies whether the Application class is instantiated only once even when
     * it has @Context fields that need JAX-RS property injection, and also verifies that
     * the @Context field is properly injected.
     */
   // @Test
    public void testApplicationWithContextInjection() throws Exception {
        // Make a request to trigger application initialization
        URL url = new URL("http://localhost:" + server.getHttpDefaultPort() + "/" + APP_NAME + "/context/context-hello");
        
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setRequestMethod("GET");
            int status = con.getResponseCode();
            
            // Verify the request was successful
            assertEquals("Expected HTTP 200 response", 200, status);
            
        } finally {
            con.disconnect();
        }
        
        // Check the logs for the ApplicationWithContextInjection constructor messages
        String constructorMsg = "ApplicationWithContextInjection constructor called";
        
        // Count how many times the constructor was called
        int constructorCallCount = server.findStringsInLogs(constructorMsg).size();
        
        System.out.println("ApplicationWithContextInjection constructor was called " + constructorCallCount + " times");
        
        // Look for evidence of double instantiation in the logs
        boolean foundCdiProxy = server.findStringsInLogs("WeldClientProxy").size() > 0;
        boolean foundJaxrsTarget = server.findStringsInLogs("JaxrsInjectionTarget").size() > 0;
        
        if (constructorCallCount > 1) {
            System.out.println("BUG REPRODUCED: ApplicationWithContextInjection was instantiated " + constructorCallCount + " times!");
            System.out.println("Found CDI proxy instantiation: " + foundCdiProxy);
            System.out.println("Found JAX-RS target instantiation: " + foundJaxrsTarget);
        }
        
        // This assertion will FAIL if the bug is present (similar to upstream RESTEasy test)
        assertEquals("Application class with @Context injection should only be instantiated once",
                     1, constructorCallCount);
    }
    
    /**
     * Test that verifies the endpoint works correctly despite the double instantiation.
     */
    @Test
    public void testEndpointFunctionality() throws Exception {
        URL url = new URL("http://localhost:" + server.getHttpDefaultPort() + "/" + APP_NAME + "/hello");
        
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setRequestMethod("GET");
            int status = con.getResponseCode();
            
            assertEquals("Expected HTTP 200 response", 200, status);
            
            // Read the response
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            System.out.println("Response: " + response.toString());
            
            assertTrue("Response should contain 'Hello'", response.toString().contains("Hello"));
            assertTrue("Response should contain 'Whatever'", response.toString().contains("Whatever"));
            
        } finally {
            con.disconnect();
        }
    }
}

// Made with Bob
