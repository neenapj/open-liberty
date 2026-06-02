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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ibm.ws.diagnostics.summary.DiagnosticEvent;
import com.ibm.ws.diagnostics.summary.DiagnosticReport;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * Implementation of DiagnosticReport interface
 */
public class DiagnosticReportImpl implements DiagnosticReport {

    private final String reportId;
    private final Instant generatedAt;
    private final TriggerType triggerType;
    private final Instant analysisWindowStart;
    private final Instant analysisWindowEnd;
    private final String serverName;
    private final String libertyVersion;
    private final String javaVersion;
    private final String executiveSummary;
    private final List<DiagnosticEvent> events;
    private final Map<String, List<DiagnosticEvent>> groupedEvents;
    private final List<RootCauseAnalysis> rootCauseAnalyses;
    private final List<TraceRecommendation> traceRecommendations;
    private final List<ExternalReference> relatedAPARs;
    private final List<ExternalReference> relatedGitHubIssues;
    private final List<ExternalReference> relatedDocumentation;
    private final List<String> recommendedActions;
    private byte[] pdfContent;

    private DiagnosticReportImpl(Builder builder) {
        this.reportId = builder.reportId != null ? builder.reportId : UUID.randomUUID().toString();
        this.generatedAt = builder.generatedAt != null ? builder.generatedAt : Instant.now();
        this.triggerType = builder.triggerType;
        this.analysisWindowStart = builder.analysisWindowStart;
        this.analysisWindowEnd = builder.analysisWindowEnd;
        this.serverName = builder.serverName;
        this.libertyVersion = builder.libertyVersion;
        this.javaVersion = builder.javaVersion;
        this.executiveSummary = builder.executiveSummary;
        this.events = Collections.unmodifiableList(new ArrayList<>(builder.events));
        this.groupedEvents = Collections.unmodifiableMap(new HashMap<>(builder.groupedEvents));
        this.rootCauseAnalyses = Collections.unmodifiableList(new ArrayList<>(builder.rootCauseAnalyses));
        this.traceRecommendations = Collections.unmodifiableList(new ArrayList<>(builder.traceRecommendations));
        this.relatedAPARs = Collections.unmodifiableList(new ArrayList<>(builder.relatedAPARs));
        this.relatedGitHubIssues = Collections.unmodifiableList(new ArrayList<>(builder.relatedGitHubIssues));
        this.relatedDocumentation = Collections.unmodifiableList(new ArrayList<>(builder.relatedDocumentation));
        this.recommendedActions = Collections.unmodifiableList(new ArrayList<>(builder.recommendedActions));
        this.pdfContent = builder.pdfContent;
    }

    @Override
    public String getReportId() {
        return reportId;
    }

    @Override
    public Instant getGeneratedAt() {
        return generatedAt;
    }

    @Override
    public TriggerType getTriggerType() {
        return triggerType;
    }

    @Override
    public Instant getAnalysisWindowStart() {
        return analysisWindowStart;
    }

    @Override
    public Instant getAnalysisWindowEnd() {
        return analysisWindowEnd;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public String getLibertyVersion() {
        return libertyVersion;
    }

    @Override
    public String getJavaVersion() {
        return javaVersion;
    }

    @Override
    public String getExecutiveSummary() {
        return executiveSummary;
    }

    @Override
    public List<DiagnosticEvent> getEvents() {
        return events;
    }

    @Override
    public Map<String, List<DiagnosticEvent>> getGroupedEvents() {
        return groupedEvents;
    }

    @Override
    public List<RootCauseAnalysis> getRootCauseAnalyses() {
        return rootCauseAnalyses;
    }

    @Override
    public List<TraceRecommendation> getTraceRecommendations() {
        return traceRecommendations;
    }

    @Override
    public List<ExternalReference> getRelatedAPARs() {
        return relatedAPARs;
    }

    @Override
    public List<ExternalReference> getRelatedGitHubIssues() {
        return relatedGitHubIssues;
    }

    @Override
    public List<ExternalReference> getRelatedDocumentation() {
        return relatedDocumentation;
    }

    @Override
    public List<String> getRecommendedActions() {
        return recommendedActions;
    }

    @Override
    public byte[] getPdfContent() {
        return pdfContent != null ? pdfContent.clone() : null;
    }

    @Override
    public String toJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("reportId", reportId)
                .add("generatedAt", generatedAt.toString())
                .add("triggerType", triggerType.name())
                .add("analysisWindowStart", analysisWindowStart.toString())
                .add("analysisWindowEnd", analysisWindowEnd.toString())
                .add("serverName", serverName)
                .add("libertyVersion", libertyVersion)
                .add("javaVersion", javaVersion)
                .add("executiveSummary", executiveSummary)
                .add("totalEvents", events.size())
                .add("uniqueSignatures", groupedEvents.size());

        // Add root cause analyses
        JsonArrayBuilder rcaBuilder = Json.createArrayBuilder();
        for (RootCauseAnalysis rca : rootCauseAnalyses) {
            rcaBuilder.add(((RootCauseAnalysisImpl) rca).toJson());
        }
        builder.add("rootCauseAnalyses", rcaBuilder);

        // Add trace recommendations
        JsonArrayBuilder traceBuilder = Json.createArrayBuilder();
        for (TraceRecommendation tr : traceRecommendations) {
            traceBuilder.add(((TraceRecommendationImpl) tr).toJson());
        }
        builder.add("traceRecommendations", traceBuilder);

        // Add recommended actions
        JsonArrayBuilder actionsBuilder = Json.createArrayBuilder();
        for (String action : recommendedActions) {
            actionsBuilder.add(action);
        }
        builder.add("recommendedActions", actionsBuilder);

        return builder.build().toString();
    }

