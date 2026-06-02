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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.ibm.ws.diagnostics.summary.DiagnosticEvent;

/**
 * Implementation of DiagnosticEvent interface
 */
public class DiagnosticEventImpl implements DiagnosticEvent {

    private final String eventId;
    private final EventType eventType;
    private final Severity severity;
    private final Instant timestamp;
    private final String sourceComponent;
    private final Throwable exception;
    private final String exceptionClassName;
    private final String exceptionMessage;
    private final String stackTrace;
    private final Throwable rootCause;
    private final Map<String, Object> context;
    private final String threadName;
    private final String serverName;
    private final String signature;
    private final boolean immediateTrigger;

    private DiagnosticEventImpl(Builder builder) {
        this.eventId = builder.eventId != null ? builder.eventId : UUID.randomUUID().toString();
        this.eventType = builder.eventType;
        this.severity = builder.severity;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.sourceComponent = builder.sourceComponent;
        this.exception = builder.exception;
        this.exceptionClassName = builder.exceptionClassName;
        this.exceptionMessage = builder.exceptionMessage;
        this.stackTrace = builder.stackTrace;
        this.rootCause = builder.rootCause;
        this.context = Collections.unmodifiableMap(new HashMap<>(builder.context));
        this.threadName = builder.threadName != null ? builder.threadName : Thread.currentThread().getName();
        this.serverName = builder.serverName;
        this.signature = builder.signature;
        this.immediateTrigger = builder.immediateTrigger;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public EventType getEventType() {
        return eventType;
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String getSourceComponent() {
        return sourceComponent;
    }

    @Override
    public Throwable getException() {
        return exception;
    }

    @Override
    public String getExceptionClassName() {
        return exceptionClassName;
    }

    @Override
    public String getExceptionMessage() {
        return exceptionMessage;
    }

    @Override
    public String getStackTrace() {
        return stackTrace;
    }

    @Override
    public Throwable getRootCause() {
        return rootCause;
    }

    @Override
    public Map<String, Object> getContext() {
        return context;
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public boolean isImmediateTrigger() {
        return immediateTrigger;
    }

    @Override
    public String toString() {
        return String.format("DiagnosticEvent[id=%s, type=%s, severity=%s, timestamp=%s, component=%s, signature=%s]",
                eventId, eventType, severity, timestamp, sourceComponent, signature);
    }

    /**
     * Builder for creating DiagnosticEvent instances
     */
    public static class Builder {
        private String eventId;
        private EventType eventType;
        private Severity severity;
        private Instant timestamp;
        private String sourceComponent;
        private Throwable exception;
        private String exceptionClassName;
        private String exceptionMessage;
        private String stackTrace;
        private Throwable rootCause;
        private Map<String, Object> context = new HashMap<>();
        private String threadName;
        private String serverName;
        private String signature;
        private boolean immediateTrigger;

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder eventType(EventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder sourceComponent(String sourceComponent) {
            this.sourceComponent = sourceComponent;
            return this;
        }

        public Builder exception(Throwable exception) {
            this.exception = exception;
            if (exception != null) {
                this.exceptionClassName = exception.getClass().getName();
                this.exceptionMessage = exception.getMessage();
                this.stackTrace = getStackTraceAsString(exception);
                this.rootCause = findRootCause(exception);
            }
            return this;
        }

        public Builder exceptionClassName(String exceptionClassName) {
            this.exceptionClassName = exceptionClassName;
            return this;
        }

        public Builder exceptionMessage(String exceptionMessage) {
            this.exceptionMessage = exceptionMessage;
            return this;
        }

        public Builder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public Builder rootCause(Throwable rootCause) {
            this.rootCause = rootCause;
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.context.putAll(context);
            return this;
        }

        public Builder addContext(String key, Object value) {
            this.context.put(key, value);
            return this;
        }

        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder serverName(String serverName) {
            this.serverName = serverName;
            return this;
        }

        public Builder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public Builder immediateTrigger(boolean immediateTrigger) {
            this.immediateTrigger = immediateTrigger;
            return this;
        }

        public DiagnosticEventImpl build() {
            if (eventType == null) {
                throw new IllegalStateException("eventType is required");
            }
            if (severity == null) {
                throw new IllegalStateException("severity is required");
            }
            return new DiagnosticEventImpl(this);
        }

        private String getStackTraceAsString(Throwable throwable) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            return sw.toString();
        }

        private Throwable findRootCause(Throwable throwable) {
            Throwable cause = throwable;
            while (cause.getCause() != null && cause.getCause() != cause) {
                cause = cause.getCause();
            }
            return cause;
        }
    }
}

// Made with Bob
