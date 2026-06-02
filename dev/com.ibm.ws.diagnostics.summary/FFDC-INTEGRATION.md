# FFDC Integration for Diagnostic Summary Feature

## Overview

The Diagnostic Summary feature now automatically captures all FFDC (First Failure Data Capture) incidents through Liberty's `IncidentForwarder` mechanism. This provides seamless integration with Liberty's exception handling infrastructure.

## How It Works

### 1. IncidentForwarder Registration

When the `DiagnosticEventDetector` component activates, it registers an `IncidentForwarder` with Liberty's FFDC system:

```java
@Activate
protected void activate(Map<String, Object> properties) {
    // ... other initialization ...

    // Register FFDC incident forwarder
    boolean registered = FFDC.registerIncidentForwarder(incidentForwarder);
    if (registered) {
        logger.info("Successfully registered FFDC IncidentForwarder");
    }
}
```

### 2. Automatic Exception Capture

Whenever any Liberty component calls `FFDCFilter.processException()`, our forwarder receives the incident:

```java
private class FFDCIncidentForwarder implements IncidentForwarder {
    @Override
    public void process(Incident incident, Throwable th) {
        // Extract incident information
        String sourceId = incident.getSourceId();
        String probeId = incident.getProbeId();
        String exceptionName = incident.getExceptionName();

        // Process only first occurrence to avoid duplicates
        if (incident.getCount() == 1) {
            detectFFDC(th, sourceComponent, incidentId);
        }
    }
}
```

### 3. Component Name Extraction

The forwarder intelligently extracts component names from source IDs:

- `com.ibm.ws.jaxrs.2.0.server.LibertyJaxRsServerFactoryBean` → `jaxrs`
- `com.ibm.ws.webcontainer.servlet.ServletWrapper` → `webcontainer`
- Other packages → last segment of class name

### 4. Event Processing

Captured FFDC incidents are processed through the existing diagnostic pipeline:

1. **Event Creation**: Creates a `DiagnosticEvent` with type `FFDC` and severity `HIGH`
2. **Signature Generation**: Generates unique signature for deduplication
3. **Aggregation**: Adds to event aggregator for pattern analysis
4. **Trigger Checking**: Evaluates against configured thresholds
5. **Report Generation**: Triggers PDF report if thresholds exceeded

## Benefits

### Automatic Capture

- No manual instrumentation required
- Captures all FFDC incidents across Liberty runtime
- Works with existing Liberty code

### Deduplication

- Only processes first occurrence of each incident
- Prevents duplicate reports for same exception
- Uses FFDC's built-in count mechanism

### Component Attribution

- Automatically identifies source component
- Enables component-level analysis in reports
- Supports root cause analysis

### Seamless Integration

- Uses Liberty's standard FFDC infrastructure
- No changes to existing Liberty components
- Compatible with all Liberty features

## Configuration

The FFDC integration is automatically enabled when the `diagnosticSummary-1.0` feature is loaded. No additional configuration is required.

### Trigger Configuration

Control when FFDC incidents trigger report generation:

```xml
<diagnosticSummary>
    <immediateTriggers>
        <exceptionTrigger
            enabled="true"
            threshold="5"
            timeWindow="10m"
            severity="CRITICAL,HIGH"/>
    </immediateTriggers>
</diagnosticSummary>
```

## Example Flow

### 1. Exception Occurs in JAX-RS

```java
// In Liberty JAX-RS runtime
try {
    // ... JAX-RS processing ...
} catch (Exception e) {
    FFDCFilter.processException(e,
        "com.ibm.ws.jaxrs.2.0.server.LibertyJaxRsServerFactoryBean",
        "processRequest");
    throw e;
}
```

### 2. Incident Forwarded

```
[INFO] Processing FFDC incident: JsonbException from jaxrs:processRequest
```

### 3. Event Detected

```
[INFO] Detected FFDC event: JsonbException in component 'jaxrs'
Signature: TYPE:FFDC|EXCEPTION:JsonbException|COMPONENT:jaxrs
```

### 4. Threshold Checked

```
[INFO] Exception threshold exceeded: 5 occurrences of JsonbException in 10m
```

### 5. Report Generated

```
[INFO] Generating diagnostic report for trigger: Exception threshold exceeded
[INFO] PDF report generated: diagnostic-report-myserver-a1b2c3d4.pdf
```

## Lifecycle Management

### Activation

- Registers `IncidentForwarder` with FFDC system
- Begins capturing all FFDC incidents
- Logs registration status

### Deactivation

- Deregisters `IncidentForwarder`
- Stops capturing incidents
- Cleans up resources

## Testing

### Verify FFDC Integration

1. Enable the feature:

```xml
<featureManager>
    <feature>diagnosticSummary-1.0</feature>
</featureManager>
```

2. Check logs for registration:

```
[INFO] Successfully registered FFDC IncidentForwarder
```

3. Trigger an exception in your application

4. Verify incident processing:

```
[INFO] Processing FFDC incident: <ExceptionType> from <component>
```

5. Check for generated PDF:

```
<server_output_dir>/logs/diagnostics/diagnostic-report-*.pdf
```

## Troubleshooting

### No Incidents Captured

**Problem**: FFDC incidents occur but are not captured

**Solutions**:

- Verify feature is enabled in server.xml
- Check logs for "Successfully registered FFDC IncidentForwarder"
- Ensure exceptions are being logged to FFDC (check ffdc/ directory)

### Duplicate Reports

**Problem**: Multiple reports generated for same exception

**Solutions**:

- Verify count check is working (should only process count=1)
- Check trigger thresholds are not too low
- Review time window configuration

### Component Name Issues

**Problem**: Component names are "unknown" or incorrect

**Solutions**:

- Check source ID format in FFDC logs
- Update `extractComponentName()` logic if needed
- Add custom mapping for specific packages

## Related Files

- [`DiagnosticEventDetector.java`](src/com/ibm/ws/diagnostics/summary/internal/DiagnosticEventDetector.java) - Main detector with FFDC integration
- [`bnd.bnd`](bnd.bnd) - Build configuration with FFDC dependencies
- [`metatype.xml`](resources/OSGI-INF/metatype/metatype.xml) - Configuration schema

## References

- Liberty FFDC: `com.ibm.ws.logging.core/src/com/ibm/ws/ffdc/FFDC.java`
- Incident Interface: `com.ibm.ws.logging.core/src/com/ibm/wsspi/logging/Incident.java`
- IncidentForwarder: `com.ibm.ws.logging.core/src/com/ibm/wsspi/logging/IncidentForwarder.java`
- Example Usage: `com.ibm.ws.collector.manager/src/com/ibm/ws/logging/ffdc/source/FFDCSource.java`