    public void setPdfContent(byte[] pdfContent) {
        this.pdfContent = pdfContent != null ? pdfContent.clone() : null;
    }

    /**
     * Builder for creating DiagnosticReport instances
     */
    public static class Builder {
        private String reportId;
        private Instant generatedAt;
        private TriggerType triggerType;
        private Instant analysisWindowStart;
        private Instant analysisWindowEnd;
        private String serverName;
        private String libertyVersion;
        private String javaVersion;
        private String executiveSummary;
        private List<DiagnosticEvent> events = new ArrayList<>();
        private Map<String, List<DiagnosticEvent>> groupedEvents = new HashMap<>();
        private List<RootCauseAnalysis> rootCauseAnalyses = new ArrayList<>();
        private List<TraceRecommendation> traceRecommendations = new ArrayList<>();
        private List<ExternalReference> relatedAPARs = new ArrayList<>();
        private List<ExternalReference> relatedGitHubIssues = new ArrayList<>();
        private List<ExternalReference> relatedDocumentation = new ArrayList<>();
        private List<String> recommendedActions = new ArrayList<>();
        private byte[] pdfContent;

        public Builder reportId(String reportId) {
            this.reportId = reportId;
            return this;
        }

        public Builder generatedAt(Instant generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }

        public Builder triggerType(TriggerType triggerType) {
            this.triggerType = triggerType;
            return this;
        }

        public Builder analysisWindow(Instant start, Instant end) {
            this.analysisWindowStart = start;
            this.analysisWindowEnd = end;
            return this;
        }

        public Builder serverName(String serverName) {
            this.serverName = serverName;
            return this;
        }

        public Builder libertyVersion(String libertyVersion) {
            this.libertyVersion = libertyVersion;
            return this;
        }

        public Builder javaVersion(String javaVersion) {
            this.javaVersion = javaVersion;
            return this;
        }

        public Builder executiveSummary(String executiveSummary) {
            this.executiveSummary = executiveSummary;
            return this;
        }

        public Builder events(List<DiagnosticEvent> events) {
            this.events.addAll(events);
            return this;
        }

        public Builder groupedEvents(Map<String, List<DiagnosticEvent>> groupedEvents) {
            this.groupedEvents.putAll(groupedEvents);
            return this;
        }

        public Builder rootCauseAnalyses(List<RootCauseAnalysis> analyses) {
            this.rootCauseAnalyses.addAll(analyses);
            return this;
        }

        public Builder addRootCauseAnalysis(RootCauseAnalysis analysis) {
            this.rootCauseAnalyses.add(analysis);
            return this;
        }

        public Builder traceRecommendations(List<TraceRecommendation> recommendations) {
            this.traceRecommendations.addAll(recommendations);
            return this;
        }

        public Builder addTraceRecommendation(TraceRecommendation recommendation) {
            this.traceRecommendations.add(recommendation);
            return this;
        }

        public Builder relatedAPARs(List<ExternalReference> apars) {
            this.relatedAPARs.addAll(apars);
            return this;
        }

        public Builder relatedGitHubIssues(List<ExternalReference> issues) {
            this.relatedGitHubIssues.addAll(issues);
            return this;
        }

        public Builder relatedDocumentation(List<ExternalReference> docs) {
            this.relatedDocumentation.addAll(docs);
            return this;
        }

        public Builder recommendedActions(List<String> actions) {
            this.recommendedActions.addAll(actions);
            return this;
        }

        public Builder addRecommendedAction(String action) {
            this.recommendedActions.add(action);
            return this;
        }

        public Builder pdfContent(byte[] pdfContent) {
            this.pdfContent = pdfContent;
            return this;
        }

