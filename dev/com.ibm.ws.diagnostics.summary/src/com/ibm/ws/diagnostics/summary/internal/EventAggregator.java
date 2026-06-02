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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.ibm.ws.diagnostics.summary.DiagnosticEvent;

/**
 * Aggregates diagnostic events using sliding time windows.
 * Maintains event history and groups events by signature for analysis.
 */
public class EventAggregator {

    private final CopyOnWriteArrayList<DiagnosticEvent> eventHistory;
    private final ConcurrentHashMap<String, EventGroup> eventGroups;
    private final Duration retentionPeriod;
    private final ExceptionSignatureAnalyzer signatureAnalyzer;

    /**
     * Create an event aggregator with default retention period (24 hours)
     */
    public EventAggregator() {
        this(Duration.ofHours(24));
    }

    /**
     * Create an event aggregator with specified retention period
     *
     * @param retentionPeriod how long to retain events in memory
     */
    public EventAggregator(Duration retentionPeriod) {
        this.eventHistory = new CopyOnWriteArrayList<>();
        this.eventGroups = new ConcurrentHashMap<>();
        this.retentionPeriod = retentionPeriod;
        this.signatureAnalyzer = new ExceptionSignatureAnalyzer();
    }

    /**
     * Add an event to the aggregator
     *
     * @param event the diagnostic event
     */
    public void addEvent(DiagnosticEvent event) {
        // Clean up old events first
        cleanupOldEvents();

        // Add to history
        eventHistory.add(event);

        // Group by signature
        String signature = event.getSignature();
        if (signature == null || signature.isEmpty()) {
            signature = signatureAnalyzer.generateSignature(event);
        }

        final String finalSignature = signature;
        eventGroups.computeIfAbsent(finalSignature, k -> new EventGroup(finalSignature))
                   .addEvent(event);
    }

    /**
     * Get events within a time window
     *
     * @param start start of time window
     * @param end end of time window
     * @return list of events in the window
     */
    public List<DiagnosticEvent> getEventsInWindow(Instant start, Instant end) {
        return eventHistory.stream()
                .filter(e -> !e.getTimestamp().isBefore(start) && !e.getTimestamp().isAfter(end))
                .collect(Collectors.toList());
    }

    /**
     * Get events within a duration from now
     *
     * @param duration duration to look back
     * @return list of events in the window
     */
    public List<DiagnosticEvent> getRecentEvents(Duration duration) {
        Instant start = Instant.now().minus(duration);
        return getEventsInWindow(start, Instant.now());
    }

    /**
     * Get the most recent N events
     *
     * @param limit maximum number of events
     * @return list of recent events
     */
    public List<DiagnosticEvent> getRecentEvents(int limit) {
        int size = eventHistory.size();
        if (size <= limit) {
            return new ArrayList<>(eventHistory);
        }
        return new ArrayList<>(eventHistory.subList(size - limit, size));
    }

    /**
     * Get all events grouped by signature
     *
     * @return map of signature to event list
     */
    public Map<String, List<DiagnosticEvent>> getGroupedEvents() {
        Map<String, List<DiagnosticEvent>> result = new HashMap<>();
        for (Map.Entry<String, EventGroup> entry : eventGroups.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getEvents());
        }
        return result;
    }

    /**
     * Get grouped events within a time window
     *
     * @param start start of time window
     * @param end end of time window
     * @return map of signature to event list
     */
    public Map<String, List<DiagnosticEvent>> getGroupedEventsInWindow(Instant start, Instant end) {
        Map<String, List<DiagnosticEvent>> result = new HashMap<>();

        for (Map.Entry<String, EventGroup> entry : eventGroups.entrySet()) {
            List<DiagnosticEvent> eventsInWindow = entry.getValue().getEventsInWindow(start, end);
            if (!eventsInWindow.isEmpty()) {
                result.put(entry.getKey(), eventsInWindow);
            }
        }

        return result;
    }

    /**
     * Check if a signature has exceeded a threshold within a time window
     *
     * @param signature event signature
     * @param threshold number of events
     * @param timeWindow time window to check
     * @return true if threshold exceeded
     */
    public boolean hasExceededThreshold(String signature, int threshold, Duration timeWindow) {
        EventGroup group = eventGroups.get(signature);
        if (group == null) {
            return false;
        }

        Instant start = Instant.now().minus(timeWindow);
        return group.getEventCountInWindow(start, Instant.now()) >= threshold;
    }

    /**
     * Get event count for a signature within a time window
     *
     * @param signature event signature
     * @param timeWindow time window to check
     * @return event count
     */
    public int getEventCount(String signature, Duration timeWindow) {
        EventGroup group = eventGroups.get(signature);
        if (group == null) {
            return 0;
        }

        Instant start = Instant.now().minus(timeWindow);
        return group.getEventCountInWindow(start, Instant.now());
    }

    /**
     * Get total event count
     *
     * @return total number of events
     */
    public int getTotalEventCount() {
        return eventHistory.size();
    }

    /**
     * Get unique signature count
     *
     * @return number of unique signatures
     */
    public int getUniqueSignatureCount() {
        return eventGroups.size();
    }

    /**
     * Clear all events
     */
    public void clear() {
        eventHistory.clear();
        eventGroups.clear();
    }

    /**
     * Clean up events older than retention period
     */
    private void cleanupOldEvents() {
        Instant cutoff = Instant.now().minus(retentionPeriod);

        // Remove old events from history
        Iterator<DiagnosticEvent> historyIter = eventHistory.iterator();
        while (historyIter.hasNext()) {
            DiagnosticEvent event = historyIter.next();
            if (event.getTimestamp().isBefore(cutoff)) {
                historyIter.remove();
            }
        }

        // Clean up event groups
        Iterator<Map.Entry<String, EventGroup>> groupIter = eventGroups.entrySet().iterator();
        while (groupIter.hasNext()) {
            Map.Entry<String, EventGroup> entry = groupIter.next();
            entry.getValue().removeOldEvents(cutoff);

            // Remove empty groups
            if (entry.getValue().isEmpty()) {
                groupIter.remove();
            }
        }
    }

    /**
     * Represents a group of events with the same signature
     */
    private static class EventGroup {
        private final String signature;
        private final CopyOnWriteArrayList<DiagnosticEvent> events;

        EventGroup(String signature) {
            this.signature = signature;
            this.events = new CopyOnWriteArrayList<>();
        }

        void addEvent(DiagnosticEvent event) {
            events.add(event);
        }

        List<DiagnosticEvent> getEvents() {
            return Collections.unmodifiableList(events);
        }

        List<DiagnosticEvent> getEventsInWindow(Instant start, Instant end) {
            return events.stream()
                    .filter(e -> !e.getTimestamp().isBefore(start) && !e.getTimestamp().isAfter(end))
                    .collect(Collectors.toList());
        }

        int getEventCountInWindow(Instant start, Instant end) {
            return (int) events.stream()
                    .filter(e -> !e.getTimestamp().isBefore(start) && !e.getTimestamp().isAfter(end))
                    .count();
        }

        void removeOldEvents(Instant cutoff) {
            events.removeIf(e -> e.getTimestamp().isBefore(cutoff));
        }

        boolean isEmpty() {
            return events.isEmpty();
        }

        String getSignature() {
            return signature;
        }
    }
}

// Made with Bob
