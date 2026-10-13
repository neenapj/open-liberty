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
package com.ibm.ws.jaxrs20.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * FAT test that replicates the Jackson JSR-310 DeserializerCache poisoning
 * reported in customer case TS022882616 (also TS020729545, TS020467656,
 * TS019659860).
 *
 * <p>Root cause: when a {@code JacksonJaxbJsonProvider} is initialised with
 * an {@link com.fasterxml.jackson.databind.ObjectMapper} that has NO
 * {@code JavaTimeModule} registered, the first deserialization of a bean
 * with a {@code java.time.LocalDate} property causes
 * {@code BeanDeserializerFactory._findUnsupportedTypeDeserializer()} to
 * install an {@code UnsupportedTypeDeserializer} sentinel in the shared
 * {@code DeserializerCache}. That entry is never evicted, so all subsequent
 * requests for the same type fail with
 * {@code InvalidDefinitionException: Java 8 date/time type
 * java.time.LocalDate not supported by default}.
 *
 * <p>Fix: register {@code JavaTimeModule} on the {@code ObjectMapper} at
 * construction time, before any request thread can reach {@code readFrom()}.
 *
 * <p>The WAR bundles jackson-databind, jackson-jaxrs-json-provider, and
 * jackson-datatype-jsr310 in {@code WEB-INF/lib} — exactly as the customer
 * does. Two separate JAX-RS application paths are registered in the same WAR:
 * <ul>
 *   <li>{@code /broken/*} — {@link com.ibm.ws.jaxrs.fat.jacksonjsr310.BrokenJacksonProvider}:
 *       no module registered → reproduces the bug</li>
 *   <li>{@code /fixed/*}  — {@link com.ibm.ws.jaxrs.fat.jacksonjsr310.FixedJacksonProvider}:
 *       module registered at construction → verifies the fix</li>
 * </ul>
 */
@RunWith(FATRunner.class)
public class JacksonJsr310Test extends AbstractTest {

    @Server("com.ibm.ws.jaxrs.fat.jacksonJsr310")
    public static LibertyServer server;

    private static final String WAR_NAME = "jacksonJsr310";
    private static final String TARGET = WAR_NAME + "/TestServlet";

    /** jackson2x libs (databind + jaxrs provider) already built by addJackson2x Gradle task. */
    private static final String JACKSON2X_LIBS = "appLibs/jackson2x/";

    /**
     * jackson-datatype-jsr310 jar — built into the appLibs/jsr310/ directory
     * by the {@code addJsr310} Gradle task defined in build.gradle.
     */
    private static final String JSR310_LIBS = "appLibs/jsr310/";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(WAR_NAME,
                "com.ibm.ws.jaxrs.fat.jacksonjsr310");
        // Add all jackson jars (databind, jaxrs-base, jaxrs-json-provider,
        // datatype-jsr310) to WEB-INF/lib — mirrors the customer deployment.
        app.addAsLibraries(new File(JACKSON2X_LIBS).listFiles());
        File jsr310Dir = new File(JSR310_LIBS);
        if (jsr310Dir.exists()) {
            app.addAsLibraries(jsr310Dir.listFiles());
        }

        ShrinkHelper.exportAppToServer(server, app);
        server.addInstalledAppForValidation(WAR_NAME);
        server.startServer(true);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    // -------------------------------------------------------------------------
    // Bug reproduction tests
    // -------------------------------------------------------------------------

    /**
     * Proves the bug: a POST with a LocalDate field to the broken provider
     * results in HTTP 500 (InvalidDefinitionException).
     */
    @Test
    public void testBrokenProviderFailsWithLocalDate() throws Exception {
        runTestOnServer(TARGET, "testBrokenProviderFailsWithLocalDate", null, "OK");
    }

    /**
     * Confirms the DeserializerCache poisoning is permanent: calling the
     * broken endpoint twice both fail — cache hit on UnsupportedTypeDeserializer.
     */
    @Test
    public void testBrokenCacheIsPermanent() throws Exception {
        runTestOnServer(TARGET, "testBrokenCacheIsPermanent", null, "OK");
    }

    /**
     * Proves the bug on the List endpoint — mirrors the customer's exact
     * CollectionDeserializer → BeanDeserializer stack trace.
     */
    @Test
    public void testBrokenProviderListFailsWithLocalDate() throws Exception {
        runTestOnServer(TARGET, "testBrokenProviderListFailsWithLocalDate", null, "OK");
    }

    // -------------------------------------------------------------------------
    // Fix verification tests
    // -------------------------------------------------------------------------

    /**
     * Proves the fix: POST a single OrderDto with a LocalDate to the
     * correctly configured provider succeeds with HTTP 200.
     */
    @Test
    public void testFixedProviderSucceedsWithLocalDate() throws Exception {
        runTestOnServer(TARGET, "testFixedProviderSucceedsWithLocalDate", null, "OK");
    }

    /**
     * Proves the fix on the List endpoint — collection round-trip succeeds.
     */
    @Test
    public void testFixedProviderListSucceedsWithLocalDate() throws Exception {
        runTestOnServer(TARGET, "testFixedProviderListSucceedsWithLocalDate", null, "OK");
    }

    /**
     * Confirms GET (serialisation) also works correctly when JavaTimeModule
     * is registered.
     */
    @Test
    public void testFixedProviderGetWithLocalDate() throws Exception {
        runTestOnServer(TARGET, "testFixedProviderGetWithLocalDate", null, "OK");
    }
}
