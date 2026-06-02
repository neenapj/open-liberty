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
import java.util.Map;

/**
 * Represents a diagnostic event detected in the Liberty runtime.
 * Events can be exceptions, FFDC incidents, health check failures,
 * circuit breaker events, or other runtime conditions.
 */
public interface DiagnosticEvent {

    /**
     * Event types that can trigger diagnostic analysis
     */
    enum EventType {
        EXCEPTION,
        FFDC,
        HEALTH_CHECK_FAILURE,
        CIRCUIT_BREAKER_OPEN,
        TIMEOUT,
        OUT_OF_MEMORY,
        THREAD_DEADLOCK,
        STARTUP_FAILURE,
        SHUTDOWN_FAILURE,
        CUSTOM
    }

    /**
     * Severity levels for diagnostic events
     */
    enum Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        INFO
    }

    /**
     * Get the unique identifier for this event
     *
     * @return event ID
     */
    String getEventId();

    /**
     * Get the type of this event
     *
     * @return event type
     */
    EventType getEventType();

    /**
     * Get the severity of this event
     *
     * @return severity level
     */
    Severity getSeverity();

    /**
     * Get the timestamp when this event occurred
     *
     * @return event timestamp
     */
    Instant getTimestamp();

    /**
     * Get the source component that generated this event
     *
     * @return source component name
     */
    String getSourceComponent();

    /**
     * Get the exception associated with this event, if any
     *
     * @return exception or null
     */
    Throwable getException();

    /**
     * Get the exception class name
     *
     * @return exception class name or null
     */
    String getExceptionClassName();

    /**
     * Get the exception message
     *
     * @return exception message or null
     */
    String getExceptionMessage();

    /**
     * Get the full stack trace
     *
     * @return stack trace or null
     */
    String getStackTrace();

    /**
     * Get the root cause exception, if any
     *
     * @return root cause exception or null
     */
    Throwable getRootCause();

    /**
     * Get additional context information for this event
     *
     * @return context map (never null)
     */
    Map<String, Object> getContext();

    /**
     * Get the thread name where this event occurred
     *
     * @return thread name
     */
    String getThreadName();

    /**
     * Get the server name where this event occurred
     *
     * @return server name
     */
    String getServerName();

    /**
     * Get a signature that uniquely identifies similar events
     * Used for grouping and aggregation
     *
     * @return event signature
     */
    String getSignature();

    /**
     * Check if this event should trigger immediate analysis
     *
     * @return true if immediate trigger is warranted
     */
    boolean isImmediateTrigger();
}

// Made with Bob
