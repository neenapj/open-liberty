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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.diagnostics.summary.DiagnosticEvent;
import com.ibm.ws.diagnostics.summary.DiagnosticEvent.EventType;
import com.ibm.ws.diagnostics.summary.DiagnosticEvent.Severity;
import com.ibm.ws.ffdc.FFDC;
import com.ibm.wsspi.logging.Incident;
import com.ibm.wsspi.logging.IncidentForwarder;

/**
 * Detects diagnostic events from various Liberty runtime sources:
 * - Exception handlers
 * - FFDC collectors
 * - MicroProfile Health checks
 * - MicroProfile Fault Tolerance circuit breakers
 * - Server lifecycle events
 *
 * This component uses OSGi Declarative Services to integrate with Liberty's
 * event infrastructure.
 */
@Component(
    configurationPid = "com.ibm.ws.diagnostics.summary",
    configurationPolicy = ConfigurationPolicy.OPTIONAL,
    immediate = true,
    service = DiagnosticEventDetector.class
)
public class DiagnosticEventDetector {

    private static final Logger logger = Logger.getLogger(DiagnosticEventDetector.class.getName());

    private final Set<DiagnosticEventListener> listeners = new CopyOnWriteArraySet<>();
    private final EventAggregator eventAggregator;
    private final ExceptionSignatureAnalyzer signatureAnalyzer;
    private final FFDCIncidentForwarder incidentForwarder;

    private volatile boolean enabled = true;
    private volatile String serverName;
    private volatile TriggerConfig triggerConfig;

    /**
     * Configuration for trigger thresholds
     */
    public static class TriggerConfig {
        private final boolean exceptionTriggerEnabled;
        private final int exceptionThreshold;
        private final Duration exceptionTimeWindow;
        private final Set<Severity> exceptionSeverities;

        public TriggerConfig(boolean exceptionTriggerEnabled, int exceptionThreshold,
                           Duration exceptionTimeWindow, Set<Severity> exceptionSeverities) {
            this.exceptionTriggerEnabled = exceptionTriggerEnabled;
            this.exceptionThreshold = exceptionThreshold;
            this.exceptionTimeWindow = exceptionTimeWindow;
            this.exceptionSeverities = exceptionSeverities;
        }

        public boolean isExceptionTriggerEnabled() {
            return exceptionTriggerEnabled;
        }

        public int getExceptionThreshold() {
            return exceptionThreshold;
        }

        public Duration getExceptionTimeWindow() {
            return exceptionTimeWindow;
        }

        public Set<Severity> getExceptionSeverities() {
            return exceptionSeverities;
        }
    }

    /**
     * Listener interface for diagnostic events
     */
    public interface DiagnosticEventListener {
        void onEvent(DiagnosticEvent event);
        void onImmediateTrigger(DiagnosticEvent triggeringEvent, String reason);
    }

    public DiagnosticEventDetector() {
        this.eventAggregator = new EventAggregator(Duration.ofHours(24));
        this.signatureAnalyzer = new ExceptionSignatureAnalyzer();
        this.incidentForwarder = new FFDCIncidentForwarder();
    }

    @Activate
    protected void activate(Map<String, Object> properties) {
        logger.info("Activating DiagnosticEventDetector");

        // Extract server name from system properties
        this.serverName = System.getProperty("wlp.server.name", "defaultServer");

        // Initialize trigger configuration from properties
        this.triggerConfig = createTriggerConfig(properties);

        // Register FFDC incident forwarder to automatically capture exceptions
        boolean registered = FFDC.registerIncidentForwarder(incidentForwarder);
        if (registered) {
            logger.info("Successfully registered FFDC IncidentForwarder");
        } else {
            logger.warning("Failed to register FFDC IncidentForwarder");
        }

        enabled = true;
        logger.info("DiagnosticEventDetector activated for server: " + serverName);
    }

