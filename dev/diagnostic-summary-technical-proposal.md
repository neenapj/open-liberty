# Open Liberty Intelligent Diagnostic Summary Feature - Technical Proposal

## Executive Summary

This proposal introduces an optional intelligent diagnostic summary feature for Open Liberty that automatically detects and analyzes runtime issues, providing actionable insights to developers and operators. The feature generates professional PDF diagnostic reports and delivers them via email when Liberty detects repeated exceptions, startup failures, health check failures, FFDC generation, OOM/thread deadlock conditions, or repeated timeout/circuit breaker events.

**Key Enhancements**:

1. **PDF Report Generation**: Professional, formatted PDF reports with charts, visualizations, and formatted code snippets
2. **Email Delivery**: Automatic email delivery to configured recipients using SMTP/JavaMail
3. **Trace String Recommendations**: Comprehensive, confidence-ranked trace string recommendations specifically designed to help customers gather detailed diagnostic logs for L2 support when creating production tickets
4. **L2 Support Guidance**: Complete instructions for log collection, sanitization, and submission

**Primary Delivery Mechanism**: PDF reports delivered via email (Phase 1)

**Future Enhancements**: REST API, Admin Center UI, Kubernetes events, and OpenTelemetry integration (Phase 2+)

## 1. Background and Motivation

### Current State

Open Liberty currently provides:

- **FFDC (First Failure Data Capture)**: Comprehensive exception logging and stack traces
- **MicroProfile Health**: Health check endpoints (liveness, readiness, startup)
- **Monitoring**: JMX MBeans, performance metrics, and monitoring capabilities
- **Logging**: Structured logging with trace capabilities
- **MicroProfile Fault Tolerance**: Circuit breaker, timeout, and retry patterns

### Problem Statement

While Liberty provides extensive diagnostic data, users often face challenges:

1. **Information Overload**: Large volumes of logs and FFDC files make root cause analysis time-consuming
2. **Expertise Gap**: Less experienced users struggle to interpret diagnostic data and don't know which trace strings to enable
3. **Delayed Response**: Issues are often discovered after significant impact
4. **Pattern Recognition**: Repeated failures may indicate systemic issues that are hard to spot manually
5. **Support Burden**: PMRs and support cases require extensive log analysis, and customers often don't provide the right trace data initially

### Value Proposition

This feature will:

- **Reduce MTTR (Mean Time To Resolution)**: Provide immediate, actionable insights
- **Improve Developer Experience**: Help developers quickly understand and fix issues
- **Reduce Support Burden**: Enable self-service problem resolution with proper trace guidance
- **Accelerate L2 Support**: Provide customers with exact trace strings needed for their specific issue
- **Align with AIOps Trends**: Provide intelligent, context-aware diagnostics
- **Maintain Liberty's Lightweight Philosophy**: Optional, non-intrusive design

## 2. Feature Architecture

### 2.1 High-Level Design (Hybrid Approach)

The architecture supports **three trigger modes** for maximum flexibility:

1. **Immediate Triggers**: Real-time response to critical issues (seconds to minutes)
2. **Scheduled Analysis**: Proactive periodic reports (every 4 hours or daily)
3. **Manual Triggers**: On-demand analysis via REST API or Admin Center

```
┌─────────────────────────────────────────────────────────────┐
│                    Liberty Runtime                           │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   FFDC       │  │   Health     │  │  Monitoring  │     │
│  │   System     │  │   Checks     │  │   System     │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                  │                  │              │
│         └──────────────────┼──────────────────┘              │
│                            │                                 │
│                   ┌────────▼────────┐                       │
│                   │  Event Detector │                       │
│                   │   & Aggregator  │                       │
│                   └────────┬────────┘                       │
│                            │                                 │
│         ┌──────────────────┼──────────────────┐             │
│         │                  │                  │             │
│  ┌──────▼──────┐  ┌────────▼────────┐  ┌─────▼──────┐    │
│  │  Immediate  │  │   Scheduled     │  │   Manual   │    │
│  │  Trigger    │  │   Analyzer      │  │  Trigger   │    │
│  │  (Critical) │  │  (Periodic)     │  │ (On-Demand)│    │
│  └──────┬──────┘  └────────┬────────┘  └─────┬──────┘    │
│         │                  │                  │             │
│         └──────────────────┼──────────────────┘             │
│                            │                                 │
│                   ┌────────▼────────┐                       │
│                   │   Diagnostic    │                       │
│                   │    Analyzer     │                       │
│                   └────────┬────────┘                       │
│                            │                                 │
│                   ┌────────▼────────┐                       │
│                   │ Trace String    │                       │
│                   │ Recommendation  │                       │
│                   │     Engine      │                       │
│                   └────────┬────────┘                       │
│                            │                                 │
└────────────────────────────┼─────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │  PDF Report     │
                    │   Generator     │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ Email Delivery  │
                    │    Service      │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  Email Server   │
                    │ (SMTP/JavaMail) │
                    └─────────────────┘

Future Enhancements (Phase 2+):
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Admin      │  │    REST      │  │   Events     │
│   Center UI  │  │     API      │  │  (K8s, OTel) │
└──────────────┘  └──────────────┘  └──────────────┘
```

### 2.1.1 Trigger Modes Comparison

| Aspect             | Immediate Trigger            | Scheduled Analysis                   | Manual Trigger          |
| ------------------ | ---------------------------- | ------------------------------------ | ----------------------- |
| **Purpose**        | Critical issue alerts        | Proactive health monitoring          | Ad-hoc investigation    |
| **Frequency**      | As issues occur              | Every 4h/daily/weekly                | User-initiated          |
| **Response Time**  | Seconds to minutes           | Hours to days                        | Immediate               |
| **Report Type**    | Focused, urgent              | Comprehensive, trends                | Custom scope            |
| **Email Priority** | High                         | Normal                               | Normal                  |
| **Use Case**       | SSL failures, OOM, deadlocks | Trend analysis, accumulated warnings | Troubleshooting, audits |

### 2.2 Core Components

#### 2.2.1 Event Detector & Aggregator

**Location**: `com.ibm.ws.diagnostics.summary.internal`

**Responsibilities**:

- Monitor FFDC generation events
- Track health check failures (startup, liveness, readiness)
- Detect repeated exceptions (same stack trace signature)
- Monitor timeout and circuit breaker events from MicroProfile Fault Tolerance
- Detect OOM conditions and thread deadlocks
- Aggregate events within configurable time windows

**Key Classes**:

- [`DiagnosticEventDetector`](com.ibm.ws.diagnostics.summary.internal/DiagnosticEventDetector.java)
- [`EventAggregator`](com.ibm.ws.diagnostics.summary.internal/EventAggregator.java)
- [`ExceptionSignatureAnalyzer`](com.ibm.ws.diagnostics.summary.internal/ExceptionSignatureAnalyzer.java)

#### 2.2.2 Diagnostic Analyzer

**Location**: `com.ibm.ws.diagnostics.summary.analyzer`

**Responsibilities**:

- Analyze aggregated events to identify root causes
- Match exception patterns against known issues database
- Correlate multiple symptoms to single root cause
- Generate confidence scores for diagnoses
- Recommend trace strings and configuration changes

**Key Classes**:

- [`DiagnosticAnalyzer`](com.ibm.ws.diagnostics.summary.analyzer/DiagnosticAnalyzer.java)
- [`KnownIssuesMatcher`](com.ibm.ws.diagnostics.summary.analyzer/KnownIssuesMatcher.java)
- [`TraceRecommendationEngine`](com.ibm.ws.diagnostics.summary.analyzer/TraceRecommendationEngine.java)

#### 2.2.2a Scheduled Analyzer (NEW)

**Location**: `com.ibm.ws.diagnostics.summary.scheduler`

**Responsibilities**:

