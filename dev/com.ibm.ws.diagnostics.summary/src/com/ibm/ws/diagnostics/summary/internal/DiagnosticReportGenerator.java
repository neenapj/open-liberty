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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.diagnostics.summary.DiagnosticEvent;
import com.ibm.ws.diagnostics.summary.DiagnosticReport;
import com.ibm.ws.diagnostics.summary.DiagnosticReport.RootCauseAnalysis;
import com.ibm.ws.diagnostics.summary.DiagnosticReport.TraceRecommendation;

/**
 * Generates comprehensive diagnostic reports from aggregated events.
 * Orchestrates root cause analysis, trace recommendations, and report assembly.
 */
public class DiagnosticReportGenerator {

    private static final Logger logger = Logger.getLogger(DiagnosticReportGenerator.class.getName());

    private final RootCauseAnalyzer rootCauseAnalyzer;
    private final TraceRecommendationEngine traceRecommendationEngine;
    private final String serverName;
    private final String libertyVersion;
    private final String javaVersion;

    public DiagnosticReportGenerator() {
        this.rootCauseAnalyzer = new RootCauseAnalyzer();
        this.traceRecommendationEngine = new TraceRecommendationEngine();
        this.serverName = System.getProperty("wlp.server.name", "defaultServer");
        this.libertyVersion = System.getProperty("liberty.version", "Unknown");
        this.javaVersion = System.getProperty("java.version", "Unknown");
    }

    /**
     * Generate a diagnostic report for immediate trigger
     *
     * @param triggeringEvent the event that triggered the report
     * @param aggregator event aggregator with historical data
     * @param timeWindow time window to analyze
     * @return diagnostic report
     */
    public DiagnosticReport generateImmediateReport(DiagnosticEvent triggeringEvent,
                                                    EventAggregator aggregator,
                                                    Duration timeWindow) {
        logger.info("Generating immediate diagnostic report for event: " + triggeringEvent.getEventId());

        Instant end = Instant.now();
        Instant start = end.minus(timeWindow);

        return generateReport(DiagnosticReport.TriggerType.IMMEDIATE, start, end, aggregator);
    }

    /**
     * Generate a diagnostic report for scheduled analysis
     *
     * @param aggregator event aggregator with historical data
     * @param timeWindow time window to analyze
     * @return diagnostic report
     */
    public DiagnosticReport generateScheduledReport(EventAggregator aggregator, Duration timeWindow) {
        logger.info("Generating scheduled diagnostic report");

        Instant end = Instant.now();
        Instant start = end.minus(timeWindow);

        return generateReport(DiagnosticReport.TriggerType.SCHEDULED, start, end, aggregator);
    }

    /**
     * Generate a diagnostic report for manual trigger
     *
     * @param aggregator event aggregator with historical data
     * @param start start of analysis window
     * @param end end of analysis window
     * @return diagnostic report
     */
    public DiagnosticReport generateManualReport(EventAggregator aggregator, Instant start, Instant end) {
        logger.info("Generating manual diagnostic report");

        return generateReport(DiagnosticReport.TriggerType.MANUAL, start, end, aggregator);
    }

    /**
     * Core report generation logic
     */
    private DiagnosticReport generateReport(DiagnosticReport.TriggerType triggerType,
                                           Instant start, Instant end,
                                           EventAggregator aggregator) {
        try {
            // Get events in time window
            List<DiagnosticEvent> events = aggregator.getEventsInWindow(start, end);
            Map<String, List<DiagnosticEvent>> groupedEvents = aggregator.getGroupedEventsInWindow(start, end);

            logger.info(String.format("Analyzing %d events across %d unique signatures",
                    events.size(), groupedEvents.size()));

            // Generate root cause analyses
            List<RootCauseAnalysis> rootCauseAnalyses = generateRootCauseAnalyses(groupedEvents);

            // Generate trace recommendations
            List<TraceRecommendation> traceRecommendations = generateTraceRecommendations(groupedEvents);

            // Generate executive summary
            String executiveSummary = generateExecutiveSummary(events, groupedEvents, rootCauseAnalyses);

            // Generate recommended actions
            List<String> recommendedActions = generateRecommendedActions(rootCauseAnalyses, traceRecommendations);

            // Build report
            DiagnosticReportImpl report = new DiagnosticReportImpl.Builder()
                    .triggerType(triggerType)
                    .analysisWindow(start, end)
                    .serverName(serverName)
                    .libertyVersion(libertyVersion)
                    .javaVersion(javaVersion)
                    .executiveSummary(executiveSummary)
                    .events(events)
                    .groupedEvents(groupedEvents)
                    .rootCauseAnalyses(rootCauseAnalyses)
                    .traceRecommendations(traceRecommendations)
                    .recommendedActions(recommendedActions)
                    .build();

            logger.info("Diagnostic report generated successfully: " + report.getReportId());
            return report;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error generating diagnostic report", e);
            throw new RuntimeException("Failed to generate diagnostic report", e);
        }
    }

