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
package com.ibm.ws.diagnostics.summary.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.ibm.ws.diagnostics.summary.DiagnosticEvent;
import com.ibm.ws.diagnostics.summary.DiagnosticReport.TraceRecommendation;
import com.ibm.ws.diagnostics.summary.internal.DiagnosticReportImpl.TraceRecommendationImpl;

/**
 * Generates trace string recommendations based on diagnostic events.
 * Provides confidence-ranked trace specifications with log volume estimates
 * and performance impact warnings.
 */
public class TraceRecommendationEngine {

    // Map of component patterns to trace specifications
    private static final Map<Pattern, TraceSpec> TRACE_SPECS = new HashMap<>();

    static {
        // Database/JDBC tracing
        TRACE_SPECS.put(
            Pattern.compile(".*jdbc.*|.*database.*|.*datasource.*", Pattern.CASE_INSENSITIVE),
            new TraceSpec(
                "com.ibm.ws.jdbc.*=all:com.ibm.ws.rsadapter.*=all",
                "Captures detailed JDBC connection pool operations, SQL statement execution, and transaction management",
                "MEDIUM",
                "LOW",
                Arrays.asList(
                    "Add to bootstrap.properties: com.ibm.ws.logging.trace.specification=com.ibm.ws.jdbc.*=all:com.ibm.ws.rsadapter.*=all",
                    "Add to server.xml: <logging traceSpecification=\"com.ibm.ws.jdbc.*=all:com.ibm.ws.rsadapter.*=all\"/>",
                    "Set via jvm.options: -Dcom.ibm.ws.logging.trace.specification=com.ibm.ws.jdbc.*=all:com.ibm.ws.rsadapter.*=all"
                ),
                90
            )
        );

        // JAX-RS/REST tracing
        TRACE_SPECS.put(
            Pattern.compile(".*jaxrs.*|.*rest.*|.*webapplication.*", Pattern.CASE_INSENSITIVE),
            new TraceSpec(
                "com.ibm.ws.jaxrs.*=all:com.ibm.ws.jaxrs20.*=all",
                "Captures REST endpoint invocations, request/response processing, and exception handling",
                "HIGH",
                "MEDIUM",
                Arrays.asList(
                    "Add to bootstrap.properties: com.ibm.ws.logging.trace.specification=com.ibm.ws.jaxrs.*=all",
                    "Add to server.xml: <logging traceSpecification=\"com.ibm.ws.jaxrs.*=all\"/>",
                    "For detailed HTTP: Add *=info:HTTPChannel=all:GenericBNF=all:HTTPDispatcher=all"
                ),
                85
            )
        );

        // SSL/TLS tracing
        TRACE_SPECS.put(
            Pattern.compile(".*ssl.*|.*tls.*|.*certificate.*|.*handshake.*", Pattern.CASE_INSENSITIVE),
            new TraceSpec(
                "com.ibm.ws.ssl.*=all:com.ibm.ws.security.ssl.*=all",
                "Captures SSL/TLS handshake details, certificate validation, and cipher suite negotiation",
                "HIGH",
                "LOW",
                Arrays.asList(
                    "Add to bootstrap.properties: com.ibm.ws.logging.trace.specification=com.ibm.ws.ssl.*=all",
                    "Add to server.xml: <logging traceSpecification=\"com.ibm.ws.ssl.*=all\"/>",
                    "For JVM SSL debug: Add to jvm.options: -Djavax.net.debug=ssl:handshake"
                ),
                90
            )
        );

        // Security/Authentication tracing
        TRACE_SPECS.put(
            Pattern.compile(".*authentication.*|.*authorization.*|.*security.*|.*login.*", Pattern.CASE_INSENSITIVE),
            new TraceSpec(
                "com.ibm.ws.security.*=all:com.ibm.ws.webcontainer.security.*=all",
                "Captures authentication flows, authorization decisions, and security context propagation",
                "MEDIUM",
                "MEDIUM",
                Arrays.asList(
                    "Add to bootstrap.properties: com.ibm.ws.logging.trace.specification=com.ibm.ws.security.*=all",
                    "Add to server.xml: <logging traceSpecification=\"com.ibm.ws.security.*=all\"/>",
                    "For LDAP: Add com.ibm.ws.security.registry.ldap.*=all"
                ),
                85
            )
        );

        // Transaction tracing
        TRACE_SPECS.put(
            Pattern.compile(".*transaction.*|.*xa.*|.*2pc.*", Pattern.CASE_INSENSITIVE),
            new TraceSpec(
                "com.ibm.ws.transaction.*=all:com.ibm.tx.*=all",
                "Captures transaction lifecycle, XA resource coordination, and commit/rollback operations",
                "MEDIUM",
                "LOW",
                Arrays.asList(
                    "Add to bootstrap.properties: com.ibm.ws.logging.trace.specification=com.ibm.ws.transaction.*=all",
                    "Add to server.xml: <logging traceSpecification=\"com.ibm.ws.transaction.*=all\"/>",
                    "For detailed XA: Add com.ibm.ws.Transaction.JTA=all"
                ),
                85
            )
        );

        // JNDI/Naming tracing
        TRACE_SPECS.put(
            Pattern.compile(".*jndi.*|.*naming.*|.*lookup.*", Pattern.CASE_INSENSITIVE),
            new TraceSpec(
                "com.ibm.ws.naming.*=all:com.ibm.ws.jndi.*=all",
                "Captures JNDI lookups, naming context operations, and resource binding",
                "LOW",
                "LOW",
                Arrays.asList(
                    "Add to bootstrap.properties: com.ibm.ws.logging.trace.specification=com.ibm.ws.naming.*=all",
                    "Add to server.xml: <logging traceSpecification=\"com.ibm.ws.naming.*=all\"/>"
                ),
                80
            )
        );

        // CDI tracing
        TRACE_SPECS.put(
            Pattern.compile(".*cdi.*|.*injection.*|.*bean.*", Pattern.CASE_INSENSITIVE),
            new TraceSpec(
                "com.ibm.ws.cdi.*=all:com.ibm.ws.webbeans.*=all",
                "Captures CDI bean discovery, injection, and lifecycle events",
                "HIGH",
                "MEDIUM",
                Arrays.asList(
                    "Add to bootstrap.properties: com.ibm.ws.logging.trace.specification=com.ibm.ws.cdi.*=all",
                    "Add to server.xml: <logging traceSpecification=\"com.ibm.ws.cdi.*=all\"/>"
                ),
                80
            )
        );

        // JPA/Persistence tracing
        TRACE_SPECS.put(
            Pattern.compile(".*jpa.*|.*persistence.*|.*entity.*", Pattern.CASE_INSENSITIVE),
            new TraceSpec(
                "com.ibm.ws.jpa.*=all:org.eclipse.persistence.*=all",
                "Captures JPA entity operations, query execution, and persistence context management",
                "HIGH",
                "MEDIUM",
                Arrays.asList(
                    "Add to bootstrap.properties: com.ibm.ws.logging.trace.specification=com.ibm.ws.jpa.*=all",
                    "Add to server.xml: <logging traceSpecification=\"com.ibm.ws.jpa.*=all\"/>",
                    "For SQL logging: Add eclipselink.logging.level=FINEST to persistence.xml"
                ),
                85
            )
        );

        // MicroProfile Health tracing
        TRACE_SPECS.put(
            Pattern.compile(".*health.*check.*|.*liveness.*|.*readiness.*", Pattern.CASE_INSENSITIVE),
            new TraceSpec(
                "com.ibm.ws.microprofile.health.*=all",
                "Captures health check execution, status evaluation, and endpoint invocations",
                "LOW",
                "LOW",
                Arrays.asList(
                    "Add to bootstrap.properties: com.ibm.ws.logging.trace.specification=com.ibm.ws.microprofile.health.*=all",
                    "Add to server.xml: <logging traceSpecification=\"com.ibm.ws.microprofile.health.*=all\"/>"
                ),
                85
            )
        );

        // MicroProfile Fault Tolerance tracing
        TRACE_SPECS.put(
            Pattern.compile(".*circuit.*breaker.*|.*retry.*|.*timeout.*|.*bulkhead.*", Pattern.CASE_INSENSITIVE),
            new TraceSpec(
                "com.ibm.ws.microprofile.faulttolerance.*=all",
                "Captures circuit breaker state changes, retry attempts, and timeout handling",
                "MEDIUM",
                "LOW",
                Arrays.asList(
                    "Add to bootstrap.properties: com.ibm.ws.logging.trace.specification=com.ibm.ws.microprofile.faulttolerance.*=all",
                    "Add to server.xml: <logging traceSpecification=\"com.ibm.ws.microprofile.faulttolerance.*=all\"/>"
                ),
                85
            )
        );

        // ClassLoader tracing
        TRACE_SPECS.put(
            Pattern.compile(".*classloader.*|.*classnotfound.*|.*noclassdef.*", Pattern.CASE_INSENSITIVE),
            new TraceSpec(
                "com.ibm.ws.classloading.*=all:com.ibm.ws.app.manager.*=all",
                "Captures class loading operations, delegation, and application deployment",
                "HIGH",
                "HIGH",
                Arrays.asList(
                    "Add to bootstrap.properties: com.ibm.ws.logging.trace.specification=com.ibm.ws.classloading.*=all",
                    "Add to server.xml: <logging traceSpecification=\"com.ibm.ws.classloading.*=all\"/>",
                    "WARNING: This generates very high log volume"
                ),
                80
            )
        );
    }

