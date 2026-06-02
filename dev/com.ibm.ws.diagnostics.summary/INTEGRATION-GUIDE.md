# Diagnostic Summary - Real Integration Guide

This guide explains how to properly integrate the Diagnostic Summary feature with real Liberty subsystems for production use.

## Current Status: Proof of Concept

The current implementation is a **proof of concept** that demonstrates the architecture and approach. To make it production-ready, you need to integrate with real Liberty subsystems.

---

## 1. Real FFDC/Exception Detection Hooks

### Current State (POC)

The current [`DiagnosticEventDetector`](src/com/ibm/ws/diagnostics/summary/internal/DiagnosticEventDetector.java:1) has placeholder methods that simulate exception detection.

### Production Integration Steps

#### Step 1: Add FFDC Bundle Dependency

**File: [`bnd.bnd`](bnd.bnd:42)**

Add the actual FFDC bundle to `-buildpath`:

```properties
-buildpath: \
  ...,\
  com.ibm.ws.logging;version=latest,\
  io.openliberty.jakarta.jsonp.2.1;version=latest
```

**Note**: The actual FFDC integration bundle name may vary. Check existing Liberty bundles that use FFDC.

#### Step 2: Create FFDC Listener Service

Create a new class that listens to FFDC events:

```java
package com.ibm.ws.diagnostics.summary.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.IncidentStream;

@Component(service = FFDCListener.class, immediate = true)
public class FFDCListener {

    @Reference
    private DiagnosticEventDetector detector;

    /**
     * Called by Liberty's FFDC framework when an exception occurs.
     * This is the real integration point.
     */
    public void processException(Throwable throwable, String sourceId, String probeId) {
        // Extract exception details
        String message = throwable.getMessage();
        String stackTrace = getStackTrace(throwable);
        String component = extractComponent(sourceId);

        // Create diagnostic event
        detector.recordException(
            throwable.getClass().getName(),
            message,
            stackTrace,
            component
        );
    }

    private String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private String extractComponent(String sourceId) {
        // Parse sourceId to determine component
        // Example: "com.ibm.ws.jaxrs.2.0.server.LibertyJaxRsServerFactoryBean"
        if (sourceId.contains(".jaxrs.")) return "JAX-RS";
        if (sourceId.contains(".jpa.")) return "JPA";
        if (sourceId.contains(".security.")) return "Security";
        // ... add more mappings
        return "Unknown";
    }
}
```

#### Step 3: Register with FFDC Framework

You may need to register your listener with Liberty's FFDC framework. Check the FFDC documentation or existing FFDC consumers in the Liberty codebase for the exact registration mechanism.

**Alternative Approach**: Use OSGi Event Admin to subscribe to FFDC events if Liberty publishes them as OSGi events.

---

## 2. MicroProfile Health Integration

### Current State (POC)

The current implementation has placeholder health check detection.

### Production Integration Steps

#### Step 1: Add MicroProfile Health Dependencies

**File: [`bnd.bnd`](bnd.bnd:42)**

```properties
-buildpath: \
  ...,\
  io.openliberty.jakarta.jsonp.2.1;version=latest,\
  io.openliberty.microprofile.health.3.1;version=latest
```

**File: [`diagnosticSummary-1.0.mf`](resources/OSGI-INF/subsystem/diagnosticSummary-1.0.mf:1)**

Add to `Subsystem-Content`:

```
Subsystem-Content:
  ...
  io.openliberty.microprofile.health.3.1; type="osgi.subsystem.feature"
```

#### Step 2: Create Health Check Monitor

```java
package com.ibm.ws.diagnostics.summary.internal;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component(service = HealthCheckMonitor.class, immediate = true)
public class HealthCheckMonitor {

    @Reference
    private DiagnosticEventDetector detector;

    // Track all registered health checks
    private final Map<String, HealthCheck> healthChecks = new ConcurrentHashMap<>();

    /**
     * OSGi will call this for each HealthCheck service that gets registered.
     */
    @Reference(
        service = HealthCheck.class,
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    protected void addHealthCheck(HealthCheck healthCheck) {
        String name = healthCheck.getClass().getName();
        healthChecks.put(name, healthCheck);
    }

    protected void removeHealthCheck(HealthCheck healthCheck) {
        healthChecks.remove(healthCheck.getClass().getName());
    }

    /**
     * Periodically check all health checks and report failures.
     * This should be called by a scheduled executor.
     */
    public void checkAllHealthChecks() {
        for (Map.Entry<String, HealthCheck> entry : healthChecks.entrySet()) {
            try {
                HealthCheckResponse response = entry.getValue().call();

                if (response.getStatus() == HealthCheckResponse.Status.DOWN) {
                    // Health check failed - create diagnostic event
                    detector.recordHealthCheckFailure(
                        entry.getKey(),
                        response.getName(),
                        response.getData().orElse(Map.of()).toString()
                    );
                }
            } catch (Exception e) {
                // Health check threw exception
                detector.recordException(
                    e.getClass().getName(),
                    "Health check failed: " + entry.getKey(),
                    getStackTrace(e),
                    "MicroProfile Health"
                );
            }
        }
    }

    private String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
```

