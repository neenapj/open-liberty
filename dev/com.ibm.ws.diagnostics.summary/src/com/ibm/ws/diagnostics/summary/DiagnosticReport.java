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
import java.util.Map;

/**
 * Represents a comprehensive diagnostic report generated from
 * analysis of one or more diagnostic events.
 */
public interface DiagnosticReport {

    /**
     * Report trigger types
     */
    enum TriggerType {
        IMMEDIATE,
        SCHEDULED,
        MANUAL
    }

    /**
     * Get the unique identifier for this report
     *
     * @return report ID
     */
    String getReportId();

    /**
     * Get the timestamp when this report was generated
     *
     * @return generation timestamp
     */
    Instant getGeneratedAt();

    /**
     * Get the trigger type that caused this report to be generated
     *
     * @return trigger type
     */
    TriggerType getTriggerType();

    /**
     * Get the analysis time window start
     *
     * @return start timestamp
     */
    Instant getAnalysisWindowStart();

    /**
     * Get the analysis time window end
     *
     * @return end timestamp
     */
    Instant getAnalysisWindowEnd();

    /**
     * Get the server name
     *
     * @return server name
     */
    String getServerName();

    /**
     * Get the Liberty version
     *
     * @return Liberty version string
     */
    String getLibertyVersion();

    /**
     * Get the Java version
     *
     * @return Java version string
     */
    String getJavaVersion();

    /**
     * Get the executive summary
     *
     * @return executive summary text
     */
    String getExecutiveSummary();

    /**
     * Get all diagnostic events included in this report
     *
     * @return list of events (never null)
     */
    List<DiagnosticEvent> getEvents();

    /**
     * Get grouped events by signature
     *
     * @return map of signature to event list
     */
    Map<String, List<DiagnosticEvent>> getGroupedEvents();

    /**
     * Get the root cause analysis results
     *
     * @return list of root cause analyses
     */
    List<RootCauseAnalysis> getRootCauseAnalyses();

    /**
     * Get trace recommendations
     *
     * @return list of trace recommendations
     */
    List<TraceRecommendation> getTraceRecommendations();

    /**
     * Get related APARs from IBM Support
     *
     * @return list of related APARs
     */
    List<ExternalReference> getRelatedAPARs();

    /**
     * Get related GitHub issues
     *
     * @return list of related GitHub issues
     */
    List<ExternalReference> getRelatedGitHubIssues();

    /**
     * Get related documentation links
     *
     * @return list of documentation references
     */
    List<ExternalReference> getRelatedDocumentation();

    /**
     * Get recommended next actions
     *
     * @return list of recommended actions
     */
    List<String> getRecommendedActions();

    /**
     * Get the PDF report content, if generated
     *
     * @return PDF bytes or null
     */
    byte[] getPdfContent();

    /**
     * Get the JSON representation of this report
     *
     * @return JSON string
     */
    String toJson();

    /**
     * Root cause analysis result
     */
    interface RootCauseAnalysis {
        String getSignature();
        String getRootCause();
        String getExplanation();
        int getConfidenceScore();
        List<String> getAffectedComponents();
        List<String> getSuggestedFixes();
    }

    /**
     * Trace recommendation
     */
    interface TraceRecommendation {
        String getTraceString();
        String getReason();
        String getLogVolumeEstimate();
        String getPerformanceImpact();
        int getConfidenceScore();
        List<String> getApplicationMethods();
    }

    /**
     * External reference (APAR, GitHub issue, documentation)
     */
    interface ExternalReference {
        String getId();
        String getTitle();
        String getUrl();
        String getDescription();
        int getRelevanceScore();
        String getStatus();
    }
}

// Made with Bob