    /**
     * Generate trace recommendations for a group of events
     *
     * @param signature event signature
     * @param events list of events with this signature
     * @return list of trace recommendations
     */
    public List<TraceRecommendation> generateRecommendations(String signature, List<DiagnosticEvent> events) {
        List<TraceRecommendation> recommendations = new ArrayList<>();

        if (events.isEmpty()) {
            return recommendations;
        }

        // Get representative event
        DiagnosticEvent representativeEvent = events.get(0);

        // Try pattern matching
        TraceRecommendation patternMatch = matchByPattern(representativeEvent);
        if (patternMatch != null) {
            recommendations.add(patternMatch);
        }

        // Add component-specific trace if available
        if (representativeEvent.getSourceComponent() != null) {
            TraceRecommendation componentTrace = generateComponentTrace(representativeEvent.getSourceComponent());
            if (componentTrace != null && !isDuplicate(recommendations, componentTrace)) {
                recommendations.add(componentTrace);
            }
        }

        // Add generic comprehensive trace if no specific matches
        if (recommendations.isEmpty()) {
            recommendations.add(generateGenericTrace());
        }

        return recommendations;
    }

    /**
     * Match event against trace specification patterns
     */
    private TraceRecommendation matchByPattern(DiagnosticEvent event) {
        String textToMatch = buildMatchText(event);

        for (Map.Entry<Pattern, TraceSpec> entry : TRACE_SPECS.entrySet()) {
            if (entry.getKey().matcher(textToMatch).find()) {
                TraceSpec spec = entry.getValue();
                return new TraceRecommendationImpl(
                    spec.traceString,
                    spec.reason,
                    spec.logVolumeEstimate,
                    spec.performanceImpact,
                    spec.confidenceScore,
                    spec.applicationMethods
                );
            }
        }

        return null;
    }