#### Step 3: Schedule Health Check Monitoring

Add to [`DiagnosticEventDetector`](src/com/ibm/ws/diagnostics/summary/internal/DiagnosticEventDetector.java:1):

```java
@Reference
private HealthCheckMonitor healthCheckMonitor;

private ScheduledExecutorService healthCheckScheduler;

@Activate
protected void activate(Map<String, Object> properties) {
    // ... existing code ...

    // Schedule health check monitoring every 30 seconds
    healthCheckScheduler = Executors.newSingleThreadScheduledExecutor();
    healthCheckScheduler.scheduleAtFixedRate(
        () -> healthCheckMonitor.checkAllHealthChecks(),
        30, 30, TimeUnit.SECONDS
    );
}

@Deactivate
protected void deactivate() {
    if (healthCheckScheduler != null) {
        healthCheckScheduler.shutdown();
    }
    // ... existing code ...
}
```

---

## 3. Email Delivery Implementation

### Current State (POC)

Email delivery is currently deferred per user request.

### Production Integration Steps

#### Step 1: Add JavaMail Dependencies

**File: [`bnd.bnd`](bnd.bnd:42)**

```properties
-buildpath: \
  ...,\
  io.openliberty.jakarta.jsonp.2.1;version=latest,\
  com.ibm.websphere.javaee.mail.1.6;version=latest
```

**File: [`diagnosticSummary-1.0.mf`](resources/OSGI-INF/subsystem/diagnosticSummary-1.0.mf:1)**

```
Subsystem-Content:
  ...
  com.ibm.websphere.appserver.javaMail-1.6; type="osgi.subsystem.feature"
```

#### Step 2: Create Email Service

```java
package com.ibm.ws.diagnostics.summary.notification;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.util.Properties;

@Component(service = EmailService.class)
public class EmailService {

    @Reference
    private DiagnosticSummaryConfig config;

    /**
     * Send diagnostic report via email.
     */
    public void sendReport(File pdfReport, String subject) throws MessagingException {
        // Get email configuration from server.xml
        String smtpHost = config.getSmtpHost();
        int smtpPort = config.getSmtpPort();
        String from = config.getEmailFrom();
        String[] recipients = config.getEmailRecipients();
        boolean useTLS = config.isSmtpUseTLS();
        String username = config.getSmtpUsername();
        String password = config.getSmtpPassword();

        // Configure mail session
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", username != null);
        props.put("mail.smtp.starttls.enable", useTLS);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        // Create message
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));

        for (String recipient : recipients) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        }

        message.setSubject(subject);

        // Create multipart message with PDF attachment
        Multipart multipart = new MimeMultipart();

        // Text part
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Please find attached the diagnostic summary report.");
        multipart.addBodyPart(textPart);

        // PDF attachment
        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.attachFile(pdfReport);
        attachmentPart.setFileName(pdfReport.getName());
        multipart.addBodyPart(attachmentPart);

        message.setContent(multipart);

        // Send
        Transport.send(message);
    }
}
```

#### Step 3: Update Configuration Schema

**File: [`metatype.xml`](resources/OSGI-INF/metatype/metatype.xml:1)**

Add email configuration attributes:

```xml
<AD id="smtpHost" name="%smtpHost" description="%smtpHost.desc"
    type="String" default="localhost" required="false"/>
<AD id="smtpPort" name="%smtpPort" description="%smtpPort.desc"
    type="Integer" default="25" required="false"/>
<AD id="smtpUseTLS" name="%smtpUseTLS" description="%smtpUseTLS.desc"
    type="Boolean" default="false" required="false"/>
<AD id="smtpUsername" name="%smtpUsername" description="%smtpUsername.desc"
    type="String" required="false"/>
<AD id="smtpPassword" name="%smtpPassword" description="%smtpPassword.desc"
    type="String" ibm:type="password" required="false"/>
<AD id="emailFrom" name="%emailFrom" description="%emailFrom.desc"
    type="String" default="liberty@example.com" required="false"/>
<AD id="emailRecipients" name="%emailRecipients" description="%emailRecipients.desc"
    type="String" cardinality="2147483647" required="false"/>
```

