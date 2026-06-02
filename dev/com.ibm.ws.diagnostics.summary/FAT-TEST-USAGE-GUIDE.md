# Diagnostic Summary Feature - FAT Test Usage Guide

This guide shows how to use the Diagnostic Summary feature in your FAT tests to automatically generate diagnostic reports when issues occur.

## Overview

The Diagnostic Summary feature can be enabled in your FAT test's server configuration to:

- Automatically detect exceptions, FFDC events, and health check failures
- Generate PDF reports with root cause analysis
- Provide trace recommendations for L2 support
- Save reports to `${server.output.dir}/logs/diagnostics/`

## Quick Start Example

Here's how to add diagnostic summary to your existing FAT test (using `io.openliberty.restfulWS.4.0_fat` as an example):

### Step 1: Create Server Configuration

Create or modify your server's `server.xml` to include the diagnostic summary feature:

**File**: `dev/io.openliberty.restfulWS.4.0_fat/publish/servers/io.openliberty.restfulWS.4.0.examples.fat/server.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<server description="REST 4.0 Examples Test Server">

    <!-- Enable diagnostic summary feature -->
    <featureManager>
        <feature>restfulWS-4.0</feature>
        <feature>diagnosticSummary-1.0</feature>
    </featureManager>

    <!-- Configure diagnostic summary -->
    <diagnosticSummary>
        <!-- Enable the feature -->
        <enabled>true</enabled>

        <!-- Immediate triggers for critical issues -->
        <immediateTriggers>
            <exceptionThreshold count="5" windowMinutes="10"/>
            <ffdcThreshold count="3" windowMinutes="5"/>
            <healthCheckFailureEnabled>true</healthCheckFailureEnabled>
        </immediateTriggers>

        <!-- Scheduled analysis (optional) -->
        <scheduledAnalysis>
            <enabled>false</enabled>
        </scheduledAnalysis>

        <!-- PDF report configuration -->
        <pdfReport>
            <enabled>true</enabled>
            <includeStackTraces>true</includeStackTraces>
            <maxStackTraceLines>50</maxStackTraceLines>
        </pdfReport>
    </diagnosticSummary>

    <!-- Your existing configuration -->
    <httpEndpoint id="defaultHttpEndpoint"
                  httpPort="${bvt.prop.HTTP_default}"
                  httpsPort="${bvt.prop.HTTP_default.secure}"/>

    <application location="rest40examples.war"/>
</server>
```

### Step 2: Update Your FAT Test Class

Modify your test class to verify diagnostic reports are generated:

**File**: `dev/io.openliberty.restfulWS.4.0_fat/fat/src/io/openliberty/restfulWS40/fat/Rest40ExamplesTest.java`

```java
package io.openliberty.restfulWS40.fat;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.restfulWS40.fat.rest40examples.Rest40ExamplesTestServlet;

@RunWith(FATRunner.class)
public class Rest40ExamplesTest extends FATServletClient {

    private static final String APP_NAME = "rest40examples";
    private static final Class<?> c = Rest40ExamplesTest.class;

    @Server("io.openliberty.restfulWS.4.0.examples.fat")
    @TestServlet(servlet = Rest40ExamplesTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, APP_NAME,
                                                       "io.openliberty.restfulWS40.fat.rest40examples");

        // Start server
        server.startServer();

        // Wait for diagnostic summary feature to be ready
        server.waitForStringInLog("CWWKF0011I.*diagnosticSummary-1.0");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            // Check for diagnostic reports before stopping
            checkForDiagnosticReports();

            server.stopServer("CWWKG0075E");
        }
    }

    /**
     * Test that verifies diagnostic reports are generated when exceptions occur
     */
    @Test
    public void testDiagnosticReportGeneration() throws Exception {
        Log.info(c, "testDiagnosticReportGeneration", "Testing diagnostic report generation");

        // Trigger some exceptions to generate a diagnostic report
        // (This would normally happen during your actual test execution)
        triggerExceptionsForTesting();

        // Wait for diagnostic report to be generated
        Thread.sleep(5000);

        // Verify diagnostic report was created
        List<File> reports = findDiagnosticReports();
        assertFalse("Expected at least one diagnostic report to be generated", reports.isEmpty());

        // Verify report content
        File latestReport = reports.get(reports.size() - 1);
        assertTrue("Diagnostic report should exist: " + latestReport.getAbsolutePath(),
                   latestReport.exists());
        assertTrue("Diagnostic report should be a PDF file",
                   latestReport.getName().endsWith(".pdf"));
        assertTrue("Diagnostic report should not be empty",
                   latestReport.length() > 0);

        Log.info(c, "testDiagnosticReportGeneration",
                 "Diagnostic report generated: " + latestReport.getAbsolutePath());
    }

    /**
     * Helper method to trigger exceptions for testing
     */
    private static void triggerExceptionsForTesting() throws Exception {
        // Example: Call an endpoint that throws exceptions
        // In a real test, this would be part of your normal test flow
        for (int i = 0; i < 6; i++) {
            try {
                // Make a request that causes an exception
                runTest(server, APP_NAME + "/Rest40ExamplesTestServlet", "testInvalidOperation");
            } catch (Exception e) {
                // Expected - we're triggering exceptions on purpose
            }
        }
    }

    /**
     * Check for diagnostic reports in the server logs directory
     */
    private static void checkForDiagnosticReports() throws Exception {
        List<File> reports = findDiagnosticReports();

        if (!reports.isEmpty()) {
            Log.info(c, "checkForDiagnosticReports",
                     "Found " + reports.size() + " diagnostic report(s):");
            for (File report : reports) {
                Log.info(c, "checkForDiagnosticReports",
                         "  - " + report.getName() + " (" + report.length() + " bytes)");
            }
        } else {
            Log.info(c, "checkForDiagnosticReports", "No diagnostic reports found");
        }
    }

    /**
     * Find all diagnostic reports in the server's logs directory
     */
    private static List<File> findDiagnosticReports() throws Exception {
        String serverRoot = server.getServerRoot();
        Path diagnosticsDir = Paths.get(serverRoot, "logs", "diagnostics");

        if (!Files.exists(diagnosticsDir)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(diagnosticsDir)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".pdf"))
                .filter(p -> p.getFileName().toString().startsWith("diagnostic-report-"))
                .map(Path::toFile)
                .collect(Collectors.toList());
        }
    }
}
```

