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
import com.ibm.ws.diagnostics.summary.DiagnosticReport.RootCauseAnalysis;
import com.ibm.ws.diagnostics.summary.internal.DiagnosticReportImpl.RootCauseAnalysisImpl;

/**
 * Analyzes diagnostic events to determine root causes.
 * Uses pattern matching and heuristics to identify common issues.
 */
public class RootCauseAnalyzer {

    // Common exception patterns and their root causes
    private static final Map<Pattern, RootCausePattern> EXCEPTION_PATTERNS = new HashMap<>();

    static {
        // Database connection issues
        EXCEPTION_PATTERNS.put(
            Pattern.compile(".*SQLException.*Connection.*refused.*", Pattern.CASE_INSENSITIVE),
            new RootCausePattern(
                "Database Connection Refused",
                "The database server is not accepting connections. This could be due to the database being down, firewall rules blocking access, or incorrect connection configuration.",
                Arrays.asList("Database", "JDBC", "Connection Pool"),
                Arrays.asList(
                    "Verify database server is running",
                    "Check database connection URL, hostname, and port",
                    "Verify firewall rules allow connections",
                    "Check database user credentials",
                    "Review connection pool configuration"
                ),
                85
            )
        );

        EXCEPTION_PATTERNS.put(
            Pattern.compile(".*SQLException.*timeout.*", Pattern.CASE_INSENSITIVE),
            new RootCausePattern(
                "Database Connection Timeout",
                "Database queries are taking too long to complete or connections are timing out. This may indicate slow queries, insufficient database resources, or network latency.",
                Arrays.asList("Database", "JDBC", "Performance"),
                Arrays.asList(
                    "Review slow query logs",
                    "Optimize database queries and indexes",
                    "Increase connection timeout settings",
                    "Check database server resources (CPU, memory, I/O)",
                    "Review network latency between application and database"
                ),
                80
            )
        );

        // JNDI/Naming issues
        EXCEPTION_PATTERNS.put(
            Pattern.compile(".*NamingException.*Cannot find.*", Pattern.CASE_INSENSITIVE),
            new RootCausePattern(
                "JNDI Resource Not Found",
                "A JNDI resource lookup failed because the resource is not bound in the naming context. This typically indicates missing configuration or incorrect resource names.",
                Arrays.asList("JNDI", "Configuration", "Resource Management"),
                Arrays.asList(
                    "Verify resource is defined in server.xml",
                    "Check JNDI name matches lookup string",
                    "Ensure resource is started before application",
                    "Review application bindings configuration",
                    "Check for typos in resource names"
                ),
                90
            )
        );

        // SSL/TLS issues
        EXCEPTION_PATTERNS.put(
            Pattern.compile(".*SSLException.*certificate.*", Pattern.CASE_INSENSITIVE),
            new RootCausePattern(
                "SSL Certificate Validation Failure",
                "SSL/TLS certificate validation failed. This could be due to expired certificates, untrusted certificate authorities, or hostname mismatches.",
                Arrays.asList("SSL", "Security", "Certificates"),
                Arrays.asList(
                    "Verify certificate is not expired",
                    "Check certificate chain is complete",
                    "Ensure CA certificate is in truststore",
                    "Verify hostname matches certificate CN/SAN",
                    "Review SSL configuration in server.xml"
                ),
                85
            )
        );

        // Authentication/Authorization
        EXCEPTION_PATTERNS.put(
            Pattern.compile(".*AuthenticationException.*", Pattern.CASE_INSENSITIVE),
            new RootCausePattern(
                "Authentication Failure",
                "User authentication failed. This could be due to incorrect credentials, expired passwords, or misconfigured authentication mechanisms.",
                Arrays.asList("Security", "Authentication", "User Registry"),
                Arrays.asList(
                    "Verify user credentials are correct",
                    "Check user exists in configured registry",
                    "Review authentication mechanism configuration",
                    "Check for password expiration policies",
                    "Verify LDAP/database connectivity if using external registry"
                ),
                80
            )
        );

        // ClassLoader issues
        EXCEPTION_PATTERNS.put(
            Pattern.compile(".*ClassNotFoundException.*", Pattern.CASE_INSENSITIVE),
            new RootCausePattern(
                "Missing Class or Library",
                "A required class could not be found. This typically indicates missing JAR files, incorrect classloader configuration, or version conflicts.",
                Arrays.asList("ClassLoader", "Dependencies", "Application"),
                Arrays.asList(
                    "Verify required JAR files are in application",
                    "Check library references in server.xml",
                    "Review classloader configuration",
                    "Check for version conflicts between libraries",
                    "Ensure shared libraries are properly configured"
                ),
                85
            )
        );

        EXCEPTION_PATTERNS.put(
            Pattern.compile(".*NoClassDefFoundError.*", Pattern.CASE_INSENSITIVE),
            new RootCausePattern(
                "Class Definition Error",
                "A class was found during compilation but not at runtime. This often indicates missing dependencies or classloader issues.",
                Arrays.asList("ClassLoader", "Dependencies", "Application"),
                Arrays.asList(
                    "Check for missing transitive dependencies",
                    "Verify all required libraries are deployed",
                    "Review classloader delegation settings",
                    "Check for initialization failures in static blocks",
                    "Ensure compatible library versions"
                ),
                80
            )
        );

        // Memory issues
        EXCEPTION_PATTERNS.put(
            Pattern.compile(".*OutOfMemoryError.*", Pattern.CASE_INSENSITIVE),
            new RootCausePattern(
                "Out of Memory",
                "The JVM has exhausted available memory. This could be due to memory leaks, insufficient heap size, or excessive object creation.",
                Arrays.asList("Memory", "Performance", "JVM"),
                Arrays.asList(
                    "Increase JVM heap size (-Xmx)",
                    "Analyze heap dump for memory leaks",
                    "Review application for resource leaks (connections, streams)",
                    "Optimize object creation and caching",
                    "Consider using memory profiling tools"
                ),
                90
            )
        );

        // JAX-RS/REST issues
        EXCEPTION_PATTERNS.put(
            Pattern.compile(".*WebApplicationException.*404.*", Pattern.CASE_INSENSITIVE),
            new RootCausePattern(
                "REST Endpoint Not Found",
                "A REST endpoint could not be found. This may indicate incorrect URL paths, missing @Path annotations, or application not started.",
                Arrays.asList("JAX-RS", "REST", "Application"),
                Arrays.asList(
                    "Verify @Path annotations on resource classes and methods",
                    "Check application path configuration",
                    "Ensure REST application is started",
                    "Review URL path construction",
                    "Check for typos in endpoint paths"
                ),
                85
            )
        );

        // Transaction issues
        EXCEPTION_PATTERNS.put(
            Pattern.compile(".*TransactionException.*rollback.*", Pattern.CASE_INSENSITIVE),
            new RootCausePattern(
                "Transaction Rollback",
                "A transaction was rolled back, typically due to an exception or timeout. This may indicate business logic errors or long-running transactions.",
                Arrays.asList("Transactions", "Database", "Application"),
                Arrays.asList(
                    "Review application logs for underlying exceptions",
                    "Check transaction timeout settings",
                    "Optimize transaction scope and duration",
                    "Verify database constraints are not violated",
                    "Review transaction isolation levels"
                ),
                75
            )
        );
    }