#### Step 4: Update server.xml Example

```xml
<diagnosticSummary enabled="true">
    <exceptionThreshold count="10" timeWindowMinutes="5"/>
    <schedule interval="4h"/>

    <!-- Email Configuration -->
    <email>
        <smtpHost>smtp.example.com</smtpHost>
        <smtpPort>587</smtpPort>
        <smtpUseTLS>true</smtpUseTLS>
        <smtpUsername>liberty-alerts</smtpUsername>
        <smtpPassword>{xor}Lz4sLCgwLTs=</smtpPassword>
        <emailFrom>liberty-diagnostics@example.com</emailFrom>
        <emailRecipients>
            <recipient>admin@example.com</recipient>
            <recipient>support@example.com</recipient>
        </emailRecipients>
    </email>
</diagnosticSummary>
```

---

## 4. External Library Dependencies (PDFBox, JFreeChart)

### Current State (POC)

The code imports PDFBox and JFreeChart but they're not properly configured in the build.

### Production Integration Steps

#### Option A: Bundle Libraries Directly (Recommended for Liberty)

**Step 1: Download JAR files**

Download these JARs:

- `pdfbox-2.0.29.jar`
- `fontbox-2.0.29.jar` (required by PDFBox)
- `jfreechart-1.5.4.jar`
- `jcommon-1.0.24.jar` (required by JFreeChart)

**Step 2: Add to bundle's lib directory**

```bash
mkdir -p dev/com.ibm.ws.diagnostics.summary/lib
# Copy JARs to lib/
```

**Step 3: Update bnd.bnd**

```properties
Bundle-ClassPath: .,\
  lib/pdfbox-2.0.29.jar,\
  lib/fontbox-2.0.29.jar,\
  lib/jfreechart-1.5.4.jar,\
  lib/jcommon-1.0.24.jar

-includeresource: \
  lib/pdfbox-2.0.29.jar,\
  lib/fontbox-2.0.29.jar,\
  lib/jfreechart-1.5.4.jar,\
  lib/jcommon-1.0.24.jar

Import-Package: \
  !org.apache.pdfbox.*,\
  !org.jfree.*,\
  *
```

#### Option B: Create Separate Library Bundles

Create separate OSGi bundles for PDFBox and JFreeChart:

**Step 1: Create library bundle projects**

```bash
mkdir -p dev/com.ibm.ws.thirdparty.pdfbox
mkdir -p dev/com.ibm.ws.thirdparty.jfreechart
```

**Step 2: Create bnd.bnd for each library**

`dev/com.ibm.ws.thirdparty.pdfbox/bnd.bnd`:

```properties
Bundle-SymbolicName: com.ibm.ws.thirdparty.pdfbox
Bundle-Name: Apache PDFBox
Bundle-Version: 2.0.29

Export-Package: \
  org.apache.pdfbox.*;version="2.0.29",\
  org.apache.fontbox.*;version="2.0.29"

-includeresource: \
  @pdfbox-2.0.29.jar!/!META-INF/maven/*,\
  @fontbox-2.0.29.jar!/!META-INF/maven/*
```

**Step 3: Reference in diagnosticSummary bundle**

`dev/com.ibm.ws.diagnostics.summary/bnd.bnd`:

```properties
-buildpath: \
  ...,\
  com.ibm.ws.thirdparty.pdfbox;version=latest,\
  com.ibm.ws.thirdparty.jfreechart;version=latest
```

`dev/com.ibm.ws.diagnostics.summary/resources/OSGI-INF/subsystem/diagnosticSummary-1.0.mf`:

```
Subsystem-Content:
  com.ibm.ws.diagnostics.summary; version="[1.0.0,1.0.200)",
  com.ibm.ws.thirdparty.pdfbox; version="[2.0.29,2.1.0)",
  com.ibm.ws.thirdparty.jfreechart; version="[1.5.4,1.6.0)"
```

#### Option C: Use Existing Liberty Third-Party Bundles

Check if Liberty already has PDFBox or JFreeChart bundles:

```bash
find dev -name "*pdfbox*" -o -name "*jfreechart*"
```

If they exist, just reference them in your `-buildpath` and feature manifest.

---

## 5. MicroProfile Fault Tolerance Integration

### Current State (POC)

Circuit breaker and timeout detection is placeholder code.

### Production Integration Steps

#### Step 1: Add Fault Tolerance Dependencies

**File: [`bnd.bnd`](bnd.bnd:42)**

```properties
-buildpath: \
  ...,\
  io.openliberty.microprofile.faulttolerance.3.0;version=latest
```