- Execute periodic analysis based on configured schedule (cron expression)
- Analyze accumulated events over time window (e.g., last 4 hours)
- Perform trend analysis and comparison with previous periods
- Generate comprehensive health reports
- Implement smart suppression rules to avoid duplicate reports
- Optimize resource usage during scheduled analysis

**Key Classes**:

- [`ScheduledAnalyzer`](com.ibm.ws.diagnostics.summary.scheduler/ScheduledAnalyzer.java)
- [`TrendAnalyzer`](com.ibm.ws.diagnostics.summary.scheduler/TrendAnalyzer.java)
- [`ScheduleManager`](com.ibm.ws.diagnostics.summary.scheduler/ScheduleManager.java)
- [`SuppressionRuleEngine`](com.ibm.ws.diagnostics.summary.scheduler/SuppressionRuleEngine.java)

**Scheduling Logic**:

```java
public class ScheduledAnalyzer {

    /**
     * Determines if scheduled report should be generated
     */
    public boolean shouldGenerateScheduledReport() {
        // Don't generate if immediate report sent recently
        if (immediateReportSentWithin(Duration.ofHours(2))) {
            return false;
        }

        // Don't generate if no issues found (configurable)
        if (config.isSuppressIfNoIssues() && !hasIssuesInWindow()) {
            return false;
        }

        // Don't generate during maintenance windows
        if (isInMaintenanceWindow()) {
            return false;
        }

        return true;
    }

    /**
     * Analyzes logs and events for scheduled report
     */
    public DiagnosticReport analyzeScheduledWindow() {
        Duration lookback = config.getScheduledLookback(); // e.g., 4 hours

        // Collect all events in window
        List<Event> events = eventAggregator.getEventsInWindow(lookback);

        // Analyze trends
        TrendAnalysis trends = analyzeTrends(events);

        // Compare with previous period
        Comparison comparison = compareWithPreviousPeriod(events);

        // Generate report
        return reportGenerator.generateScheduledReport(
            events, trends, comparison
        );
    }
}
```

**Report Differentiation**:

- **Immediate Alert Report**: Focused on specific critical issue, urgent tone
- **Scheduled Health Report**: Comprehensive analysis with trends, informational tone
- **Manual On-Demand Report**: User-specified scope, investigative tone

#### 2.2.3 PDF Report Generator

#### 2.2.6 Dynamic Content Fetcher (NEW)

**Location**: `com.ibm.ws.diagnostics.summary.external`

**Responsibilities**:

- Fetch real-time data from external sources
- Search and retrieve relevant APARs from IBM Support
- Query GitHub issues via GitHub API
- Search Open Liberty documentation
- Cache results to minimize external API calls
- Handle API rate limiting and failures gracefully

**Key Classes**:

- [`DynamicContentFetcher`](com.ibm.ws.diagnostics.summary.external/DynamicContentFetcher.java)
- [`APARSearchService`](com.ibm.ws.diagnostics.summary.external/APARSearchService.java)
- [`GitHubIssueSearchService`](com.ibm.ws.diagnostics.summary.external/GitHubIssueSearchService.java)
- [`DocumentationSearchService`](com.ibm.ws.diagnostics.summary.external/DocumentationSearchService.java)
- [`ContentCache`](com.ibm.ws.diagnostics.summary.external/ContentCache.java)

**Data Sources**:

1. **IBM Support APARs**:
   - URL: `https://www.ibm.com/support/pages/apar/search`
   - Search by: Exception type, error codes, keywords
   - Parse: APAR ID, title, description, fix availability

2. **GitHub Issues**:
   - API: `https://api.github.com/repos/OpenLiberty/open-liberty/issues`
   - Search by: Exception type, labels, keywords
   - Parse: Issue number, title, status, resolution, comments

3. **Open Liberty Documentation**:
   - URL: `https://openliberty.io/docs/`
   - Search by: Feature names, error codes, configuration elements
   - Parse: Documentation links, troubleshooting guides

4. **Knowledge Center**:
   - URL: `https://www.ibm.com/docs/en/was-liberty`
   - Search by: Error messages, configuration topics
   - Parse: Article links, solutions

**Implementation Example**:

```java
public class DynamicContentFetcher {

    private final APARSearchService aparService;
    private final GitHubIssueSearchService githubService;
    private final DocumentationSearchService docsService;
    private final ContentCache cache;

    /**
     * Fetch related APARs for an exception
     */
    public List<APAR> fetchRelatedAPARs(String exceptionType, String errorMessage) {
        // Check cache first
        String cacheKey = "apar:" + exceptionType;
        List<APAR> cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Build search query
        String query = buildAPARSearchQuery(exceptionType, errorMessage);

        // Search IBM Support
        List<APAR> apars = aparService.search(query);

        // Rank by relevance
        apars = rankByRelevance(apars, exceptionType, errorMessage);

        // Cache results (24 hour TTL)
        cache.put(cacheKey, apars, Duration.ofHours(24));

        return apars;
    }

    /**
     * Fetch related GitHub issues
     */
    public List<GitHubIssue> fetchRelatedGitHubIssues(String exceptionType) {
        // Check cache
        String cacheKey = "github:" + exceptionType;
        List<GitHubIssue> cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Search GitHub API
        String query = String.format(
            "repo:OpenLiberty/open-liberty %s is:issue",
            exceptionType
        );

        List<GitHubIssue> issues = githubService.search(query);

        // Filter and rank
        issues = filterRelevantIssues(issues, exceptionType);

        // Cache results (6 hour TTL)
        cache.put(cacheKey, issues, Duration.ofHours(6));

        return issues;
    }

    /**
     * Fetch relevant documentation
     */
    public List<Documentation> fetchRelevantDocs(String feature, String errorCode) {
        // Check cache
        String cacheKey = "docs:" + feature + ":" + errorCode;
        List<Documentation> cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Search documentation
        List<Documentation> docs = docsService.search(feature, errorCode);

        // Cache results (7 day TTL - docs change infrequently)
        cache.put(cacheKey, docs, Duration.ofDays(7));

        return docs;
    }
}
```

**API Integration Details**:

**GitHub API**:

```java
public class GitHubIssueSearchService {
    private static final String GITHUB_API = "https://api.github.com";
    private final String accessToken; // Optional, for higher rate limits

    public List<GitHubIssue> search(String query) {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GITHUB_API + "/search/issues?q=" +
                URLEncoder.encode(query, StandardCharsets.UTF_8)))
            .header("Accept", "application/vnd.github.v3+json")
            .header("Authorization", "token " + accessToken) // If available
            .GET()
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        // Parse JSON response
        return parseGitHubResponse(response.body());
    }
}
```

**IBM Support APAR Search**:

```java
public class APARSearchService {
    private static final String APAR_SEARCH_URL =
        "https://www.ibm.com/support/pages/apar/search";

    public List<APAR> search(String query) {
        // Use web scraping or API if available
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(APAR_SEARCH_URL + "?q=" +
                URLEncoder.encode(query, StandardCharsets.UTF_8)))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        // Parse HTML or JSON response
        return parseAPARResponse(response.body());
    }
}
```

**Configuration**:

```xml
<diagnosticSummary enabled="true" mailSessionRef="diagnosticMailSession">

    <!-- Dynamic content fetching -->
    <externalContentFetching enabled="true">
        <!-- GitHub API configuration -->
        <github
            enabled="true"
            accessToken="${env.GITHUB_TOKEN}"
            maxResults="10"
            cacheHours="6"/>

        <!-- APAR search configuration -->
        <aparSearch
            enabled="true"
            maxResults="5"
            cacheHours="24"/>

        <!-- Documentation search -->
        <documentation
            enabled="true"
            sources="openliberty.io,ibm.com/docs"
            maxResults="5"
            cacheDays="7"/>

        <!-- Fallback behavior if external services unavailable -->
        <fallback
            useStaticDatabase="true"
            continueOnError="true"/>
    </externalContentFetching>

</diagnosticSummary>
```