    /**
     * Analyze a group of events with the same signature
     *
     * @param signature event signature
     * @param events list of events with this signature
     * @return root cause analysis
     */
    public RootCauseAnalysis analyze(String signature, List<DiagnosticEvent> events) {
        if (events.isEmpty()) {
            return null;
        }

        // Get a representative event
        DiagnosticEvent representativeEvent = events.get(0);

        // Try pattern matching first
        RootCauseAnalysis patternMatch = analyzeByPattern(representativeEvent);
        if (patternMatch != null) {
            return patternMatch;
        }

        // Fall back to generic analysis
        return analyzeGeneric(signature, representativeEvent, events);
    }

    /**
     * Analyze event using pattern matching
     */
    private RootCauseAnalysis analyzeByPattern(DiagnosticEvent event) {
        String exceptionMessage = event.getExceptionMessage();
        String stackTrace = event.getStackTrace();

        if (exceptionMessage == null && stackTrace == null) {
            return null;
        }

        String textToMatch = (exceptionMessage != null ? exceptionMessage : "") +
                            (stackTrace != null ? stackTrace : "");

        for (Map.Entry<Pattern, RootCausePattern> entry : EXCEPTION_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(textToMatch).matches()) {
                RootCausePattern pattern = entry.getValue();
                return new RootCauseAnalysisImpl(
                    event.getSignature(),
                    pattern.rootCause,
                    pattern.explanation,
                    pattern.confidenceScore,
                    pattern.affectedComponents,
                    pattern.suggestedFixes
                );
            }
        }

        return null;
    }

    /**
     * Generic analysis when no pattern matches
     */
    private RootCauseAnalysis analyzeGeneric(String signature, DiagnosticEvent event,
                                            List<DiagnosticEvent> events) {
        String rootCause = "Recurring " + event.getEventType() + ": " +
                          (event.getExceptionClassName() != null ? event.getExceptionClassName() : "Unknown");

        String explanation = String.format(
            "This issue has occurred %d times. The root cause could not be automatically determined. " +
            "Manual investigation is recommended using the provided trace recommendations.",
            events.size()
        );

        List<String> affectedComponents = new ArrayList<>();
        if (event.getSourceComponent() != null) {
            affectedComponents.add(event.getSourceComponent());
        }

        List<String> suggestedFixes = Arrays.asList(
            "Review application logs for additional context",
            "Enable detailed tracing using recommended trace strings",
            "Check for recent configuration or code changes",
            "Review Liberty documentation for this exception type",
            "Contact IBM Support if issue persists"
        );

        return new RootCauseAnalysisImpl(
            signature,
            rootCause,
            explanation,
            50, // Lower confidence for generic analysis
            affectedComponents,
            suggestedFixes
        );
    }

    /**
     * Helper class to store pattern matching information
     */
    private static class RootCausePattern {
        final String rootCause;
        final String explanation;
        final List<String> affectedComponents;
        final List<String> suggestedFixes;
        final int confidenceScore;

        RootCausePattern(String rootCause, String explanation, List<String> affectedComponents,
                        List<String> suggestedFixes, int confidenceScore) {
            this.rootCause = rootCause;
            this.explanation = explanation;
            this.affectedComponents = affectedComponents;
            this.suggestedFixes = suggestedFixes;
            this.confidenceScore = confidenceScore;
        }
    }
}

// Made with Bob