#### Step 2: Create Fault Tolerance Monitor

```java
package com.ibm.ws.diagnostics.summary.internal;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component(service = FaultToleranceMonitor.class)
public class FaultToleranceMonitor {

    @Reference
    private DiagnosticEventDetector detector;

    // Track circuit breaker states
    private final Map<String, CircuitBreakerState> circuitStates = new ConcurrentHashMap<>();

    /**
     * Called when a circuit breaker opens.
     */
    public void onCircuitBreakerOpen(String methodName, String reason) {
        detector.recordCircuitBreakerEvent(methodName, "OPEN", reason);
    }

    /**
     * Called when a timeout occurs.
     */
    public void onTimeout(String methodName, long duration, long limit) {
        detector.recordTimeoutEvent(methodName, duration, limit);
    }

    private static class CircuitBreakerState {
        String state; // CLOSED, OPEN, HALF_OPEN
        long lastStateChange;
        int failureCount;
    }
}
```

**Note**: The exact integration mechanism depends on how Liberty's Fault Tolerance implementation exposes events. You may need to use interceptors or aspect-oriented programming.

---

## 6. Testing the Real Integrations

### Unit Tests

Create unit tests for each integration:

```java
@Test
public void testFFDCIntegration() {
    // Simulate FFDC event
    FFDCListener listener = new FFDCListener();
    listener.processException(
        new NullPointerException("Test exception"),
        "com.ibm.ws.test.Component",
        "probe123"
    );

    // Verify event was recorded
    // ...
}
```

### FAT Tests

Create FAT tests that trigger real exceptions and verify reports are generated:

```java
@Test
public void testRealExceptionDetection() throws Exception {
    // Deploy app that throws exceptions
    server.startServer();

    // Trigger exceptions via REST API
    HttpURLConnection conn = HttpUtils.getHttpConnection(
        new URL("http://localhost:" + server.getHttpDefaultPort() + "/app/trigger-error"),
        HttpURLConnection.HTTP_INTERNAL_ERROR,
        10
    );

    // Wait for diagnostic report
    Thread.sleep(5000);

    // Verify PDF report was generated
    File reportDir = new File(server.getServerRoot() + "/logs/diagnostics");
    assertTrue("Report directory should exist", reportDir.exists());

    File[] reports = reportDir.listFiles((dir, name) -> name.endsWith(".pdf"));
    assertTrue("At least one PDF report should exist", reports.length > 0);
}
```

---

## 7. Production Checklist

Before deploying to production:

- [ ] FFDC integration tested with real exceptions
- [ ] Health check monitoring tested with failing health checks
- [ ] Email delivery tested with real SMTP server
- [ ] PDF generation tested with large datasets (1000+ events)
- [ ] Memory usage profiled under load
- [ ] Thread pool sizing validated
- [ ] File I/O permissions configured correctly
- [ ] Security audit completed
- [ ] Documentation updated
- [ ] Performance benchmarks completed

---

## 8. Performance Considerations

### Memory Management

```java
// In EventAggregator, limit event history
private static final int MAX_EVENTS_PER_SIGNATURE = 100;

public void addEvent(DiagnosticEvent event) {
    List<DiagnosticEvent> events = eventsBySignature.get(signature);
    if (events.size() > MAX_EVENTS_PER_SIGNATURE) {
        events.remove(0); // Remove oldest
    }
    events.add(event);
}
```

### Async Processing

```java
// Process reports asynchronously to avoid blocking
private ExecutorService reportExecutor = Executors.newFixedThreadPool(2);

public void generateReport() {
    reportExecutor.submit(() -> {
        try {
            // Generate and send report
        } catch (Exception e) {
            // Log error
        }
    });
}
```

### Rate Limiting

```java
// Limit report generation frequency
private long lastReportTime = 0;
private static final long MIN_REPORT_INTERVAL_MS = 60000; // 1 minute

public void maybeGenerateReport() {
    long now = System.currentTimeMillis();
    if (now - lastReportTime < MIN_REPORT_INTERVAL_MS) {
        return; // Too soon
    }
    lastReportTime = now;
    generateReport();
}
```

---

## Summary

This guide provides the roadmap for converting the proof-of-concept implementation into a production-ready feature. The key integration points are:

1. **FFDC**: Hook into Liberty's exception logging framework
2. **Health Checks**: Monitor MicroProfile Health endpoints
3. **Email**: Use JavaMail for report delivery
4. **Libraries**: Bundle PDFBox and JFreeChart properly
5. **Fault Tolerance**: Monitor circuit breakers and timeouts

Each integration requires careful testing and performance validation before production deployment.