        public DiagnosticReportImpl build() {
            if (triggerType == null) {
                throw new IllegalStateException("triggerType is required");
            }
            if (analysisWindowStart == null || analysisWindowEnd == null) {
                throw new IllegalStateException("analysis window is required");
            }
            return new DiagnosticReportImpl(this);
        }
    }

    /**
     * Implementation of RootCauseAnalysis
     */
    public static class RootCauseAnalysisImpl implements RootCauseAnalysis {
        private final String signature;
        private final String rootCause;
        private final String explanation;
        private final int confidenceScore;
        private final List<String> affectedComponents;
        private final List<String> suggestedFixes;

        public RootCauseAnalysisImpl(String signature, String rootCause, String explanation,
                                     int confidenceScore, List<String> affectedComponents,
                                     List<String> suggestedFixes) {
            this.signature = signature;
            this.rootCause = rootCause;
            this.explanation = explanation;
            this.confidenceScore = confidenceScore;
            this.affectedComponents = Collections.unmodifiableList(new ArrayList<>(affectedComponents));
            this.suggestedFixes = Collections.unmodifiableList(new ArrayList<>(suggestedFixes));
        }

        @Override
        public String getSignature() {
            return signature;
        }

        @Override
        public String getRootCause() {
            return rootCause;
        }

        @Override
        public String getExplanation() {
            return explanation;
        }

        @Override
        public int getConfidenceScore() {
            return confidenceScore;
        }

        @Override
        public List<String> getAffectedComponents() {
            return affectedComponents;
        }

        @Override
        public List<String> getSuggestedFixes() {
            return suggestedFixes;
        }

        public JsonObject toJson() {
            JsonArrayBuilder componentsBuilder = Json.createArrayBuilder();
            for (String component : affectedComponents) {
                componentsBuilder.add(component);
            }

            JsonArrayBuilder fixesBuilder = Json.createArrayBuilder();
            for (String fix : suggestedFixes) {
                fixesBuilder.add(fix);
            }

            return Json.createObjectBuilder()
                    .add("signature", signature)
                    .add("rootCause", rootCause)
                    .add("explanation", explanation)
                    .add("confidenceScore", confidenceScore)
                    .add("affectedComponents", componentsBuilder)
                    .add("suggestedFixes", fixesBuilder)
                    .build();
        }
    }

    /**
     * Implementation of TraceRecommendation
     */
    public static class TraceRecommendationImpl implements TraceRecommendation {
        private final String traceString;
        private final String reason;
        private final String logVolumeEstimate;
        private final String performanceImpact;
        private final int confidenceScore;
        private final List<String> applicationMethods;

        public TraceRecommendationImpl(String traceString, String reason, String logVolumeEstimate,
                                      String performanceImpact, int confidenceScore,
                                      List<String> applicationMethods) {
            this.traceString = traceString;
            this.reason = reason;
            this.logVolumeEstimate = logVolumeEstimate;
            this.performanceImpact = performanceImpact;
            this.confidenceScore = confidenceScore;
            this.applicationMethods = Collections.unmodifiableList(new ArrayList<>(applicationMethods));
        }

        @Override
        public String getTraceString() {
            return traceString;
        }

        @Override
        public String getReason() {
            return reason;
        }

        @Override
        public String getLogVolumeEstimate() {
            return logVolumeEstimate;
        }

        @Override
        public String getPerformanceImpact() {
            return performanceImpact;
        }

        @Override
        public int getConfidenceScore() {
            return confidenceScore;
        }

        @Override
        public List<String> getApplicationMethods() {
            return applicationMethods;
        }

        public JsonObject toJson() {
            JsonArrayBuilder methodsBuilder = Json.createArrayBuilder();
            for (String method : applicationMethods) {
                methodsBuilder.add(method);
            }

            return Json.createObjectBuilder()
                    .add("traceString", traceString)
                    .add("reason", reason)
                    .add("logVolumeEstimate", logVolumeEstimate)
                    .add("performanceImpact", performanceImpact)
                    .add("confidenceScore", confidenceScore)
                    .add("applicationMethods", methodsBuilder)
                    .build();
        }
    }

    /**
     * Implementation of ExternalReference
     */
    public static class ExternalReferenceImpl implements ExternalReference {
        private final String id;
        private final String title;
        private final String url;
        private final String description;
        private final int relevanceScore;
        private final String status;

        public ExternalReferenceImpl(String id, String title, String url, String description,
                                    int relevanceScore, String status) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.description = description;
            this.relevanceScore = relevanceScore;
            this.status = status;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public int getRelevanceScore() {
            return relevanceScore;
        }

        @Override
        public String getStatus() {
            return status;
        }
    }
}

// Made with Bob
