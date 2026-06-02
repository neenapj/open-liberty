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

/**
 * Diagnostic Summary Feature for Open Liberty
 *
 * <p>This package provides the core interfaces and data models for the
 * Diagnostic Summary feature, which automatically detects and analyzes
 * runtime issues in Open Liberty servers.</p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.ibm.ws.diagnostics.summary.DiagnosticEvent} - Represents a diagnostic event</li>
 *   <li>{@link com.ibm.ws.diagnostics.summary.DiagnosticReport} - Represents a comprehensive diagnostic report</li>
 *   <li>{@link com.ibm.ws.diagnostics.summary.DiagnosticSummaryService} - Main service interface</li>
 *   <li>{@link com.ibm.ws.diagnostics.summary.DiagnosticSummaryConfig} - Configuration interface</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic detection of exceptions, FFDC events, health check failures</li>
 *   <li>Root cause analysis with confidence scoring</li>
 *   <li>Trace string recommendations for L2 support</li>
 *   <li>Dynamic content fetching from IBM Support, GitHub, and documentation</li>
 *   <li>PDF report generation with charts and visualizations</li>
 *   <li>Email delivery of diagnostic reports</li>
 *   <li>Immediate, scheduled, and manual trigger modes</li>
 * </ul>
 *
 * @since 1.0
 */
package com.ibm.ws.diagnostics.summary;

// Made with Bob