### Step 3: Add Test Servlet Method (Optional)

If you want to explicitly test exception handling, add a method to your test servlet:

**File**: `dev/io.openliberty.restfulWS.4.0_fat/test-applications/rest40examples/src/io/openliberty/restfulWS40/fat/rest40examples/Rest40ExamplesTestServlet.java`

```java
/**
 * Test method that intentionally throws exceptions to trigger diagnostic report
 */
@Test
public void testInvalidOperation() throws Exception {
    // This will trigger an exception that gets detected by diagnostic summary
    throw new IllegalStateException("Test exception for diagnostic summary");
}
```

## Configuration Options

### Minimal Configuration

```xml
<diagnosticSummary>
    <enabled>true</enabled>
</diagnosticSummary>
```

### Full Configuration with All Options

```xml
<diagnosticSummary>
    <!-- Enable/disable the feature -->
    <enabled>true</enabled>

    <!-- Immediate triggers -->
    <immediateTriggers>
        <!-- Trigger when 5+ exceptions occur within 10 minutes -->
        <exceptionThreshold count="5" windowMinutes="10"/>

        <!-- Trigger when 3+ FFDC events occur within 5 minutes -->
        <ffdcThreshold count="3" windowMinutes="5"/>

        <!-- Trigger on health check failures -->
        <healthCheckFailureEnabled>true</healthCheckFailureEnabled>

        <!-- Trigger on circuit breaker open -->
        <circuitBreakerEnabled>true</circuitBreakerEnabled>

        <!-- Trigger on OOM detection -->
        <oomDetectionEnabled>true</oomDetectionEnabled>
    </immediateTriggers>

    <!-- Scheduled analysis (proactive reports) -->
    <scheduledAnalysis>
        <enabled>true</enabled>
        <intervalHours>4</intervalHours>
        <analysisWindowHours>24</analysisWindowHours>
    </scheduledAnalysis>

    <!-- PDF report configuration -->
    <pdfReport>
        <enabled>true</enabled>
        <includeStackTraces>true</includeStackTraces>
        <maxStackTraceLines>50</maxStackTraceLines>
        <includeConfiguration>true</includeConfiguration>
        <maxRootCauses>5</maxRootCauses>
        <maxTraceRecommendations>10</maxTraceRecommendations>
    </pdfReport>

    <!-- Analysis configuration -->
    <analysis>
        <confidenceThreshold>50</confidenceThreshold>
        <maxEventsToAnalyze>1000</maxEventsToAnalyze>
        <eventRetentionHours>24</eventRetentionHours>
    </analysis>
</diagnosticSummary>
```

## Verifying Reports in FAT Tests

### Method 1: Check for Report Files

```java
@Test
public void testReportGeneration() throws Exception {
    // Trigger conditions that should generate a report
    triggerExceptions();

    // Wait for report generation
    Thread.sleep(5000);

    // Find reports
    Path diagnosticsDir = Paths.get(server.getServerRoot(), "logs", "diagnostics");
    assertTrue("Diagnostics directory should exist", Files.exists(diagnosticsDir));

    List<Path> reports = Files.list(diagnosticsDir)
        .filter(p -> p.getFileName().toString().startsWith("diagnostic-report-"))
        .collect(Collectors.toList());

    assertFalse("At least one report should be generated", reports.isEmpty());
}
```

### Method 2: Check Server Logs

