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
package com.ibm.ws.diagnostics.summary;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main service interface for the Diagnostic Summary feature.
 * This service coordinates event detection, analysis, report generation,
 * and delivery.
 */
public interface DiagnosticSummaryService {

    /**
     * Register a diagnostic event for analysis
     *
     * @param event the diagnostic event
     */
    void registerEvent(DiagnosticEvent event);

    /**
     * Manually trigger a diagnostic report generation
     *
     * @return future that completes when report is generated and delivered
     */
    CompletableFuture<DiagnosticReport> triggerManualReport();

    /**
     * Manually trigger a diagnostic report for a specific time window
     *
     * @param start start of analysis window
     * @param end end of analysis window
     * @return future that completes when report is generated and delivered
     */
    CompletableFuture<DiagnosticReport> triggerManualReport(Instant start, Instant end);

    /**
     * Get the current configuration
     *
     * @return configuration object
     */
    DiagnosticSummaryConfig getConfiguration();

    /**
     * Update the configuration
     *
     * @param config new configuration
     */
    void updateConfiguration(DiagnosticSummaryConfig config);

    /**
     * Get recent diagnostic events
     *
     * @param limit maximum number of events to return
     * @return list of recent events
     */
    List<DiagnosticEvent> getRecentEvents(int limit);

    /**
     * Get recent diagnostic reports
     *
     * @param limit maximum number of reports to return
     * @return list of recent reports
     */
    List<DiagnosticReport> getRecentReports(int limit);

    /**
     * Get a specific report by ID
     *
     * @param reportId report identifier
     * @return report or null if not found
     */
    DiagnosticReport getReport(String reportId);

    /**
     * Check if the service is enabled
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Enable or disable the service
     *
     * @param enabled true to enable, false to disable
     */
    void setEnabled(boolean enabled);

    /**
     * Get service statistics
     *
     * @return statistics object
     */
    ServiceStatistics getStatistics();

    /**
     * Service statistics
     */
    interface ServiceStatistics {
        long getTotalEventsDetected();
        long getTotalReportsGenerated();
        long getTotalReportsDelivered();
        long getTotalReportsFailed();
        Instant getLastReportGeneratedAt();
        Instant getServiceStartedAt();
        long getAverageReportGenerationTimeMs();
        long getAverageReportDeliveryTimeMs();
    }
}

// Made with Bob
