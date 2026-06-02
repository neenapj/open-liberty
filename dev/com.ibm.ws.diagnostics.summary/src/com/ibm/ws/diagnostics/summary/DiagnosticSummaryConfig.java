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

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Configuration for the Diagnostic Summary feature
 */
public interface DiagnosticSummaryConfig {

    /**
     * Check if the feature is enabled
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Get the mail session reference
     *
     * @return mail session ID
     */
    String getMailSessionRef();

    /**
     * Get email recipients
     *
     * @return list of recipients
     */
    List<EmailRecipient> getEmailRecipients();

    /**
     * Get immediate trigger configuration
     *
     * @return immediate triggers config
     */
    ImmediateTriggersConfig getImmediateTriggersConfig();

    /**
     * Get scheduled analysis configuration
     *
     * @return scheduled analysis config
     */
    ScheduledAnalysisConfig getScheduledAnalysisConfig();

    /**
     * Check if manual trigger is enabled
     *
     * @return true if manual trigger is enabled
     */
    boolean isManualTriggerEnabled();

    /**
     * Get report options
     *
     * @return report options
     */
    ReportOptions getReportOptions();

    /**
     * Get trace recommendations configuration
     *
     * @return trace recommendations config
     */
    TraceRecommendationsConfig getTraceRecommendationsConfig();

    /**
     * Get external content fetching configuration
     *
     * @return external content config
     */
    ExternalContentConfig getExternalContentConfig();

    /**
     * Email recipient configuration
     */
    interface EmailRecipient {
        String getEmail();
        RecipientType getType();

        enum RecipientType {
            TO, CC, BCC
        }
    }

    /**
     * Immediate triggers configuration
     */
    interface ImmediateTriggersConfig {
        boolean isExceptionTriggerEnabled();
        int getExceptionThreshold();
        Duration getExceptionTimeWindow();
        Set<DiagnosticEvent.Severity> getExceptionSeverities();
    }

    /**
     * Scheduled analysis configuration
     */
    interface ScheduledAnalysisConfig {
        boolean isEnabled();
        String getSchedule();
        String getTimezone();
        int getMinIssuesForReport();
    }

    /**
     * Report options
     */
    interface ReportOptions {
        boolean isIncludeFullStackTraces();
        boolean isGeneratePDF();
        boolean isPdfIncludeCharts();
    }

    /**
     * Trace recommendations configuration
     */
    interface TraceRecommendationsConfig {
        boolean isEnabled();
        boolean isIncludeLogVolume();
        boolean isIncludePerformanceImpact();
    }

    /**
     * External content fetching configuration
     */
    interface ExternalContentConfig {
        boolean isEnabled();
        String getGitHubToken();
        boolean isCacheEnabled();
        Duration getCacheTTL();
        Duration getTimeout();
    }
}

// Made with Bob
