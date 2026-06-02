# FFDC Integration Testing Guide

## Changes Made

### 1. DiagnosticEventDetector - FFDC Integration

Added automatic FFDC exception capture through `IncidentForwarder`:

**Key Changes:**

- Implemented `FFDCIncidentForwarder` inner class
- Registers forwarder on component activation
- Deregisters on component deactivation
- Processes all FFDC incidents automatically
- Extracts component names from source IDs
- Only processes first occurrence to avoid duplicates

**Enhanced Logging:**

- Added detailed trace logging at every step
- Logs forwarder registration/deregistration
- Logs incident details (sourceId, probeId, exceptionName, count)
- Logs component extraction
- Logs event processing flow

### 2. Test Servlet - Enhanced Tracing

Added trace output to `testInvalidJsonMergePatch()`:

**Trace Points:**

- Test start/end markers
- Request details
- Response status
- Exception information

## Testing Steps

### 1. Build the Feature

```bash
cd dev
./gradlew com.ibm.ws.diagnostics.summary:clean com.ibm.ws.diagnostics.summary:build
```

### 2. Run FAT Test

```bash
./gradlew io.openliberty.restfulWS.4.0_fat:buildandrun
```

### 3. Check Logs

Look for these log entries in the test output:

#### Component Activation

```
[INFO] Activating DiagnosticEventDetector
[INFO] Successfully registered FFDC IncidentForwarder
[INFO] DiagnosticEventDetector activated for server: io.openliberty.restfulWS.4.0.examples.fat
```

#### Test Execution

```
========================================
=== TEST: testInvalidJsonMergePatch ===
========================================
Attempting to patch non-existent customer (ID: 99999)
This should trigger a 404 response and potentially an FFDC incident
```

#### FFDC Incident Processing

```
[INFO] === FFDC IncidentForwarder.process() CALLED ===
[INFO]   enabled=true, throwable=jakarta.json.bind.JsonbException
[INFO]   Incident details:
[INFO]     sourceId: com.ibm.ws.jaxrs...
[INFO]     probeId: ...
[INFO]     exceptionName: JsonbException
[INFO]     count: 1
[INFO]   Processing FFDC incident: JsonbException from ...
[INFO]   Extracted component: jaxrs
[INFO]   Calling detectFFDC()...
```

#### Event Detection

```
[INFO] === detectFFDC() called ===
[INFO]   enabled: true
[INFO]   throwable: jakarta.json.bind.JsonbException
[INFO]   sourceComponent: jaxrs
[INFO]   Generating signature...
[INFO]   Signature: TYPE:FFDC|EXCEPTION:JsonbException|...
[INFO]   Building DiagnosticEvent...
[INFO]   Processing event...
```

#### Event Processing

```
[INFO] === processEvent() called ===
[INFO]   Event type: FFDC
[INFO]   Severity: HIGH
[INFO]   Signature: TYPE:FFDC|EXCEPTION:JsonbException|...
[INFO]   Adding to aggregator...
[INFO]   Notifying 0 listeners...
[INFO]   Checking immediate triggers...
```

### 4. Verify PDF Generation

If thresholds are met, check for PDF:

```bash
ls -la build.image/wlp/usr/servers/io.openliberty.restfulWS.4.0.examples.fat/logs/diagnostics/
```

Expected file:

```
diagnostic-report-io.openliberty.restfulWS.4.0.examples.fat-<timestamp>.pdf
```

## Expected Behavior

### Scenario 1: FFDC Incident Occurs

1. Test triggers exception in JAX-RS runtime
2. Liberty calls `FFDCFilter.processException()`
3. Our `IncidentForwarder` receives the incident
4. Incident is processed and logged
5. Event is added to aggregator
6. Thresholds are checked

### Scenario 2: Threshold Not Met

- Event is captured and logged
- No PDF is generated (threshold not reached)
- Event remains in aggregator for pattern analysis

### Scenario 3: Threshold Met

- Multiple similar exceptions occur
- Threshold is exceeded
- PDF report is automatically generated
- Report includes root cause analysis and recommendations

## Troubleshooting

### No FFDC Forwarder Registration

**Symptom:** Missing log: "Successfully registered FFDC IncidentForwarder"

**Solutions:**

1. Verify feature is enabled in server.xml
2. Check component activation logs
3. Verify bnd.bnd has correct DS annotations

### FFDC Incidents Not Captured

**Symptom:** No "FFDC IncidentForwarder.process() CALLED" logs

**Solutions:**

1. Verify exceptions are being logged to FFDC
2. Check ffdc/ directory for incident files
3. Ensure forwarder was registered successfully
4. Verify `enabled` flag is true

### Events Not Processed

**Symptom:** Forwarder called but no "detectFFDC() called" logs

**Solutions:**

1. Check if count > 1 (duplicate filtering)
2. Verify throwable is not null
3. Check for exceptions in forwarder processing

### No PDF Generated

**Symptom:** Events processed but no PDF created

**Solutions:**

1. Check threshold configuration
2. Verify multiple similar exceptions occurred
3. Check output directory permissions
4. Review trigger checking logs

## Log File Locations

### Server Logs

```
build.image/wlp/usr/servers/io.openliberty.restfulWS.4.0.examples.fat/logs/
├── console.log          # Component activation, FFDC processing
├── messages.log         # High-level messages
├── trace.log           # Detailed trace (if enabled)
└── ffdc/               # FFDC incident files
```

### Test Logs

```
io.openliberty.restfulWS.4.0_fat/build/libs/autoFVT/output/
└── <test_name>/
    ├── output.txt      # Test execution output
    └── logs/           # Server logs from test
```

### Diagnostic Reports

```
build.image/wlp/usr/servers/<server_name>/logs/diagnostics/
└── diagnostic-report-*.pdf
```

## Verification Checklist

- [ ] Feature builds successfully
- [ ] Component activates without errors
- [ ] FFDC forwarder registers successfully
- [ ] Test servlet runs and logs trace output
- [ ] FFDC incidents are captured
- [ ] Incidents are processed through detectFFDC()
- [ ] Events are added to aggregator
- [ ] Thresholds are checked
- [ ] PDF is generated (if threshold met)

## Next Steps

After successful testing:

1. **Reduce Logging**: Remove verbose trace logging from production code
2. **Add Unit Tests**: Test FFDC forwarder in isolation
3. **Add FAT Tests**: Create dedicated tests for diagnostic summary
4. **Performance Testing**: Verify minimal overhead
5. **Documentation**: Update user documentation with FFDC integration details
