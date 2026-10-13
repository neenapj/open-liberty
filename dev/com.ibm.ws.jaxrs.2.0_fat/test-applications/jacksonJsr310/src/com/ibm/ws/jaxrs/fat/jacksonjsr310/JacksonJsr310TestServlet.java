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
package com.ibm.ws.jaxrs.fat.jacksonjsr310;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Servlet that drives the replication scenario for the Jackson JSR-310 cache
 * poisoning bug (TS022882616).
 *
 * Two application paths are exposed by the same WAR:
 * <ul>
 *   <li>{@code /broken/orders/*} — uses {@link BrokenJacksonProvider} (no module)</li>
 *   <li>{@code /fixed/orders/*}  — uses {@link FixedJacksonProvider}  (module registered)</li>
 * </ul>
 *
 * Test methods:
 * <ul>
 *   <li>{@code testBrokenProviderFailsWithLocalDate} — proves the bug:
 *       a POST with a LocalDate field returns HTTP 500 on every call after
 *       the cache is poisoned, even on the very first call.</li>
 *   <li>{@code testBrokenProviderGetWithLocalDate} — shows serialisation
 *       (write path) also fails once the mapper is broken.</li>
 *   <li>{@code testFixedProviderSucceedsWithLocalDate} — proves the fix:
 *       POST round-trips correctly.</li>
 *   <li>{@code testBrokenCacheIsPermanent} — calls the broken endpoint twice
 *       to confirm the cache is permanently poisoned (not a transient error).</li>
 * </ul>
 */
public class JacksonJsr310TestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private String serverIP;
    private String serverPort;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PrintWriter pw = resp.getWriter();
        String testMethod = req.getParameter("test");
        if (testMethod == null) {
            pw.write("no test to run");
            return;
        }
        runTest(testMethod, pw, req);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

    private void runTest(String testMethod, PrintWriter pw, HttpServletRequest req) {
        serverIP = req.getLocalAddr();
        serverPort = String.valueOf(req.getLocalPort());

        Client client = ClientBuilder.newClient();
        try {
            Map<String, String> params = new HashMap<>();
            Method m = this.getClass().getDeclaredMethod(testMethod,
                                                         Client.class,
                                                         Map.class,
                                                         StringBuilder.class);
            StringBuilder result = new StringBuilder();
            m.invoke(this, client, params, result);
            pw.write(result.toString());
        } catch (InvocationTargetException e) {
            e.getCause().printStackTrace(pw);
        } catch (Exception e) {
            e.printStackTrace(pw);
        } finally {
            client.close();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String baseUrl(String appPath) {
        return "http://" + serverIP + ":" + serverPort + "/jacksonJsr310/" + appPath;
    }

    /** A JSON payload matching {@link OrderDto} with a LocalDate field. */
    private static final String ORDER_JSON =
            "{\"id\":\"ORD-001\",\"orderDate\":\"2026-08-25\"}";

    /** A JSON payload matching a list of two orders. */
    private static final String ORDER_LIST_JSON =
            "[{\"id\":\"ORD-001\",\"orderDate\":\"2026-08-25\"},"
            + "{\"id\":\"ORD-002\",\"orderDate\":\"2026-08-26\"}]";

    // -------------------------------------------------------------------------
    // BUG REPRODUCTION tests (broken provider)
    // -------------------------------------------------------------------------

    /**
     * Proves the bug: POST a single OrderDto (with LocalDate) to the broken
     * application. BeanDeserializerFactory._findUnsupportedTypeDeserializer()
     * fires because JavaTimeModule is not registered, and the response is 500.
     *
     * This is the exact failure path from the customer Javacore:
     *   BeanDeserializer.vanillaDeserialize()
     *     → MethodProperty.deserializeAndSet()
     *       → UnsupportedTypeDeserializer.deserialize()
     *         → DeserializationContext.reportBadDefinition() → 500
     */
    public void testBrokenProviderFailsWithLocalDate(Client client,
                                                      Map<String, String> params,
                                                      StringBuilder ret) {
        Response r = client.target(baseUrl("broken/orders/single"))
                           .request(MediaType.APPLICATION_JSON)
                           .post(Entity.json(ORDER_JSON));
        int status = r.getStatus();
        r.close();
        if (status == 500) {
            ret.append("OK"); // expected: reproduces the bug
        } else {
            ret.append("FAIL: expected HTTP 500 (bug), got " + status);
        }
    }

    /**
     * Calls the broken endpoint a SECOND time to confirm the DeserializerCache
     * remains poisoned — not a transient failure. The customer reported
     * "continues until restart".
     */
    public void testBrokenCacheIsPermanent(Client client,
                                            Map<String, String> params,
                                            StringBuilder ret) {
        // First call — poisons the cache
        Response r1 = client.target(baseUrl("broken/orders/single"))
                            .request(MediaType.APPLICATION_JSON)
                            .post(Entity.json(ORDER_JSON));
        int status1 = r1.getStatus();
        r1.close();

        // Second call — must still fail (cache hit on UnsupportedTypeDeserializer)
        Response r2 = client.target(baseUrl("broken/orders/single"))
                            .request(MediaType.APPLICATION_JSON)
                            .post(Entity.json(ORDER_JSON));
        int status2 = r2.getStatus();
        r2.close();

        if (status1 == 500 && status2 == 500) {
            ret.append("OK"); // both calls fail → cache is permanently poisoned
        } else {
            ret.append("FAIL: call1=" + status1 + " call2=" + status2
                       + " (expected both 500)");
        }
    }

    /**
     * Broken list endpoint — mirrors the customer's exact stack trace:
     *   CollectionDeserializer → BeanDeserializer → UnsupportedTypeDeserializer
     */
    public void testBrokenProviderListFailsWithLocalDate(Client client,
                                                          Map<String, String> params,
                                                          StringBuilder ret) {
        Response r = client.target(baseUrl("broken/orders/list"))
                           .request(MediaType.APPLICATION_JSON)
                           .post(Entity.json(ORDER_LIST_JSON));
        int status = r.getStatus();
        r.close();
        if (status == 500) {
            ret.append("OK"); // expected: reproduces the bug on list deserialization
        } else {
            ret.append("FAIL: expected HTTP 500 (bug), got " + status);
        }
    }

    // -------------------------------------------------------------------------
    // FIX VERIFICATION tests (fixed provider)
    // -------------------------------------------------------------------------

    /**
     * Proves the fix: POST a single OrderDto to the fixed application.
     * JavaTimeModule is registered before any request, so LocalDate
     * deserializes correctly and the response is 200.
     */
    public void testFixedProviderSucceedsWithLocalDate(Client client,
                                                        Map<String, String> params,
                                                        StringBuilder ret) throws Exception {
        Response r = client.target(baseUrl("fixed/orders/single"))
                           .request(MediaType.APPLICATION_JSON)
                           .post(Entity.json(ORDER_JSON));
        int status = r.getStatus();
        String body = r.readEntity(String.class);
        r.close();

        if (status != 200) {
            ret.append("FAIL: expected HTTP 200, got " + status + " body=" + body);
            return;
        }
        // Verify the echoed body contains both fields
        if (!body.contains("ORD-001") || !body.contains("2026-08-25")) {
            ret.append("FAIL: response body missing expected data: " + body);
            return;
        }
        ret.append("OK");
    }

    /**
     * Proves the fix on the list endpoint — mirrors the customer's
     * CollectionDeserializer path.
     */
    public void testFixedProviderListSucceedsWithLocalDate(Client client,
                                                            Map<String, String> params,
                                                            StringBuilder ret) throws Exception {
        Response r = client.target(baseUrl("fixed/orders/list"))
                           .request(MediaType.APPLICATION_JSON)
                           .post(Entity.json(ORDER_LIST_JSON));
        int status = r.getStatus();
        String body = r.readEntity(String.class);
        r.close();

        if (status != 200) {
            ret.append("FAIL: expected HTTP 200, got " + status + " body=" + body);
            return;
        }
        if (!body.contains("ORD-001") || !body.contains("ORD-002")) {
            ret.append("FAIL: response body missing expected data: " + body);
            return;
        }
        ret.append("OK");
    }

    /**
     * GET with fixed provider — confirms LocalDate serialization (write path)
     * also works correctly when JavaTimeModule is registered.
     */
    public void testFixedProviderGetWithLocalDate(Client client,
                                                   Map<String, String> params,
                                                   StringBuilder ret) throws Exception {
        Response r = client.target(baseUrl("fixed/orders/single"))
                           .request(MediaType.APPLICATION_JSON)
                           .get();
        int status = r.getStatus();
        String body = r.readEntity(String.class);
        r.close();

        if (status != 200) {
            ret.append("FAIL: expected HTTP 200, got " + status + " body=" + body);
            return;
        }
        if (!body.contains("2026-08-25")) {
            ret.append("FAIL: date not found in response: " + body);
            return;
        }
        ret.append("OK");
    }
}