    /**
     * Generate root cause analyses for all event groups
     */
    private List<RootCauseAnalysis> generateRootCauseAnalyses(Map<String, List<DiagnosticEvent>> groupedEvents) {
        List<RootCauseAnalysis> analyses = new ArrayList<>();

        for (Map.Entry<String, List<DiagnosticEvent>> entry : groupedEvents.entrySet()) {
            try {
                RootCauseAnalysis analysis = rootCauseAnalyzer.analyze(entry.getKey(), entry.getValue());
                if (analysis != null) {
                    analyses.add(analysis);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error analyzing signature: " + entry.getKey(), e);
            }
        }

        // Sort by confidence score (highest first)
        analyses.sort((a, b) -> Integer.compare(b.getConfidenceScore(), a.getConfidenceScore()));

        return analyses;
    }

    /**
     * Generate trace recommendations for all event groups
     */
    private List<TraceRecommendation> generateTraceRecommendations(Map<String, List<DiagnosticEvent>> groupedEvents) {
        List<TraceRecommendation> recommendations = new ArrayList<>();

        for (Map.Entry<String, List<DiagnosticEvent>> entry : groupedEvents.entrySet()) {
            try {
                List<TraceRecommendation> groupRecs = traceRecommendationEngine.generateRecommendations(
                        entry.getKey(), entry.getValue());
                recommendations.addAll(groupRecs);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error generating trace recommendations for: " + entry.getKey(), e);
            }
        }

        // Sort by confidence score (highest first)
        recommendations.sort((a, b) -> Integer.compare(b.getConfidenceScore(), a.getConfidenceScore()));

        // Limit to top 10 recommendations
        if (recommendations.size() > 10) {
            recommendations = recommendations.subList(0, 10);
        }

        return recommendations;
    }

    /**
     * Generate executive summary
     */
    private String generateExecutiveSummary(List<DiagnosticEvent> events,
                                           Map<String, List<DiagnosticEvent>> groupedEvents,
                                           List<RootCauseAnalysis> rootCauseAnalyses) {
        StringBuilder summary = new StringBuilder();

        // Overview
        summary.append(String.format("Detected %d diagnostic events across %d unique issue signatures. ",
                events.size(), groupedEvents.size()));

        // Severity breakdown
        Map<DiagnosticEvent.Severity, Long> severityCounts = new java.util.HashMap<>();
        for (DiagnosticEvent event : events) {
            severityCounts.merge(event.getSeverity(), 1L, Long::sum);
        }

        if (!severityCounts.isEmpty()) {
            summary.append("Severity breakdown: ");
            List<String> severityParts = new ArrayList<>();
            for (Map.Entry<DiagnosticEvent.Severity, Long> entry : severityCounts.entrySet()) {
                severityParts.add(String.format("%d %s", entry.getValue(), entry.getKey()));
            }
            summary.append(String.join(", ", severityParts)).append(". ");
        }

        // Top issues
        if (!rootCauseAnalyses.isEmpty()) {
            summary.append(String.format("Identified %d root causes with confidence scores. ",
                    rootCauseAnalyses.size()));

            // Mention top issue
            RootCauseAnalysis topIssue = rootCauseAnalyses.get(0);
            summary.append(String.format("Primary issue: %s (confidence: %d%%). ",
                    topIssue.getRootCause(), topIssue.getConfidenceScore()));
        }

        // Recommendation
        summary.append("Review root cause analyses and apply recommended trace specifications for detailed diagnostics.");

        return summary.toString();
    }

    /**
     * Generate recommended actions
     */
    private List<String> generateRecommendedActions(List<RootCauseAnalysis> rootCauseAnalyses,
                                                    List<TraceRecommendation> traceRecommendations) {
        List<String> actions = new ArrayList<>();

        // Add immediate actions from root cause analyses
        if (!rootCauseAnalyses.isEmpty()) {
            actions.add("Review root cause analysis for primary issue: " +
                       rootCauseAnalyses.get(0).getRootCause());

            // Add top suggested fix
            RootCauseAnalysis topAnalysis = rootCauseAnalyses.get(0);
            if (!topAnalysis.getSuggestedFixes().isEmpty()) {
                actions.add("Immediate action: " + topAnalysis.getSuggestedFixes().get(0));
            }
        }

        // Add trace recommendations
        if (!traceRecommendations.isEmpty()) {
            actions.add("Enable recommended trace specification: " +
                       traceRecommendations.get(0).getTraceString());
            actions.add("Collect logs with trace enabled and review for additional details");
        }

        // Add general recommendations
        actions.add("Review Liberty messages.log and FFDC logs for additional context");
        actions.add("Check for recent configuration or application changes");

        if (rootCauseAnalyses.stream().anyMatch(a -> a.getConfidenceScore() < 70)) {
            actions.add("Consider contacting IBM Support with this diagnostic report and trace logs");
        }

        return actions;
    }
}

// Made with Bob