    @Deactivate
    protected void deactivate() {
        logger.info("Deactivating DiagnosticEventDetector");

        // Deregister FFDC incident forwarder
        boolean deregistered = FFDC.deregisterIncidentForwarder(incidentForwarder);
        if (deregistered) {
            logger.info("Successfully deregistered FFDC IncidentForwarder");
        } else {
            logger.warning("Failed to deregister FFDC IncidentForwarder");
        }

        enabled = false;
        listeners.clear();
        eventAggregator.clear();
    }

    /**
     * Register a listener for diagnostic events
     *
     * @param listener the listener
     */
    public void addListener(DiagnosticEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Unregister a listener
     *
     * @param listener the listener
     */
    public void removeListener(DiagnosticEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Detect and process an exception event
     *
     * @param throwable the exception
     * @param sourceComponent the component that threw the exception
     * @param severity the severity level
     */
    public void detectException(Throwable throwable, String sourceComponent, Severity severity) {
        if (!enabled) {
            return;
        }

        try {
            // Generate signature
            String signature = signatureAnalyzer.generateSignature(
                new DiagnosticEventImpl.Builder()
                    .eventType(EventType.EXCEPTION)
                    .exception(throwable)
                    .severity(severity)
                    .build()
            );

            // Create event
            DiagnosticEvent event = new DiagnosticEventImpl.Builder()
                    .eventType(EventType.EXCEPTION)
                    .severity(severity)
                    .sourceComponent(sourceComponent)
                    .exception(throwable)
                    .serverName(serverName)
                    .signature(signature)
                    .build();

            processEvent(event);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error detecting exception event", e);
        }
    }

    /**
     * Detect and process an FFDC event
     *
     * @param throwable the exception
     * @param sourceComponent the component
     * @param ffdcId the FFDC incident ID
     */
    public void detectFFDC(Throwable throwable, String sourceComponent, String ffdcId) {
        logger.info("=== detectFFDC() called ===");
        logger.info("  enabled: " + enabled);
        logger.info("  throwable: " + (throwable != null ? throwable.getClass().getName() : "null"));
        logger.info("  sourceComponent: " + sourceComponent);
        logger.info("  ffdcId: " + ffdcId);

        if (!enabled) {
            logger.info("  Detector not enabled, returning");
            return;
        }

        try {
            logger.info("  Generating signature...");
            String signature = signatureAnalyzer.generateSignature(
                new DiagnosticEventImpl.Builder()
                    .eventType(EventType.FFDC)
                    .exception(throwable)
                    .severity(Severity.HIGH)
                    .build()
            );
            logger.info("  Signature: " + signature);

            logger.info("  Building DiagnosticEvent...");
            DiagnosticEvent event = new DiagnosticEventImpl.Builder()
                    .eventType(EventType.FFDC)
                    .severity(Severity.HIGH)
                    .sourceComponent(sourceComponent)
                    .exception(throwable)
                    .serverName(serverName)
                    .signature(signature)
                    .addContext("ffdcId", ffdcId)
                    .build();

            logger.info("  Processing event...");
            processEvent(event);
            logger.info("  Event processed successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "ERROR detecting FFDC event", e);
        }
    }

    /**
     * Detect and process a health check failure
     *
     * @param healthCheckName the name of the health check
     * @param reason the failure reason
     */
    public void detectHealthCheckFailure(String healthCheckName, String reason) {
        if (!enabled) {
            return;
        }

        try {
            String signature = "TYPE:HEALTH_CHECK_FAILURE|CHECK:" + healthCheckName;

            DiagnosticEvent event = new DiagnosticEventImpl.Builder()
                    .eventType(EventType.HEALTH_CHECK_FAILURE)
                    .severity(Severity.HIGH)
                    .sourceComponent("MicroProfile Health")
                    .serverName(serverName)
                    .signature(signature)
                    .addContext("healthCheckName", healthCheckName)
                    .addContext("reason", reason)
                    .build();

            processEvent(event);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error detecting health check failure", e);
        }
    }

    /**
     * Detect and process a circuit breaker open event
     *
     * @param methodName the method with circuit breaker
     * @param failureCount the number of failures
     */
    public void detectCircuitBreakerOpen(String methodName, int failureCount) {
        if (!enabled) {
            return;
        }

        try {
            String signature = "TYPE:CIRCUIT_BREAKER_OPEN|METHOD:" + methodName;

            DiagnosticEvent event = new DiagnosticEventImpl.Builder()
                    .eventType(EventType.CIRCUIT_BREAKER_OPEN)
                    .severity(Severity.MEDIUM)
                    .sourceComponent("MicroProfile Fault Tolerance")
                    .serverName(serverName)
                    .signature(signature)
                    .addContext("methodName", methodName)
                    .addContext("failureCount", failureCount)
                    .build();

            processEvent(event);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error detecting circuit breaker event", e);
        }
    }

    /**
     * Process a diagnostic event
     */
    private void processEvent(DiagnosticEvent event) {
        logger.info("=== processEvent() called ===");
        logger.info("  Event type: " + event.getEventType());
        logger.info("  Severity: " + event.getSeverity());
        logger.info("  Signature: " + event.getSignature());

        // Add to aggregator
        logger.info("  Adding to aggregator...");
        eventAggregator.addEvent(event);
        logger.info("  Added to aggregator");

        // Notify listeners
        logger.info("  Notifying " + listeners.size() + " listeners...");
        for (DiagnosticEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error notifying listener", e);
            }
        }
        logger.info("  Listeners notified");

        // Check for immediate triggers
        logger.info("  Checking immediate triggers...");
        checkImmediateTriggers(event);
        logger.info("  Trigger check complete");
    }

    /**
     * Check if event should trigger immediate report generation
     */
    private void checkImmediateTriggers(DiagnosticEvent event) {
        if (triggerConfig == null || !triggerConfig.isExceptionTriggerEnabled()) {
            return;
        }

        // Check exception threshold trigger
        if (event.getEventType() == EventType.EXCEPTION || event.getEventType() == EventType.FFDC) {
            if (triggerConfig.getExceptionSeverities().contains(event.getSeverity())) {
                String signature = event.getSignature();
                int count = eventAggregator.getEventCount(signature, triggerConfig.getExceptionTimeWindow());

                if (count >= triggerConfig.getExceptionThreshold()) {
                    String reason = String.format(
                        "Exception threshold exceeded: %d occurrences of %s in %s",
                        count, signature, triggerConfig.getExceptionTimeWindow()
                    );

                    for (DiagnosticEventListener listener : listeners) {
                        try {
                            listener.onImmediateTrigger(event, reason);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Error notifying listener of immediate trigger", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the event aggregator
     *
     * @return event aggregator
     */
    public EventAggregator getEventAggregator() {
        return eventAggregator;
    }

    /**
     * Update trigger configuration
     *
     * @param config new configuration
     */
    public void updateTriggerConfig(TriggerConfig config) {
        this.triggerConfig = config;
    }

    /**
     * Create trigger configuration from properties map
     */
    private TriggerConfig createTriggerConfig(Map<String, Object> properties) {
        boolean exceptionTriggerEnabled = getBoolean(properties, "immediateTriggers.exceptionTrigger.enabled", true);
        int exceptionThreshold = getInt(properties, "immediateTriggers.exceptionTrigger.threshold", 5);
        Duration exceptionTimeWindow = parseDuration(
            getString(properties, "immediateTriggers.exceptionTrigger.timeWindow", "10m")
        );
        Set<Severity> severities = parseSeverities(
            getString(properties, "immediateTriggers.exceptionTrigger.severity", "CRITICAL,HIGH")
        );

        return new TriggerConfig(exceptionTriggerEnabled, exceptionThreshold, exceptionTimeWindow, severities);
    }

    private boolean getBoolean(Map<String, Object> properties, String key, boolean defaultValue) {
        Object value = properties.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    private int getInt(Map<String, Object> properties, String key, int defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private String getString(Map<String, Object> properties, String key, String defaultValue) {
        Object value = properties.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    private Duration parseDuration(String durationStr) {
        // Parse duration strings like "10m", "1h", "30s"
        if (durationStr.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(durationStr.substring(0, durationStr.length() - 1)));
        } else if (durationStr.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(durationStr.substring(0, durationStr.length() - 1)));
        } else if (durationStr.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(durationStr.substring(0, durationStr.length() - 1)));
        }
        return Duration.ofMinutes(10); // default
    }

    private Set<Severity> parseSeverities(String severitiesStr) {
        Set<Severity> severities = new java.util.HashSet<>();
        for (String s : severitiesStr.split(",")) {
            try {
                severities.add(Severity.valueOf(s.trim()));
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid severity: " + s);
            }
        }
        return severities;
    }

    /**
     * Inner class that implements IncidentForwarder to capture FFDC incidents
     */
    private class FFDCIncidentForwarder implements IncidentForwarder {

        @Override
        public void process(Incident incident, Throwable th) {
            logger.info("=== FFDC IncidentForwarder.process() CALLED ===");
            logger.info("  enabled=" + enabled + ", throwable=" + (th != null ? th.getClass().getName() : "null"));

            if (!enabled || th == null) {
                logger.info("  Skipping: enabled=" + enabled + ", throwable is null=" + (th == null));
                return;
            }

            try {
                // Extract information from the incident
                String sourceId = incident.getSourceId();
                String probeId = incident.getProbeId();
                String exceptionName = incident.getExceptionName();
                int count = incident.getCount();

                logger.info("  Incident details:");
                logger.info("    sourceId: " + sourceId);
                logger.info("    probeId: " + probeId);
                logger.info("    exceptionName: " + exceptionName);
                logger.info("    count: " + count);
                logger.info("    label: " + incident.getLabel());

                // Only process first occurrence to avoid duplicates
                if (count > 1) {
                    logger.info("  Skipping duplicate FFDC incident: " + exceptionName + " (count=" + count + ")");
                    return;
                }

                logger.info("  Processing FFDC incident: " + exceptionName +
                           " from " + sourceId + ":" + probeId);

                // Determine source component from sourceId
                String sourceComponent = extractComponentName(sourceId);
                logger.info("  Extracted component: " + sourceComponent);

                // Create incident ID from label or generate one
                String incidentId = incident.getLabel();
                if (incidentId == null || incidentId.isEmpty()) {
                    incidentId = String.format("%s-%d", exceptionName, incident.getTimeStamp());
                }
                logger.info("  Incident ID: " + incidentId);

                // Call the existing detectFFDC method
                logger.info("  Calling detectFFDC()...");
                detectFFDC(th, sourceComponent, incidentId);
                logger.info("  detectFFDC() completed successfully");

            } catch (Exception e) {
                logger.log(Level.SEVERE, "ERROR processing FFDC incident", e);
            }
        }

        /**
         * Extract component name from source ID
         * Example: "com.ibm.ws.jaxrs.2.0.server.LibertyJaxRsServerFactoryBean" -> "jaxrs"
         */
        private String extractComponentName(String sourceId) {
            if (sourceId == null || sourceId.isEmpty()) {
                return "unknown";
            }

            // Try to extract component from package name
            // Pattern: com.ibm.ws.<component>.*
            if (sourceId.startsWith("com.ibm.ws.")) {
                String remainder = sourceId.substring("com.ibm.ws.".length());
                int dotIndex = remainder.indexOf('.');
                if (dotIndex > 0) {
                    return remainder.substring(0, dotIndex);
                }
                return remainder;
            }

            // For other packages, use the last segment of the class name
            int lastDot = sourceId.lastIndexOf('.');
            if (lastDot > 0 && lastDot < sourceId.length() - 1) {
                return sourceId.substring(lastDot + 1);
            }

            return sourceId;
        }
    }
}

// Made with Bob
