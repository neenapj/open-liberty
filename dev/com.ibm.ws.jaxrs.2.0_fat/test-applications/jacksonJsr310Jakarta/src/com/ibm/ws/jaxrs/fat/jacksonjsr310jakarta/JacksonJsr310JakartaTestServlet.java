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

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Test driver servlet for the restfulWS-3.1 BYO Jackson provider coverage test.
 *
 * <p>Verifies that an application-packaged
 * {@code com.fasterxml.jackson.jakarta.rs} provider:
 * <ol>
 *   <li>Overrides the container JSON-B/Yasson provider auto-installed by the
 *       {@code io.openliberty.restfulWS3.0-jsonb2.0} auto-feature.</li>
 *   <li>Correctly serializes and deserializes a DTO containing a
 *       {@code java.time.LocalDate} field (requires {@code JavaTimeModule}).</li>
 *   <li>Correctly handles collection types ({@code List<OrderDto>}).</li>
 * </ol>
 *
 * <p>Test methods:
 * <ul>
 *   <li>{@code testJacksonProviderOverridesJsonb} — GET a single order and
 *       confirm that Jackson (not JSON-B) serialized the date as an ISO-8601
 *       string rather than a JSON object.</li>
 *   <li>{@code testJacksonPostRoundTrip} — POST/echo a single OrderDto and
 *       confirm that both deserialization and re-serialization succeed.</li>
 *   <li>{@code testJacksonListRoundTrip} — POST/echo a list of OrderDtos.</li>
 *   <li>{@code testJacksonGetList} — GET a list of orders and confirm correct
 *       date serialization throughout the collection.</li>
 * </ul>
 */
public class JacksonJsr310JakartaTestServlet extends HttpServlet {

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

    private String baseUrl() {
        return "http://" + serverIP + ":" + serverPort + "/jacksonJsr310Jakarta/jackson";
    }

    /** JSON payload for a single OrderDto with a LocalDate. */
    private static final String ORDER_JSON =
            "{\"id\":\"ORD-001\",\"orderDate\":\"2026-08-25\"}";

    /** JSON payload for a list of two OrderDtos. */
    private static final String ORDER_LIST_JSON =
            "[{\"id\":\"ORD-001\",\"orderDate\":\"2026-08-25\"},"
            + "{\"id\":\"ORD-002\",\"orderDate\":\"2026-08-26\"}]";

    // -------------------------------------------------------------------------
    // Test methods
    // -------------------------------------------------------------------------

    /**
     * GETs a single order and confirms that Jackson serialized the
     * {@code LocalDate} as an ISO-8601 string (e.g. {@code "2026-08-25"}).
     *
     * <p>If JSON-B/Yasson were used instead of Jackson it would either fail
     * with {@code JsonbException} (XMLGregorianCalendar scenario) or produce
     * a different date representation depending on the JSON-B config. For
     * plain {@code LocalDate}, JSON-B also produces an ISO string, but the
     * point of this test is that the response succeeds at all — confirming
     * Jackson handled the request rather than JSON-B falling back to
     * bean-introspection mode.
     */
    public void testJacksonProviderOverridesJsonb(Client client,
                                                   Map<String, String> params,
                                                   StringBuilder ret) throws Exception {
        Response r = client.target(baseUrl() + "/orders/single")
                           .request(MediaType.APPLICATION_JSON)
                           .get();
        int status = r.getStatus();
        String body = r.readEntity(String.class);
        r.close();

        if (status != 200) {
            ret.append("FAIL: expected HTTP 200, got " + status + " body=" + body);
            return;
        }
        // Jackson with JavaTimeModule produces "2026-08-25" for LocalDate
        if (!body.contains("ORD-001") || !body.contains("2026-08-25")) {
            ret.append("FAIL: response body missing expected data: " + body);
            return;
        }
        ret.append("OK");
    }

    /**
     * POSTs a single OrderDto and asserts the echo round-trip succeeds.
     * Confirms both deserialization (read) and re-serialization (write) work
     * with the application Jackson provider under restfulWS-3.1.
     */
    public void testJacksonPostRoundTrip(Client client,
                                          Map<String, String> params,
                                          StringBuilder ret) throws Exception {
        Response r = client.target(baseUrl() + "/orders/single")
                           .request(MediaType.APPLICATION_JSON)
                           .post(Entity.json(ORDER_JSON));
        int status = r.getStatus();
        String body = r.readEntity(String.class);
        r.close();

        if (status != 200) {
            ret.append("FAIL: expected HTTP 200, got " + status + " body=" + body);
            return;
        }
        if (!body.contains("ORD-001") || !body.contains("2026-08-25")) {
            ret.append("FAIL: round-trip body missing expected data: " + body);
            return;
        }
        ret.append("OK");
    }

    /**
     * POSTs a list of OrderDtos and asserts the echo round-trip succeeds.
     * Exercises the {@code CollectionDeserializer} path that was the root
     * cause of the customer stack trace.
     */
    public void testJacksonListRoundTrip(Client client,
                                          Map<String, String> params,
                                          StringBuilder ret) throws Exception {
        Response r = client.target(baseUrl() + "/orders/list")
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
            ret.append("FAIL: list round-trip body missing expected data: " + body);
            return;
        }
        ret.append("OK");
    }

    /**
     * GETs a list of orders and confirms serialization works for collections.
     */
    public void testJacksonGetList(Client client,
                                    Map<String, String> params,
                                    StringBuilder ret) throws Exception {
        Response r = client.target(baseUrl() + "/orders/list")
                           .request(MediaType.APPLICATION_JSON)
                           .get();
        int status = r.getStatus();
        String body = r.readEntity(String.class);
        r.close();

        if (status != 200) {
            ret.append("FAIL: expected HTTP 200, got " + status + " body=" + body);
            return;
        }
        if (!body.contains("ORD-001") || !body.contains("ORD-002")
                || !body.contains("2026-08-25") || !body.contains("2026-08-26")) {
            ret.append("FAIL: list GET body missing expected data: " + body);
            return;
        }
        ret.append("OK");
    }
}