    /**
     * Build text for pattern matching
     */
    private String buildMatchText(DiagnosticEvent event) {
        StringBuilder text = new StringBuilder();

        if (event.getSourceComponent() != null) {
            text.append(event.getSourceComponent()).append(" ");
        }
        if (event.getExceptionClassName() != null) {
            text.append(event.getExceptionClassName()).append(" ");
        }
        if (event.getExceptionMessage() != null) {
            text.append(event.getExceptionMessage()).append(" ");
        }
        if (event.getStackTrace() != null) {
            text.append(event.getStackTrace());
        }

        return text.toString();
    }

    /**
     * Generate component-specific trace
     */
    private TraceRecommendation generateComponentTrace(String component) {
        String traceString = component + ".*=all";
        String reason = "Enables detailed tracing for the " + component + " component";

        return new TraceRecommendationImpl(
            traceString,
            reason,
            "MEDIUM",
            "LOW",
            70,
            Arrays.asList(
                "Add to bootstrap.properties: com.ibm.ws.logging.trace.specification=" + traceString,
                "Add to server.xml: <logging traceSpecification=\"" + traceString + "\"/>"
            )
        );
    }

    /**
     * Generate generic comprehensive trace
     */
    private TraceRecommendation generateGenericTrace() {
        return new TraceRecommendationImpl(
            "*=info:com.ibm.ws.*=all",
            "Comprehensive trace for general troubleshooting when specific component is unknown",
            "VERY HIGH",
            "HIGH",
            50,
            Arrays.asList(
                "Add to bootstrap.properties: com.ibm.ws.logging.trace.specification=*=info:com.ibm.ws.*=all",
                "Add to server.xml: <logging traceSpecification=\"*=info:com.ibm.ws.*=all\"/>",
                "WARNING: This generates very high log volume and should only be used temporarily"
            )
        );
    }

    /**
     * Check if recommendation is duplicate
     */
    private boolean isDuplicate(List<TraceRecommendation> recommendations, TraceRecommendation newRec) {
        for (TraceRecommendation existing : recommendations) {
            if (existing.getTraceString().equals(newRec.getTraceString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper class to store trace specification information
     */
    private static class TraceSpec {
        final String traceString;
        final String reason;
        final String logVolumeEstimate;
        final String performanceImpact;
        final List<String> applicationMethods;
        final int confidenceScore;

        TraceSpec(String traceString, String reason, String logVolumeEstimate,
                 String performanceImpact, List<String> applicationMethods, int confidenceScore) {
            this.traceString = traceString;
            this.reason = reason;
            this.logVolumeEstimate = logVolumeEstimate;
            this.performanceImpact = performanceImpact;
            this.applicationMethods = applicationMethods;
            this.confidenceScore = confidenceScore;
        }
    }
}

// Made with Bob