```java
@Test
public void testReportGenerationLogged() throws Exception {
    // Trigger conditions
    triggerExceptions();

    // Wait and check logs
    String reportGenerated = server.waitForStringInLog("Diagnostic report generated:");
    assertNotNull("Report generation should be logged", reportGenerated);
}
```

### Method 3: Verify Report Content

```java
@Test
public void testReportContent() throws Exception {
    // Generate report
    triggerExceptions();
    Thread.sleep(5000);

    // Find latest report
    File report = findLatestDiagnosticReport();
    assertNotNull("Report should exist", report);

    // Verify it's a valid PDF
    assertTrue("Should be PDF file", report.getName().endsWith(".pdf"));
    assertTrue("Should not be empty", report.length() > 1000);

    // Optional: Use PDFBox to verify content
    try (PDDocument doc = PDDocument.load(report)) {
        assertTrue("Should have multiple pages", doc.getNumberOfPages() > 0);
    }
}
```

## Common Use Cases

### Use Case 1: Test Exception Handling

```java
@Test
public void testExceptionHandling() throws Exception {
    // Your test that may cause exceptions
    runTest(server, APP_NAME + "/servlet", "testMethod");

    // Verify diagnostic report captures the exceptions
    Thread.sleep(5000);
    List<File> reports = findDiagnosticReports();

    if (!reports.isEmpty()) {
        Log.info(c, "testExceptionHandling",
                 "Diagnostic report generated for exception analysis");
    }
}
```

### Use Case 2: Test Health Check Integration

```xml
<!-- In server.xml -->
<diagnosticSummary>
    <enabled>true</enabled>
    <immediateTriggers>
        <healthCheckFailureEnabled>true</healthCheckFailureEnabled>
    </immediateTriggers>
</diagnosticSummary>

<mpHealth>
    <readinessCheck>true</readinessCheck>
    <livenessCheck>true</livenessCheck>
</mpHealth>
```

```java
@Test
public void testHealthCheckFailureDetection() throws Exception {
    // Cause a health check to fail
    causeHealthCheckFailure();

    // Verify diagnostic report is generated
    String reportLog = server.waitForStringInLog("Diagnostic report generated.*health check");
    assertNotNull("Report should be generated for health check failure", reportLog);
}
```

### Use Case 3: Scheduled Analysis Testing

```xml
<diagnosticSummary>
    <enabled>true</enabled>
    <scheduledAnalysis>
        <enabled>true</enabled>
        <intervalHours>1</intervalHours>
    </scheduledAnalysis>
</diagnosticSummary>
```

```java
@Test
public void testScheduledAnalysis() throws Exception {
    // Run test for over an hour to trigger scheduled analysis
    // Or manually trigger via MBean (if implemented)

    // Wait for scheduled report
    String scheduledReport = server.waitForStringInLog(
        "Scheduled diagnostic report generated",
        3700000); // 1 hour + buffer

    assertNotNull("Scheduled report should be generated", scheduledReport);
}
```

## Troubleshooting

### Reports Not Generated

1. **Check feature is enabled**:

   ```
   server.waitForStringInLog("CWWKF0011I.*diagnosticSummary-1.0");
   ```

2. **Check thresholds are met**:
   - Default: 5 exceptions in 10 minutes
   - Verify your test triggers enough events

3. **Check server logs**:

   ```
   grep "diagnostic" ${server.output.dir}/logs/messages.log
   ```

4. **Verify directory permissions**:
   - Reports save to `${server.output.dir}/logs/diagnostics/`
   - Ensure directory is writable

### Reports Empty or Incomplete

1. **Increase analysis window**:

   ```xml
   <analysis>
       <eventRetentionHours>48</eventRetentionHours>
   </analysis>
   ```

2. **Lower confidence threshold**:
   ```xml
   <analysis>
       <confidenceThreshold>30</confidenceThreshold>
   </analysis>
   ```

## Best Practices

1. **Enable in Test Setup**: Add diagnostic summary to your test server configuration
2. **Check Reports in Teardown**: Verify and log any generated reports
3. **Archive Reports**: Copy reports to test output for debugging
4. **Use Appropriate Thresholds**: Set thresholds that match your test scenarios
5. **Clean Up**: Remove old reports between test runs if needed

## Example: Complete FAT Test Integration

See the complete example in:

- Server config: `dev/io.openliberty.restfulWS.4.0_fat/publish/servers/io.openliberty.restfulWS.4.0.examples.fat/server.xml`
- Test class: `dev/io.openliberty.restfulWS.4.0_fat/fat/src/io/openliberty/restfulWS40/fat/Rest40ExamplesTest.java`

## Additional Resources

- [Technical Proposal](diagnostic-summary-technical-proposal.md)
- [Implementation Plan](../diagnostic-summary-implementation-plan.md)
- [Milestone Documentation](MILESTONE-1.1-COMPLETE.md)
