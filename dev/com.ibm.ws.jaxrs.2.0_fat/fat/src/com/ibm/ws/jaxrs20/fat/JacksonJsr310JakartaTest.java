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
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * FAT test that confirms an application-packaged
 * {@code com.fasterxml.jackson.jakarta.rs} provider overrides the container
 * JSON-B/Yasson provider under restfulWS-3.1.
 *
 * <h2>Coverage gap this test closes</h2>
 * <p>Prior Jackson BYO-provider tests ({@link JacksonPOJOwithUserJacksonLib2xTest},
 * {@link JacksonJsr310Test}) run under {@code jaxrs-2.0} / {@code jaxrs-2.1}
 * with the {@code javax.ws.rs} namespace. This test runs the same scenario on
 * {@code restfulWS-3.1} with the {@code jakarta.ws.rs} namespace, validating
 * the provider-precedence mechanism that customers must rely on when migrating
 * from traditional WebSphere (Jackson + javax.*) to Liberty 25+ (restfulWS-3.1
 * + Jakarta + JSON-B by default).
 *
 * <h2>How provider precedence works</h2>
 * <p>The auto-feature {@code io.openliberty.restfulWS3.0-jsonb2.0} installs
 * {@code io.openliberty.restfulWS30.jsonb20provider.JsonBProvider} as a
 * container built-in with no {@code @Priority}. RESTEasy registers application
 * providers at {@code Priorities.USER} (1000) and, critically, marks them as
 * {@code isBuiltin=false}. The {@code SortedKey} comparator sorts non-built-in
 * providers ahead of built-ins for the same media type, so the application
 * provider wins without any Liberty-specific configuration.
 *
 * <h2>What is tested</h2>
 * <ul>
 *   <li>GET — serialization: Jackson writes {@code LocalDate} as ISO-8601
 *       string, not as a JSON-B bean.</li>
 *   <li>POST echo — deserialization + re-serialization round-trip for a
 *       single DTO.</li>
 *   <li>POST list echo — collection deserialization round-trip
 *       ({@code List<OrderDto>}).</li>
 *   <li>GET list — collection serialization.</li>
 * </ul>
 *
 * <h2>Deployment</h2>
 * <p>The WAR bundles {@code jackson-jakarta-rs-json-provider},
 * {@code jackson-databind}, and {@code jackson-datatype-jsr310} in
 * {@code WEB-INF/lib} — exactly as the customer does when migrating.
 * The server has both {@code restfulWS-3.1} and {@code jsonb-3.0} active to
 * confirm that the auto-feature JSON-B provider is present but bypassed.
 *
 * @see JacksonJsr310Test   (jaxrs-2.1 equivalent)
 * @see JacksonPOJOwithUserJacksonLib2xTest (jaxrs-2.0 equivalent)
 */
@RunWith(FATRunner.class)
public class JacksonJsr310JakartaTest extends AbstractTest {

    /**
     * Always run once without modification (local runs included), then add an
     * EE10 full-FAT-only repeat so the suite also covers the feature-replaced
     * configuration in CI.
     */
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
            .andWith(FeatureReplacementAction.EE10_FEATURES()
                    .alwaysAddFeature("jsonb-3.0")
                    .removeFeature("jsonb-2.0")
                    .fullFATOnly());

    @Server("com.ibm.ws.jaxrs.fat.jacksonJsr310Jakarta")
    public static LibertyServer server;

    private static final String WAR_NAME = "jacksonJsr310Jakarta";
    private static final String TARGET = WAR_NAME + "/TestServlet";

    /** jackson2x libs (databind + jaxrs-base + jaxrs-json-provider). */
    private static final String JACKSON2X_LIBS = "appLibs/jackson2x/";

    /** jackson-jakarta-rs-json-provider and supporting jars. */
    private static final String JACKSON_JAKARTA_LIBS = "appLibs/jacksonJakarta/";

    /** jackson-datatype-jsr310. */
    private static final String JSR310_LIBS = "appLibs/jsr310/";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(WAR_NAME,
                "com.ibm.ws.jaxrs.fat.jacksonjsr310jakarta");

        // Add jackson-databind (needed by both provider flavours)
        app.addAsLibraries(new File(JACKSON2X_LIBS).listFiles());

        // Add the Jakarta-namespace Jackson provider jars
        app.addAsLibraries(new File(JACKSON_JAKARTA_LIBS).listFiles());

        // Add jackson-datatype-jsr310 for LocalDate support
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
    // Provider precedence tests
    // -------------------------------------------------------------------------

    /**
     * GETs a single order and asserts that Jackson (not JSON-B) serialized
     * the {@code LocalDate} field. The Jackson provider must have been
     * selected over the container JSON-B provider.
     */
    @Test
    public void testJacksonProviderOverridesJsonb() throws Exception {
        runTestOnServer(TARGET, "testJacksonProviderOverridesJsonb", null, "OK");
    }

    /**
     * POSTs a single {@code OrderDto} and asserts the echo round-trip
     * succeeds — confirms both deserialization and re-serialization work
     * under restfulWS-3.1 with the application-packaged Jackson provider.
     */
    @Test
    public void testJacksonPostRoundTrip() throws Exception {
        runTestOnServer(TARGET, "testJacksonPostRoundTrip", null, "OK");
    }

    /**
     * POSTs a {@code List<OrderDto>} and asserts the echo round-trip
     * succeeds. Exercises the {@code CollectionDeserializer} path that
     * was the root cause of the customer stack trace in TS022989586.
     */
    @Test
    public void testJacksonListRoundTrip() throws Exception {
        runTestOnServer(TARGET, "testJacksonListRoundTrip", null, "OK");
    }

    /**
     * GETs a list of orders and confirms collection serialization works
     * with the application Jackson provider under restfulWS-3.1.
     */
    @Test
    public void testJacksonGetList() throws Exception {
        runTestOnServer(TARGET, "testJacksonGetList", null, "OK");
    }
}
