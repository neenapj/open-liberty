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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import com.ibm.ws.diagnostics.summary.DiagnosticEvent;

/**
 * Generates charts and visualizations for PDF reports using JFreeChart.
 */
public class ChartGenerator {

    private static final int CHART_WIDTH = 600;
    private static final int CHART_HEIGHT = 400;

    /**
     * Generate severity distribution pie chart
     *
     * @param events list of diagnostic events
     * @return chart image as PNG bytes
     */
    public byte[] generateSeverityChart(List<DiagnosticEvent> events) throws Exception {
        // Count events by severity
        Map<DiagnosticEvent.Severity, Integer> severityCounts = new HashMap<>();
        for (DiagnosticEvent event : events) {
            severityCounts.merge(event.getSeverity(), 1, Integer::sum);
        }

        // Create dataset
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        for (Map.Entry<DiagnosticEvent.Severity, Integer> entry : severityCounts.entrySet()) {
            dataset.setValue(entry.getKey().name(), entry.getValue());
        }

        // Create chart
        JFreeChart chart = ChartFactory.createPieChart(
            "Event Severity Distribution",
            dataset,
            true,  // legend
            true,  // tooltips
            false  // URLs
        );

        // Customize colors
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionPaint("CRITICAL", new Color(204, 0, 0));
        plot.setSectionPaint("HIGH", new Color(255, 102, 0));
        plot.setSectionPaint("MEDIUM", new Color(255, 204, 0));
        plot.setSectionPaint("LOW", new Color(102, 204, 102));
        plot.setSectionPaint("INFO", new Color(153, 153, 153));

        // Convert to PNG bytes
        return chartToBytes(chart);
    }

    /**
     * Convert JFreeChart to PNG bytes
     */
    private byte[] chartToBytes(JFreeChart chart) throws Exception {
        BufferedImage image = chart.createBufferedImage(CHART_WIDTH, CHART_HEIGHT);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}

// Made with Bob
