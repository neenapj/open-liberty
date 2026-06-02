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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.diagnostics.summary.DiagnosticEvent;

/**
 * Analyzes exceptions to generate unique signatures for grouping similar events.
 * The signature is based on:
 * - Exception class name
 * - Root cause exception class name
 * - Normalized stack trace (removing line numbers, variable names)
 * - Exception message pattern (removing specific values)
 */
public class ExceptionSignatureAnalyzer {

    private static final Pattern LINE_NUMBER_PATTERN = Pattern.compile(":\\d+\\)");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\d+");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+\\b");
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}");
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
    private static final Pattern PORT_PATTERN = Pattern.compile(":\\d{2,5}\\b");

    /**
     * Generate a signature for the given diagnostic event
     *
     * @param event the diagnostic event
     * @return signature string
     */
    public String generateSignature(DiagnosticEvent event) {
        if (event.getEventType() == DiagnosticEvent.EventType.EXCEPTION && event.getException() != null) {
            return generateExceptionSignature(event.getException(), event.getStackTrace());
        } else if (event.getExceptionClassName() != null) {
            return generateExceptionSignature(event.getExceptionClassName(),
                                             event.getExceptionMessage(),
                                             event.getStackTrace());
        } else {
            return generateGenericSignature(event);
        }
    }

    /**
     * Generate signature from Throwable object
     */
    private String generateExceptionSignature(Throwable throwable, String stackTrace) {
        String exceptionClass = throwable.getClass().getName();
        String message = throwable.getMessage();

        // Find root cause
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        String rootCauseClass = rootCause.getClass().getName();

        return generateExceptionSignature(exceptionClass, rootCauseClass, message, stackTrace);
    }

    /**
     * Generate signature from exception details
     */
    private String generateExceptionSignature(String exceptionClassName, String message, String stackTrace) {
        return generateExceptionSignature(exceptionClassName, exceptionClassName, message, stackTrace);
    }

    /**
     * Generate signature from full exception details
     */
    private String generateExceptionSignature(String exceptionClass, String rootCauseClass,
                                              String message, String stackTrace) {
        StringBuilder signatureBuilder = new StringBuilder();

        // Add exception class
        signatureBuilder.append("EX:").append(exceptionClass);

        // Add root cause if different
        if (!exceptionClass.equals(rootCauseClass)) {
            signatureBuilder.append("|RC:").append(rootCauseClass);
        }

        // Add normalized message pattern
        if (message != null && !message.isEmpty()) {
            String normalizedMessage = normalizeMessage(message);
            if (!normalizedMessage.isEmpty()) {
                signatureBuilder.append("|MSG:").append(normalizedMessage);
            }
        }

        // Add normalized stack trace signature
        if (stackTrace != null && !stackTrace.isEmpty()) {
            String stackSignature = generateStackTraceSignature(stackTrace);
            signatureBuilder.append("|ST:").append(stackSignature);
        }

        return signatureBuilder.toString();
    }

    /**
     * Generate signature for non-exception events
     */
    private String generateGenericSignature(DiagnosticEvent event) {
        StringBuilder signatureBuilder = new StringBuilder();

        signatureBuilder.append("TYPE:").append(event.getEventType());

        if (event.getSourceComponent() != null) {
            signatureBuilder.append("|COMP:").append(event.getSourceComponent());
        }

        signatureBuilder.append("|SEV:").append(event.getSeverity());

        return signatureBuilder.toString();
    }

    /**
     * Normalize exception message by removing specific values
     */
    private String normalizeMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        String normalized = message;

        // Remove UUIDs
        normalized = UUID_PATTERN.matcher(normalized).replaceAll("<UUID>");

        // Remove timestamps
        normalized = TIMESTAMP_PATTERN.matcher(normalized).replaceAll("<TIMESTAMP>");

        // Remove IP addresses
        normalized = IP_ADDRESS_PATTERN.matcher(normalized).replaceAll("<IP>");

        // Remove port numbers
        normalized = PORT_PATTERN.matcher(normalized).replaceAll(":<PORT>");

        // Remove specific numbers (but keep them if they're part of error codes)
        normalized = NUMBER_PATTERN.matcher(normalized).replaceAll("<NUM>");

        // Truncate if too long
        if (normalized.length() > 200) {
            normalized = normalized.substring(0, 200);
        }

        return normalized.trim();
    }

    /**
     * Generate a signature from stack trace by extracting key frames
     */
    private String generateStackTraceSignature(String stackTrace) {
        if (stackTrace == null || stackTrace.isEmpty()) {
            return "";
        }

        List<String> keyFrames = extractKeyStackFrames(stackTrace);

        if (keyFrames.isEmpty()) {
            return "";
        }

        // Create a hash of the key frames
        StringBuilder frameBuilder = new StringBuilder();
        for (String frame : keyFrames) {
            frameBuilder.append(frame).append("|");
        }

        return hashString(frameBuilder.toString());
    }

    /**
     * Extract key stack frames (first few frames, excluding common framework code)
     */
    private List<String> extractKeyStackFrames(String stackTrace) {
        List<String> keyFrames = new ArrayList<>();
        String[] lines = stackTrace.split("\\r?\\n");

        int frameCount = 0;
        int maxFrames = 5;

        for (String line : lines) {
            line = line.trim();

            // Skip empty lines and "Caused by" lines
            if (line.isEmpty() || line.startsWith("Caused by:")) {
                continue;
            }

            // Look for stack frame pattern: "at package.Class.method(File.java:123)"
            if (line.startsWith("at ")) {
                String frame = normalizeStackFrame(line);

                // Skip common framework frames
                if (!isFrameworkFrame(frame)) {
                    keyFrames.add(frame);
                    frameCount++;

                    if (frameCount >= maxFrames) {
                        break;
                    }
                }
            }
        }

        return keyFrames;
    }

    /**
     * Normalize a stack frame by removing line numbers and file names
     */
    private String normalizeStackFrame(String frame) {
        // Remove "at " prefix
        frame = frame.substring(3).trim();

        // Remove line numbers: (File.java:123) -> (File.java)
        frame = LINE_NUMBER_PATTERN.matcher(frame).replaceAll(")");

        // Remove variable names: $1, $2, etc.
        frame = VARIABLE_PATTERN.matcher(frame).replaceAll("");

        // Extract just the method signature
        int parenIndex = frame.indexOf('(');
        if (parenIndex > 0) {
            frame = frame.substring(0, parenIndex);
        }

        return frame;
    }

    /**
     * Check if a frame is from common framework code that should be ignored
     */
    private boolean isFrameworkFrame(String frame) {
        return frame.startsWith("java.lang.reflect.") ||
               frame.startsWith("sun.reflect.") ||
               frame.startsWith("java.lang.Thread.") ||
               frame.startsWith("java.util.concurrent.") ||
               frame.startsWith("org.eclipse.osgi.") ||
               frame.startsWith("org.apache.felix.");
    }

    /**
     * Generate a hash of a string for compact representation
     */
    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash code
            return String.valueOf(input.hashCode());
        }
    }
}

// Made with Bob