**Benefits**:

1. **Always Current**: Fetches latest APARs, issues, and documentation
2. **Relevant Results**: Searches based on actual exception and error context
3. **Cached for Performance**: Reduces external API calls with intelligent caching
4. **Graceful Degradation**: Falls back to static database if external services unavailable
5. **Rate Limit Aware**: Respects API rate limits and handles throttling

**Caching Strategy**:

- **APARs**: 24-hour cache (APARs don't change frequently)
- **GitHub Issues**: 6-hour cache (issues update more frequently)
- **Documentation**: 7-day cache (documentation changes infrequently)
- **Cache Storage**: In-memory with LRU eviction or persistent cache (Redis/file-based)

**Error Handling**:

```java
public List<APAR> fetchRelatedAPARs(String exceptionType, String errorMessage) {
    try {
        return aparService.search(buildQuery(exceptionType, errorMessage));
    } catch (IOException | InterruptedException e) {
        // Log error
        logger.warn("Failed to fetch APARs from external service", e);

        // Fall back to static database
        if (config.isFallbackToStaticDatabase()) {
            return staticDatabase.searchAPARs(exceptionType);
        }

        return Collections.emptyList();
    }
}
```

**Privacy and Security**:

- **No Sensitive Data**: Never send customer data, stack traces, or configuration to external services
- **Search Terms Only**: Only send exception types, error codes, and generic keywords
- **Configurable**: Can be disabled entirely for air-gapped environments
- **Proxy Support**: Supports corporate proxies for external access

**Location**: `com.ibm.ws.diagnostics.summary.report`

**Responsibilities**:

- Generate professional PDF reports from diagnostic analysis
- Include formatted trace recommendations with syntax highlighting
- Embed charts and visualizations (exception frequency, timeline)
- Format code snippets and configuration examples
- Include clickable links to documentation and APARs
- Support multi-page reports with table of contents

**Key Classes**:

- [`PDFReportGenerator`](com.ibm.ws.diagnostics.summary.report/PDFReportGenerator.java)
- [`ReportFormatter`](com.ibm.ws.diagnostics.summary.report/ReportFormatter.java)
- [`TraceInstructionFormatter`](com.ibm.ws.diagnostics.summary.report/TraceInstructionFormatter.java)

**Dependencies**:

- Apache PDFBox or iText for PDF generation
- Chart generation library (JFreeChart or similar)

**Report Structure**:

1. **Executive Summary**: High-level issue overview
2. **Issue Details**: Exception details, frequency, timeline
3. **Root Cause Analysis**: Confidence-ranked diagnoses
4. **Trace Recommendations**: Step-by-step trace enablement instructions
5. **L2 Support Package**: File collection checklist
6. **Related Information**: APARs, documentation links
7. **Appendices**: Full stack traces, configuration snippets

#### 2.2.4 Email Delivery Service

**Location**: `com.ibm.ws.diagnostics.summary.notification`

**Responsibilities**:

- Send PDF reports via email using JavaMail API
- Support SMTP and SMTPS protocols
- Handle authentication (plain, TLS, OAuth2)
- Manage email templates and formatting
- Support multiple recipients (to, cc, bcc)
- Retry logic for failed deliveries
- Track delivery status

**Key Classes**:

- [`EmailDeliveryService`](com.ibm.ws.diagnostics.summary.notification/EmailDeliveryService.java)
- [`SMTPConfigurationManager`](com.ibm.ws.diagnostics.summary.notification/SMTPConfigurationManager.java)
- [`EmailTemplateEngine`](com.ibm.ws.diagnostics.summary.notification/EmailTemplateEngine.java)

**Dependencies**:

- Jakarta Mail API (javax.mail / jakarta.mail)
- Liberty's existing mail session configuration support

**Email Structure**:

- **Subject**: "Liberty Diagnostic Report: [Issue Type] - [Timestamp]"
- **Body**: HTML formatted summary with key findings
- **Attachment**: PDF report (diagnostic-report-[timestamp].pdf)

#### 2.2.5 Trace String Recommendation Engine

**Location**: `com.ibm.ws.diagnostics.summary.trace`

**Responsibilities**:

- Map exception patterns to relevant Liberty trace packages
- Rank trace strings by confidence and diagnostic value
- Estimate log volume impact for each trace string
- Provide guidance on trace application and log collection
- Generate L2 support package instructions

**Key Classes**:

- [`TraceRecommendationEngine`](com.ibm.ws.diagnostics.summary.trace/TraceRecommendationEngine.java)
- [`TracePackageMapper`](com.ibm.ws.diagnostics.summary.trace/TracePackageMapper.java)
- [`LogVolumeEstimator`](com.ibm.ws.diagnostics.summary.trace/LogVolumeEstimator.java)

### 2.3 Configuration (Hybrid Approach)

The feature is configured via `server.xml` with support for **immediate triggers**, **scheduled analysis**, and **manual triggers**:

```xml
<server>
    <!-- Enable the diagnostic summary feature -->
    <featureManager>
        <feature>diagnosticSummary-1.0</feature>
        <feature>mail-2.1</feature>
    </featureManager>

    <!-- Configure mail session for report delivery -->
    <mailSession id="diagnosticMailSession"
                 host="smtp.example.com"
                 port="587"
                 user="liberty-diagnostics@example.com"
                 password="{xor}Lz4sLCgwLTs="
                 from="liberty-diagnostics@example.com">
        <property name="mail.smtp.auth" value="true"/>
        <property name="mail.smtp.starttls.enable" value="true"/>
    </mailSession>

    <!-- Configure diagnostic summary feature -->
    <diagnosticSummary
        enabled="true"
        mailSessionRef="diagnosticMailSession">

        <!-- Email recipients -->
        <emailRecipients>
            <recipient email="ops-team@example.com" type="to"/>
            <recipient email="dev-team@example.com" type="cc"/>
        </emailRecipients>

        <!-- IMMEDIATE TRIGGERS (for critical issues) -->
        <immediateTriggers>
            <exceptionTrigger
                threshold="5"
                timeWindow="10m"
                severity="CRITICAL,HIGH"
                enabled="true"/>

            <healthCheckTrigger
                consecutiveFailures="3"
                checkTypes="liveness,readiness"
                enabled="true"/>

            <ffdcTrigger
                threshold="10"
                timeWindow="5m"
                severity="ERROR,SEVERE"
                enabled="true"/>
        </immediateTriggers>

        <!-- SCHEDULED PROACTIVE ANALYSIS (NEW) -->
        <scheduledAnalysis
            enabled="true"
            schedule="0 */4 * * *"
            timezone="America/New_York"
            minIssuesForReport="1"
            includeHealthSummary="true">

            <!-- What to analyze in scheduled reports -->
            <analysisScope>
                <exceptions minOccurrences="2" severity="WARNING,ERROR,SEVERE"/>
                <healthChecks includeHistory="true" lookbackHours="4"/>
                <performance includeMetrics="true"/>
                <trends enabled="true"/>
            </analysisScope>

            <!-- Suppress scheduled report if immediate report sent recently -->
            <suppressionRules>
                <suppressIfImmediateReportWithin>2h</suppressIfImmediateReportWithin>
                <suppressIfNoIssuesFound>true</suppressIfNoIssuesFound>
            </suppressionRules>
        </scheduledAnalysis>

        <!-- MANUAL TRIGGER (via REST API or Admin Center) -->
        <manualTrigger enabled="true"/>

        <!-- Report generation options -->
        <reportOptions
            includeFullStackTraces="true"
            includeConfiguration="true"
            includeRecentLogs="true"
            maxRecentLogLines="500"
            generatePDF="true"
            pdfIncludeCharts="true"/>

        <!-- Trace recommendation options -->
        <traceRecommendations
            includeHighConfidence="true"
            includeMediumConfidence="true"
            includeLowConfidence="false"
            includeLogVolumeEstimates="true"
            includeL2SupportGuidance="true"/>
    </diagnosticSummary>
</server>
```

**Configuration Options**:

**Core Settings**:

- **enabled**: Enable/disable the feature (default: true)
- **mailSessionRef**: Reference to configured mail session for email delivery
- **emailRecipients**: List of email recipients (to, cc, bcc)

**Immediate Triggers**:

- **exceptionTrigger**: Configure exception-based report generation for critical issues
- **healthCheckTrigger**: Configure health check failure triggers
- **ffdcTrigger**: Configure FFDC-based triggers

**Scheduled Analysis** (NEW):

- **enabled**: Enable/disable scheduled analysis (default: false)
- **schedule**: Cron expression for scheduling (e.g., "0 _/4 _ \* \*" = every 4 hours)
- **timezone**: Timezone for schedule (default: server timezone)
- **minIssuesForReport**: Minimum issues to generate report (default: 1)
- **analysisScope**: What to analyze (exceptions, health checks, performance, trends)
- **suppressionRules**: Rules to avoid duplicate reports

**Report Options**:

- **reportOptions**: Control report content and format
- **traceRecommendations**: Control trace recommendation inclusion

### 2.3.1 Schedule Examples

**Every 4 Hours**:

```xml
<scheduledAnalysis enabled="true" schedule="0 */4 * * *"/>
```

**Daily at 2 AM**:

```xml
<scheduledAnalysis enabled="true" schedule="0 2 * * *"/>
```

**Business Hours Only (Mon-Fri, 9 AM - 5 PM, every hour)**:

```xml
<scheduledAnalysis enabled="true" schedule="0 9-17 * * 1-5"/>
```

**Weekly Summary (Sunday at midnight)**:

```xml
<scheduledAnalysis enabled="true" schedule="0 0 * * 0"/>
```

### 2.3.2 Simplified Configurations

**Immediate Triggers Only** (current behavior):

```xml
<diagnosticSummary
    enabled="true"
    mailSessionRef="diagnosticMailSession"
    recipients="ops-team@example.com,dev-team@example.com"/>
```

**Scheduled Reports Only** (proactive monitoring):

```xml
<diagnosticSummary
    enabled="true"
    mailSessionRef="diagnosticMailSession"
    recipients="ops-team@example.com">
    <scheduledAnalysis enabled="true" schedule="0 */4 * * *"/>
    <immediateTriggers>
        <exceptionTrigger enabled="false"/>
        <healthCheckTrigger enabled="false"/>
        <ffdcTrigger enabled="false"/>
    </immediateTriggers>
</diagnosticSummary>
```

**Hybrid Approach** (RECOMMENDED):

```xml
<diagnosticSummary
    enabled="true"
    mailSessionRef="diagnosticMailSession"
    recipients="ops-team@example.com">
    <!-- Immediate alerts for critical issues -->
    <immediateTriggers>
        <exceptionTrigger threshold="5" timeWindow="10m" severity="CRITICAL,HIGH"/>
    </immediateTriggers>
    <!-- Scheduled health reports every 4 hours -->
    <scheduledAnalysis enabled="true" schedule="0 */4 * * *">
        <suppressionRules>
            <suppressIfImmediateReportWithin>2h</suppressIfImmediateReportWithin>
        </suppressionRules>
    </scheduledAnalysis>
</diagnosticSummary>
```

## 3. Diagnostic Report Format with Enhanced Trace Recommendations

### 3.1 Complete Report Structure

```json
{
  "reportId": "diag-2026-05-18-123456-001",
  "timestamp": "2026-05-18T06:30:00.000Z",
  "serverName": "defaultServer",
  "serverVersion": "26.0.0.5",
  "triggerType": "REPEATED_EXCEPTION",
  "severity": "HIGH",

  "summary": {
    "title": "SSL Handshake Failures Detected",
    "description": "37 SSL handshake exceptions in 10 minutes affecting inventory-service",
    "impact": "Service unavailable for external clients",
    "confidence": 0.95
  },

  "detectedIssue": {
    "category": "SECURITY",
    "type": "SSL_HANDSHAKE_FAILURE",
    "exceptionType": "javax.net.ssl.SSLHandshakeException",
    "frequency": 37,
    "timeWindow": "10m",
    "affectedComponents": ["inventory-service"],
    "firstOccurrence": "2026-05-18T06:20:00.000Z",
    "lastOccurrence": "2026-05-18T06:30:00.000Z"
  },

  "rootCauseAnalysis": {
    "possibleCauses": [
      {
        "cause": "Expired certificate",
        "confidence": 0.85,
        "evidence": ["Certificate expiration date: 2026-05-15", "Current date: 2026-05-18", "Stack trace contains: ValidatorException: PKIX path validation failed"]
      },
      {
        "cause": "Truststore missing signer certificate",
        "confidence": 0.7,
        "evidence": ["PKIX path building failed in stack trace", "No matching certificate found in truststore"]
      },
      {
        "cause": "TLS protocol mismatch",
        "confidence": 0.4,
        "evidence": ["Client requesting TLSv1.2", "Server configured for TLSv1.3 only"]
      }
    ],
    "recommendedCause": "Expired certificate"
  },

  "recommendations": {
    "immediate": [
      {
        "action": "Verify certificate expiration",
        "command": "keytool -list -v -keystore ${server.config.dir}/resources/security/key.p12 -storepass <password>",
        "expectedResult": "Check 'Valid from' and 'Valid until' dates in output",
        "priority": "HIGH"
      },
      {
        "action": "Check truststore configuration",
        "configLocation": "server.xml: <ssl id=\"defaultSSLConfig\">",
        "verificationSteps": ["Verify keyStoreRef points to correct keystore", "Verify trustStoreRef points to correct truststore", "Check that truststore contains required CA certificates"],
        "priority": "HIGH"
      },
      {
        "action": "Review recent certificate changes",
        "description": "Check if certificates were recently updated or if any expired",
        "priority": "HIGH"
      }
    ],

    "diagnostic": [
      {
        "action": "Enable comprehensive SSL/TLS trace for L2 support",
        "traceSpecification": "com.ibm.ws.ssl.*=all:com.ibm.ws.security.*=all:com.ibm.ws.security.registry.*=all:com.ibm.ws.security.authentication.*=all",
        "howToApply": {
          "method1": {
            "name": "Via bootstrap.properties (requires restart)",
            "steps": ["1. Edit ${server.config.dir}/bootstrap.properties", "2. Add: com.ibm.ws.logging.trace.specification=com.ibm.ws.ssl.*=all:com.ibm.ws.security.*=all:com.ibm.ws.security.registry.*=all:com.ibm.ws.security.authentication.*=all", "3. Restart server"]
          },
          "method2": {
            "name": "Via server.xml (dynamic, no restart)",
            "steps": ["1. Edit ${server.config.dir}/server.xml", "2. Add: <logging traceSpecification=\"com.ibm.ws.ssl.*=all:com.ibm.ws.security.*=all:com.ibm.ws.security.registry.*=all:com.ibm.ws.security.authentication.*=all\"/>", "3. Save file (trace activates automatically)"]
          }
        },
        "expectedOutput": "Detailed SSL handshake negotiation, certificate chain validation, truststore operations, and security context information",
        "priority": "HIGH",
        "confidenceLevel": 0.95,
        "estimatedLogVolume": "HIGH - 10-50 MB per hour under moderate load",
        "diagnosticValue": "Essential for diagnosing certificate validation, SSL configuration, and handshake failures"
      },
      {
        "action": "Enable network and channel trace for connection analysis",
        "traceSpecification": "com.ibm.ws.channel.ssl.*=all:com.ibm.ws.tcpchannel.*=all:com.ibm.io.async.*=all",
        "howToApply": {
          "method1": {
            "name": "Via bootstrap.properties",
            "steps": ["1. Edit ${server.config.dir}/bootstrap.properties", "2. Add: com.ibm.ws.logging.trace.specification=com.ibm.ws.channel.ssl.*=all:com.ibm.ws.tcpchannel.*=all:com.ibm.io.async.*=all", "3. Restart server"]
          }
        },
        "expectedOutput": "TCP connection lifecycle, SSL channel operations, network I/O patterns, and connection pool behavior",
        "priority": "MEDIUM",
        "confidenceLevel": 0.75,
        "estimatedLogVolume": "MEDIUM - 5-20 MB per hour under moderate load",
        "diagnosticValue": "Useful for diagnosing connection-level issues, timeouts, and network problems"
      },
      {
        "action": "Enable JVM-level SSL debug (use sparingly)",
        "traceSpecification": "-Djavax.net.debug=all",
        "howToApply": {
          "method1": {
            "name": "Via jvm.options (requires restart)",
            "steps": ["1. Edit ${server.config.dir}/jvm.options", "2. Add line: -Djavax.net.debug=all", "3. Restart server"]
          }
        },
        "expectedOutput": "JVM SSL/TLS handshake details, certificate validation at JVM level, cipher suite negotiation, and JSSE internals",
        "priority": "LOW",
        "confidenceLevel": 0.85,
        "estimatedLogVolume": "VERY HIGH - 50-200 MB per hour under moderate load",
        "diagnosticValue": "Provides deepest level of SSL diagnostics but generates massive log volume",
        "warning": "⚠️ This generates extensive output and may impact performance. Use only for short diagnostic periods (5-10 minutes) and only if Liberty traces don't provide sufficient information."
      },
      {
        "action": "Enable PKIX certificate path validation trace",
        "traceSpecification": "sun.security.validator.*=all:sun.security.provider.certpath.*=all",
        "howToApply": {
          "method1": {
            "name": "Via jvm.options",
            "steps": ["1. Edit ${server.config.dir}/jvm.options", "2. Add: -Djava.util.logging.config.file=${server.config.dir}/logging.properties", "3. Create logging.properties with: sun.security.validator.level=ALL", "4. Restart server"]
          }
        },
        "expectedOutput": "Certificate path building, trust anchor validation, and certificate chain verification details",
        "priority": "MEDIUM",
        "confidenceLevel": 0.8,
        "estimatedLogVolume": "MEDIUM - 5-15 MB per hour under moderate load",
        "diagnosticValue": "Specifically useful for certificate trust chain issues"
      }
    ],

    "traceGuidance": {
      "recommendedApproach": "Progressive trace enablement strategy",
      "strategy": ["1. Start with HIGH confidence Liberty traces (com.ibm.ws.ssl.*=all)", "2. Reproduce the issue and capture logs for 5-10 minutes", "3. If issue root cause is not clear, add MEDIUM confidence traces", "4. Only enable JVM-level traces (javax.net.debug) as last resort", "5. Disable traces after capturing sufficient data to reduce log volume"],

      "captureInstructions": {
        "preparation": ["1. Note current server time and timezone", "2. Clear old logs if disk space is limited: rm ${server.output.dir}/logs/trace*.log", "3. Enable recommended trace strings (start with HIGH confidence)", "4. Verify trace is active: check for trace entries in logs/trace.log"],
        "reproduction": ["1. Trigger the issue (e.g., attempt SSL connections that fail)", "2. Capture logs for 5-10 minutes OR until issue occurs 3-5 times", "3. Note exact timestamps when issue occurred", "4. If possible, capture network traffic (tcpdump/wireshark) simultaneously"],
        "collection": ["1. Disable traces to stop log growth", "2. Collect all required files (see filesForL2Support below)", "3. Compress logs: tar -czf diagnostic-logs-$(date +%Y%m%d-%H%M%S).tar.gz logs/", "4. Verify compressed file size is reasonable for upload"]
      },

      "filesForL2Support": [
        {
          "file": "logs/messages.log",
          "description": "Last 24 hours of messages",
          "required": true,
          "sanitize": false
        },
        {
          "file": "logs/trace.log",
          "description": "Complete trace log with recommended traces enabled",
          "required": true,
          "sanitize": false
        },
        {
          "file": "logs/ffdc/*.log",
          "description": "All FFDC files related to SSL/security",
          "required": true,
          "sanitize": false
        },
        {
          "file": "server.xml",
          "description": "Server configuration",
          "required": true,
          "sanitize": true,
          "sanitizeFields": ["password", "keyPassword", "keystorePassword"]
        },
        {
          "file": "bootstrap.properties",
          "description": "Bootstrap configuration",
          "required": true,
          "sanitize": true,
          "sanitizeFields": ["password"]
        },
        {
          "file": "jvm.options",
          "description": "JVM options",
          "required": true,
          "sanitize": false
        },
        {
          "file": "${server.config.dir}/resources/security/*.p12",
          "description": "Keystore and truststore files (DO NOT include private keys)",
          "required": false,
          "sanitize": false,
          "note": "Only provide if requested by L2. Use keytool -list output instead."
        }
      ],

      "sanitizationGuidance": {
        "warning": "⚠️ CRITICAL: Remove or mask sensitive data before sharing with support",
        "sensitiveData": ["Passwords (keystore, truststore, database, etc.)", "API keys and tokens", "Personal information (names, emails, addresses)", "Internal hostnames and IP addresses (if security concern)", "Database connection strings with credentials", "OAuth client secrets", "Private keys (NEVER share)"],
        "sanitizationCommands": ["# Replace passwords in server.xml", "sed -i 's/password=\"[^\"]*\"/password=\"***REDACTED***\"/g' server.xml", "", "# Replace passwords in bootstrap.properties", "sed -i 's/password=.*/password=***REDACTED***/g' bootstrap.properties"]
      },

      "estimatedTotalLogSize": {
        "withHighConfidenceTraces": "15-60 MB for 10 minutes",
        "withAllRecommendedTraces": "50-150 MB for 10 minutes",
        "withJVMDebug": "100-500 MB for 10 minutes"
      },

      "performanceImpact": {
        "highConfidenceTraces": "Minimal - <5% CPU overhead, <100MB memory",
        "allRecommendedTraces": "Low - 5-10% CPU overhead, <200MB memory",
        "jvmDebug": "Moderate - 10-20% CPU overhead, <500MB memory, may affect response times"
      }
    },

    "preventive": [
      {
        "action": "Implement certificate expiration monitoring",
        "description": "Configure alerts for certificates expiring within 30 days",
        "implementation": "Use MicroProfile Health check or external monitoring",
        "priority": "MEDIUM"
      },
      {
        "action": "Automate certificate renewal",
        "description": "Implement automated certificate renewal process",
        "tools": ["Let's Encrypt", "cert-manager (Kubernetes)", "Internal PKI automation"],
        "priority": "MEDIUM"
      },
      {
        "action": "Document certificate management procedures",
        "description": "Create runbook for certificate updates and troubleshooting",
        "priority": "LOW"
      }
    ]
  },

  "relatedInformation": {
    "apars": [
      {
        "id": "PH12345",
        "title": "SSL handshake failures after JDK update",
        "url": "https://www.ibm.com/support/pages/apar/PH12345",
        "relevance": 0.85
      }
    ],
    "documentation": [
      {
        "title": "Configuring SSL in Liberty",
        "url": "https://openliberty.io/docs/latest/reference/config/ssl.html",
        "section": "SSL configuration reference"
      },
      {
        "title": "Troubleshooting SSL/TLS",
        "url": "https://openliberty.io/docs/latest/troubleshooting.html#ssl",
        "section": "SSL troubleshooting guide"
      },
      {
        "title": "Liberty Trace Specification",
        "url": "https://openliberty.io/docs/latest/log-trace-configuration.html",
        "section": "Configuring trace"
      }
    ],
    "githubIssues": [
      {
        "number": 12345,
        "title": "SSL handshake failure with expired certificates",
        "url": "https://github.com/OpenLiberty/open-liberty/issues/12345",
        "status": "closed",
        "resolution": "Certificate renewal required"
      }
    ],
    "knowledgeBase": [
      {
        "id": "KB001234",
        "title": "How to diagnose SSL handshake failures in Liberty",
        "url": "https://www.ibm.com/support/pages/node/001234"
      }
    ]
  },

  "context": {
    "configuration": {
      "sslConfig": {
        "id": "defaultSSLConfig",
        "keyStoreRef": "defaultKeyStore",
        "trustStoreRef": "defaultTrustStore",
        "sslProtocol": "TLSv1.2",
        "enabledCiphers": "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,..."
      }
    },
    "environment": {
      "javaVersion": "17.0.5",
      "javaVendor": "IBM Semeru Runtime",
      "osName": "Linux",
      "osVersion": "5.15.0",
      "libertyVersion": "26.0.0.5",
      "libertyEdition": "Open Liberty"
    },
    "recentLogs": [
      {
        "timestamp": "2026-05-18T06:29:55.000Z",
        "level": "ERROR",
        "logger": "com.ibm.ws.ssl.core.WSX509TrustManager",
        "message": "CWPKI0022E: SSL HANDSHAKE FAILURE: A signer with SubjectDN CN=example.com, O=Example Corp was sent from the target host..."
      },
      {
        "timestamp": "2026-05-18T06:29:55.100Z",
        "level": "ERROR",
        "logger": "com.ibm.ws.channel.ssl.internal.SSLChannelProvider",
        "message": "CWWKO0801E: Unable to initialize SSL connection. The exception is javax.net.ssl.SSLHandshakeException..."
      }
    ]
  },

  "attachments": {
    "ffdcFiles": ["logs/ffdc/ffdc_26.05.18_06.30.00.0.log", "logs/ffdc/ffdc_26.05.18_06.29.55.0.log"],
    "relevantLogs": ["logs/messages.log", "logs/trace.log"],
    "configFiles": ["server.xml", "bootstrap.properties", "jvm.options"]
  }
}
```

### 3.2 Trace Recommendation Database

The system maintains a database mapping exception patterns to trace specifications:

```json
{
  "traceRecommendations": [
    {
      "pattern": "javax.net.ssl.SSLHandshakeException",
      "category": "SSL/TLS",
      "traces": [
        {
          "specification": "com.ibm.ws.ssl.*=all:com.ibm.ws.security.*=all",
          "confidence": 0.95,
          "priority": "HIGH",
          "estimatedVolume": "HIGH",
          "diagnosticValue": "Essential for SSL issues"
        },
        {
          "specification": "com.ibm.ws.channel.ssl.*=all",
          "confidence": 0.75,
          "priority": "MEDIUM",
          "estimatedVolume": "MEDIUM",
          "diagnosticValue": "Connection-level diagnostics"
        }
      ]
    },
    {
      "pattern": "java.sql.SQLException.*timeout",
      "category": "Database",
      "traces": [
        {
          "specification": "com.ibm.ws.jdbc.*=all:com.ibm.ws.rsadapter.*=all",
          "confidence": 0.9,
          "priority": "HIGH",
          "estimatedVolume": "MEDIUM",
          "diagnosticValue": "JDBC connection and statement execution"
        },
        {
          "specification": "com.ibm.ws.connectionpool.*=all",
          "confidence": 0.85,
          "priority": "HIGH",
          "estimatedVolume": "LOW",
          "diagnosticValue": "Connection pool behavior"
        }
      ]
    },
    {
      "pattern": "javax.ejb.EJBTransactionRolledbackException",
      "category": "Transaction",
      "traces": [
        {
          "specification": "com.ibm.ws.transaction.*=all:com.ibm.tx.*=all",
          "confidence": 0.95,
          "priority": "HIGH",
          "estimatedVolume": "MEDIUM",
          "diagnosticValue": "Transaction lifecycle and coordination"
        },
        {
          "specification": "com.ibm.ws.ejbcontainer.*=all",
          "confidence": 0.8,
          "priority": "MEDIUM",
          "estimatedVolume": "HIGH",
          "diagnosticValue": "EJB container operations"
        }
      ]
    },
    {
      "pattern": "java.lang.OutOfMemoryError",
      "category": "Memory",
      "traces": [
        {
          "specification": "com.ibm.ws.classloading.*=all",
          "confidence": 0.7,
          "priority": "MEDIUM",
          "estimatedVolume": "HIGH",
          "diagnosticValue": "Classloading and memory usage"
        }
      ],
      "additionalDiagnostics": [
        {
          "action": "Enable heap dumps on OOM",
          "jvmOption": "-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${server.output.dir}/heapdumps"
        },
        {
          "action": "Enable GC logging",
          "jvmOption": "-Xlog:gc*:file=${server.output.dir}/logs/gc.log:time,level,tags"
        }
      ]
    }
  ]
}
```

## 4. Trigger Conditions

### 4.1 Exception-Based Triggers

#### Repeated Exceptions

**Trigger**: Same exception type and stack trace signature occurs N times within time window T

**Configuration**:

```xml
<diagnosticSummary>
    <exceptionTrigger
        threshold="5"
        timeWindow="10m"
        includeStackTrace="true"/>
</diagnosticSummary>
```

**Detection Logic**:

- Generate stack trace signature (hash of exception type + top N frames)
- Track occurrences in sliding time window
- Trigger when threshold exceeded

#### FFDC Generation

**Trigger**: FFDC file generated for specific exception types

**Configuration**:

```xml
<diagnosticSummary>
    <ffdcTrigger
        enabled="true"
        severity="ERROR,SEVERE"
        autoAnalyze="true"/>
</diagnosticSummary>
```

### 4.2 Startup and Health Check Triggers

#### Startup Failures

**Trigger**: Application or server fails to start

**Detection Points**:

- Application state transitions to FAILED
- Server startup timeout
- Critical component initialization failures

#### Health Check Failures

**Trigger**: Health check endpoints report DOWN status

**Configuration**:

```xml
<diagnosticSummary>
    <healthCheckTrigger
        enabled="true"
        consecutiveFailures="3"
        checkTypes="startup,liveness,readiness"/>
</diagnosticSummary>
```

### 4.3 Resource and Performance Triggers

#### OOM Conditions

**Trigger**: OutOfMemoryError detected or heap usage exceeds threshold

**Detection**:

- Monitor for `java.lang.OutOfMemoryError`
- Track heap usage via JMX
- Analyze GC patterns

#### Thread Deadlock

**Trigger**: Thread deadlock detected via ThreadMXBean

**Detection**:

- Periodic deadlock detection (configurable interval)
- Immediate analysis when detected

#### Timeout Events

**Trigger**: Repeated timeout events from MicroProfile Fault Tolerance

**Configuration**:

```xml
<diagnosticSummary>
    <timeoutTrigger
        threshold="10"
        timeWindow="5m"
        includeCircuitBreaker="true"/>
</diagnosticSummary>
```

## 5. Implementation Plan

### 5.1 Phase 1: Core Infrastructure and PDF Generation (Milestone 1)

**Duration**: 8-10 weeks

**Deliverables**:

- Event detection and aggregation framework
- Basic exception pattern matching
- PDF report generation with professional formatting
- Report data model and JSON intermediate format
- Configuration schema and parsing
- File-based report storage

**Key Classes**:

- `DiagnosticEventDetector`
- `EventAggregator`
- `ExceptionSignatureAnalyzer`
- `PDFReportGenerator`
- `ReportFormatter`

**Dependencies**:

- Apache PDFBox or iText library integration
- Chart generation library (JFreeChart)

### 5.2 Phase 2: Email Delivery Service (Milestone 2)

**Duration**: 4-6 weeks

**Deliverables**:

- Email delivery service using JavaMail API
- SMTP/SMTPS configuration support
- Email template engine
- Retry logic and delivery tracking
- Integration with Liberty mail session configuration

**Key Classes**:

- `EmailDeliveryService`
- `SMTPConfigurationManager`
- `EmailTemplateEngine`
- `DeliveryStatusTracker`

**Dependencies**:

- Jakarta Mail API integration
- Liberty mail-2.1 feature dependency

### 5.3 Phase 3: Trace Recommendation Engine (Milestone 3)

**Duration**: 8-10 weeks

**Deliverables**:

- Trace string recommendation engine
- Exception pattern to trace mapping database
- Confidence scoring for trace recommendations
- Log volume estimation
- L2 support package generation guidance
- Integration with PDF report generation

**Key Classes**:

- `TraceRecommendationEngine`
- `TracePackageMapper`
- `LogVolumeEstimator`
- `L2SupportPackageGenerator`
- `TraceInstructionFormatter`

### 5.4 Phase 4: Analysis Engine (Milestone 4)

**Duration**: 8-10 weeks

**Deliverables**:

- Root cause analysis engine
- Known issues database and matching
- Confidence scoring system
- APAR/documentation linking
- Enhanced PDF report with root cause analysis

**Key Classes**:

- `DiagnosticAnalyzer`
- `KnownIssuesMatcher`
- `ConfidenceScorer`
- `APARLinker`

### 5.5 Phase 5: Advanced Triggers and Analysis (Milestone 5)

**Duration**: 8-10 weeks

**Deliverables**:

- Health check failure analysis
- Timeout/circuit breaker pattern detection
- OOM and deadlock analysis
- Historical trend analysis
- Enhanced visualizations in PDF reports

### 5.6 Phase 6: Future Enhancements (Milestone 6)

**Duration**: 8-10 weeks (Optional - Post-MVP)

**Deliverables**:

- REST API endpoints for programmatic access
- Admin Center UI components for interactive viewing
- Kubernetes event generation
- OpenTelemetry integration
- Webhook notifications
- Real-time dashboard

**Key Components**:

- REST API bundle
- Admin Center React components
- Event emitters
- WebSocket support for real-time updates

### 5.7 Phase 7: Testing and Documentation (Milestone 7)

**Duration**: 6-8 weeks

**Deliverables**:

- Comprehensive FAT tests for all components
  - Event detection and aggregation tests
  - PDF generation tests with various report types
  - Email delivery tests (mock SMTP server)
  - Trace recommendation accuracy tests
  - End-to-end integration tests
- Performance testing
  - Memory overhead measurement
  - CPU impact analysis
  - PDF generation performance
  - Email delivery throughput
- User documentation
  - Feature overview and benefits
  - Configuration guide with examples
  - Trace recommendation interpretation guide
  - L2 support package preparation guide
  - Troubleshooting guide
- Sample configurations for common scenarios
- L2 support team training materials
- Security review and hardening
  - Email credential protection
  - PDF content sanitization
  - Rate limiting for email delivery

## 6. Success Metrics

### 6.1 Adoption Metrics

- Feature enablement rate
- Report generation frequency
- Trace recommendation usage rate
- L2 support package completeness improvement

### 6.2 Effectiveness Metrics

- Time to resolution improvement
- Support case reduction
- First-time-right diagnostic data submission rate
- User satisfaction scores
- False positive rate for trace recommendations

### 6.3 Performance Metrics

- Memory overhead
- CPU overhead
- Report generation time
- Trace recommendation accuracy

## 7. Conclusion

The Intelligent Diagnostic Summary feature with PDF report generation and email delivery represents a significant advancement in Open Liberty's observability and troubleshooting capabilities. By providing:

1. **Automated Issue Detection**: Proactive identification of runtime problems
2. **Professional PDF Reports**: Well-formatted, comprehensive diagnostic reports with charts and visualizations
3. **Automatic Email Delivery**: Immediate notification to operations and development teams
4. **Intelligent Root Cause Analysis**: Context-aware diagnosis with confidence scoring
5. **Actionable Trace Recommendations**: Confidence-ranked, volume-estimated trace strings specifically designed for L2 support
6. **Comprehensive L2 Support Guidance**: Complete instructions for log collection, sanitization, and submission
7. **Progressive Diagnostic Strategy**: Start with high-confidence traces and escalate as needed

This feature will dramatically reduce the time and expertise required to diagnose and resolve production issues, while ensuring that when L2 support is needed, customers provide exactly the right diagnostic data on the first attempt.

### Implementation Strategy

The phased implementation approach prioritizes the core value proposition:

**Phase 1 (MVP)**: PDF generation and email delivery with basic diagnostics

- Provides immediate value to customers
- Establishes the foundation for future enhancements
- Minimal dependencies and complexity

**Phase 2-5**: Enhanced analysis, trace recommendations, and advanced triggers

- Builds on the solid foundation
- Adds intelligence and sophistication
- Improves diagnostic accuracy

**Phase 6 (Future)**: REST API, UI, and integration capabilities

- Extends reach and accessibility
- Enables programmatic access
- Supports advanced use cases

This approach ensures that customers receive tangible benefits early while allowing for continuous improvement and feature expansion based on real-world feedback.

### Key Differentiators

1. **Email-First Approach**: Unlike traditional monitoring solutions that require users to check dashboards, this feature proactively delivers diagnostic reports to stakeholders
2. **L2 Support Focus**: Specifically designed to accelerate support case resolution by providing exactly the diagnostic data L2 teams need
3. **Trace Recommendation Intelligence**: Goes beyond simple error reporting to provide actionable guidance on gathering additional diagnostic data
4. **Professional Presentation**: PDF reports provide a polished, shareable format suitable for both technical and management audiences
5. **Liberty-Native**: Deep integration with Liberty's diagnostic infrastructure (FFDC, health checks, monitoring) for accurate and comprehensive analysis

## Appendix A: Additional Trace Recommendation Examples

## Appendix B: PDF Report Structure and Email Template

### B.1 PDF Report Layout

The PDF report follows a professional, multi-page structure:

#### Page 1: Executive Summary

- **Header**: Liberty logo, report title, timestamp
- **Alert Box**: Issue severity indicator (Critical/High/Medium/Low)
- **Summary Section**:
  - Issue type and description
  - First occurrence and frequency
  - Affected server and application
  - Recommended priority level
- **Quick Actions**: Top 3 immediate actions to take
- **Footer**: Page number, report ID

#### Page 2-3: Issue Details

- **Timeline Chart**: Visual representation of issue occurrences over time
- **Exception Details**:
  - Exception type and message
  - Stack trace (formatted with syntax highlighting)
  - Frequency analysis
- **Affected Components**: List of Liberty features/bundles involved
- **Environment Context**:
  - Java version and vendor
  - Liberty version and edition
  - OS information
  - Recent configuration changes

#### Page 4-5: Root Cause Analysis

- **Confidence-Ranked Diagnoses**:
  - Each diagnosis with confidence score (0-100%)
  - Supporting evidence from logs and configuration
  - Visual confidence meter
- **Related Issues**:
  - Links to APARs
  - GitHub issues
  - Knowledge base articles

#### Page 6-8: Trace Recommendations

- **High Confidence Traces** (highlighted in green):
  - Trace specification with copy-paste ready format
  - Application methods (bootstrap.properties, server.xml, jvm.options)
  - Step-by-step instructions with screenshots
  - Expected output description
  - Log volume estimate with visual indicator
- **Medium Confidence Traces** (highlighted in yellow)
- **Low Confidence Traces** (highlighted in gray)
- **Progressive Strategy Flowchart**: Decision tree for trace enablement

#### Page 9-10: L2 Support Package Preparation

- **File Collection Checklist** (with checkboxes):
  - Required files with descriptions
  - Optional files
  - Files to exclude
- **Sanitization Instructions**:
  - Sensitive data types to remove
  - Command examples for sanitization
  - Verification steps
- **Packaging Instructions**:
  - Compression commands
  - Size estimation
  - Upload instructions
- **Support Case Template**: Pre-filled template with diagnostic summary

#### Page 11: Configuration Review

- **Current Configuration Snippets**:
  - Relevant server.xml sections
  - Bootstrap properties
  - JVM options
- **Configuration Recommendations**:
  - Suggested changes with rationale
  - Before/after comparison

#### Page 12: Appendices

- **Full Stack Traces**: Complete stack traces for all occurrences
- **Recent Log Entries**: Last 500 lines of relevant logs
- **FFDC File References**: List of related FFDC files
- **Glossary**: Definitions of technical terms

### B.2 Email Template

#### Subject Line Format

```
[Liberty Diagnostic] {SEVERITY}: {ISSUE_TYPE} - {SERVER_NAME} - {TIMESTAMP}
```

Examples:

- `[Liberty Diagnostic] CRITICAL: SSL Handshake Failure - prodServer01 - 2026-05-18 06:30`
- `[Liberty Diagnostic] HIGH: Database Connection Timeout - testServer - 2026-05-18 14:22`

#### Email Body (HTML Format)

```html
<!DOCTYPE html>
<html>
  <head>
    <style>
      body {
        font-family: Arial, sans-serif;
        line-height: 1.6;
        color: #333;
      }
      .header {
        background-color: #0f62fe;
        color: white;
        padding: 20px;
      }
      .severity-critical {
        background-color: #da1e28;
        color: white;
        padding: 10px;
      }
      .severity-high {
        background-color: #ff832b;
        color: white;
        padding: 10px;
      }
      .severity-medium {
        background-color: #f1c21b;
        color: black;
        padding: 10px;
      }
      .severity-low {
        background-color: #24a148;
        color: white;
        padding: 10px;
      }
      .summary {
        background-color: #f4f4f4;
        padding: 15px;
        margin: 20px 0;
      }
      .action-item {
        background-color: #e8f4fd;
        padding: 10px;
        margin: 10px 0;
        border-left: 4px solid #0f62fe;
      }
      .footer {
        background-color: #f4f4f4;
        padding: 15px;
        margin-top: 30px;
        font-size: 0.9em;
      }
      table {
        border-collapse: collapse;
        width: 100%;
        margin: 20px 0;
      }
      th,
      td {
        border: 1px solid #ddd;
        padding: 12px;
        text-align: left;
      }
      th {
        background-color: #0f62fe;
        color: white;
      }
    </style>
  </head>
  <body>
    <div class="header">
      <h1>🔍 Liberty Diagnostic Report</h1>
      <p>Automated diagnostic analysis for your Liberty server</p>
    </div>

    <div class="severity-{SEVERITY_LEVEL}">
      <h2>⚠️ {SEVERITY_LEVEL}: {ISSUE_TYPE}</h2>
    </div>

    <div class="summary">
      <h3>Executive Summary</h3>
      <table>
        <tr>
          <th>Server</th>
          <td>{SERVER_NAME}</td>
        </tr>
        <tr>
          <th>Issue Type</th>
          <td>{ISSUE_TYPE}</td>
        </tr>
        <tr>
          <th>First Occurrence</th>
          <td>{FIRST_OCCURRENCE}</td>
        </tr>
        <tr>
          <th>Frequency</th>
          <td>{OCCURRENCE_COUNT} times in {TIME_WINDOW}</td>
        </tr>
        <tr>
          <th>Confidence</th>
          <td>{CONFIDENCE_SCORE}%</td>
        </tr>
      </table>
      <p><strong>Root Cause:</strong> {ROOT_CAUSE_SUMMARY}</p>
    </div>

    <h3>🎯 Immediate Actions Required</h3>
    <div class="action-item">
      <strong>1. {ACTION_1_TITLE}</strong>
      <p>{ACTION_1_DESCRIPTION}</p>
    </div>
    <div class="action-item">
      <strong>2. {ACTION_2_TITLE}</strong>
      <p>{ACTION_2_DESCRIPTION}</p>
    </div>
    <div class="action-item">
      <strong>3. {ACTION_3_TITLE}</strong>
      <p>{ACTION_3_DESCRIPTION}</p>
    </div>

    <h3>📊 Trace Recommendations for L2 Support</h3>
    <p>If you need to open a support case, enable these traces to gather diagnostic data:</p>
    <div class="action-item">
      <strong>High Confidence Trace:</strong>
      <pre style="background-color: #f4f4f4; padding: 10px; overflow-x: auto;">
{HIGH_CONFIDENCE_TRACE_SPEC}
        </pre
      >
      <p><em>Expected log volume: {LOG_VOLUME_ESTIMATE}</em></p>
    </div>

    <h3>📎 Detailed Report</h3>
    <p>A comprehensive PDF report is attached to this email with:</p>
    <ul>
      <li>Complete root cause analysis with confidence scores</li>
      <li>Step-by-step trace enablement instructions</li>
      <li>L2 support package preparation guide</li>
      <li>Full stack traces and configuration review</li>
      <li>Related APARs and documentation links</li>
    </ul>

    <div class="footer">
      <p><strong>Report ID:</strong> {REPORT_ID}</p>
      <p><strong>Generated:</strong> {TIMESTAMP}</p>
      <p><strong>Liberty Version:</strong> {LIBERTY_VERSION}</p>
      <hr />
      <p style="font-size: 0.8em; color: #666;">This is an automated diagnostic report from Open Liberty. For questions or feedback, contact your Liberty administrator.</p>
      <p style="font-size: 0.8em; color: #666;">To disable these reports, set <code>enabled="false"</code> in your <code>&lt;diagnosticSummary&gt;</code> configuration.</p>
    </div>
  </body>
</html>
```

### B.3 Email Attachment Naming Convention

```
liberty-diagnostic-{SERVER_NAME}-{ISSUE_TYPE}-{TIMESTAMP}.pdf
```

Examples:

- `liberty-diagnostic-prodServer01-ssl-handshake-20260518-063000.pdf`
- `liberty-diagnostic-testServer-db-timeout-20260518-142245.pdf`

### B.4 Email Delivery Options

#### Standard Delivery

- **To**: Primary operations team email
- **CC**: Development team lead
- **Priority**: Based on severity (Critical/High = High priority)

#### Escalation Delivery (for Critical issues)

- **To**: Operations team + On-call engineer
- **CC**: Development team + Management
- **Priority**: High
- **Additional**: SMS notification (if configured)

#### Digest Mode (Optional)

- Batch multiple reports into a single email
- Sent at configured intervals (e.g., hourly, daily)
- Useful for high-volume environments
- Summary table with links to individual PDF reports

### A.1 Database Connection Issues

```json
{
  "issue": "Database connection timeout",
  "traces": [
    {
      "spec": "com.ibm.ws.jdbc.*=all:com.ibm.ws.rsadapter.*=all:com.ibm.ws.connectionpool.*=all",
      "confidence": 0.95,
      "volume": "MEDIUM",
      "value": "Complete JDBC and connection pool diagnostics"
    }
  ]
}
```

### A.2 Authentication Failures

```json
{
  "issue": "LDAP authentication failure",
  "traces": [
    {
      "spec": "com.ibm.ws.security.registry.ldap.*=all:com.ibm.ws.security.authentication.*=all:com.ibm.ws.security.wim.*=all",
      "confidence": 0.9,
      "volume": "MEDIUM",
      "value": "LDAP registry and authentication flow"
    }
  ]
}
```

### A.3 JAX-RS/REST Issues

```json
{
  "issue": "REST service invocation failure",
  "traces": [
    {
      "spec": "com.ibm.ws.jaxrs.*=all:org.apache.cxf.*=all:com.ibm.ws.webcontainer.*=all",
      "confidence": 0.85,
      "volume": "HIGH",
      "value": "JAX-RS request processing and CXF internals"
    }
  ]
}
```
