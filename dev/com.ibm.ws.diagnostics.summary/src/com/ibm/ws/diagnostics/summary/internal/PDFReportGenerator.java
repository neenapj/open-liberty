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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.ibm.ws.diagnostics.summary.DiagnosticEvent;
import com.ibm.ws.diagnostics.summary.DiagnosticReport;
import com.ibm.ws.diagnostics.summary.DiagnosticReport.RootCauseAnalysis;
import com.ibm.ws.diagnostics.summary.DiagnosticReport.TraceRecommendation;

/**
 * Generates PDF reports from diagnostic data using Apache PDFBox.
 * Creates professional, multi-page reports with sections, tables, and charts.
 */
public class PDFReportGenerator {

    private static final Logger logger = Logger.getLogger(PDFReportGenerator.class.getName());

    // Fonts
    private static final PDFont FONT_TITLE = PDType1Font.HELVETICA_BOLD;
    private static final PDFont FONT_HEADING = PDType1Font.HELVETICA_BOLD;
    private static final PDFont FONT_BODY = PDType1Font.HELVETICA;
    private static final PDFont FONT_CODE = PDType1Font.COURIER;

    // Font sizes
    private static final float FONT_SIZE_TITLE = 24;
    private static final float FONT_SIZE_HEADING = 16;
    private static final float FONT_SIZE_SUBHEADING = 12;
    private static final float FONT_SIZE_BODY = 10;
    private static final float FONT_SIZE_CODE = 9;

    // Colors
    private static final Color COLOR_TITLE = new Color(0, 51, 102);
    private static final Color COLOR_HEADING = new Color(0, 102, 204);
    private static final Color COLOR_CRITICAL = new Color(204, 0, 0);
    private static final Color COLOR_HIGH = new Color(255, 102, 0);
    private static final Color COLOR_MEDIUM = new Color(255, 204, 0);
    private static final Color COLOR_LOW = new Color(102, 204, 102);
    private static final Color COLOR_CODE_BG = new Color(245, 245, 245);

    // Layout
    private static final float MARGIN = 50;
    private static final float LINE_SPACING = 1.5f;

    private final ChartGenerator chartGenerator;
    private final String outputDirectory;

    public PDFReportGenerator(String outputDirectory) {
        this.chartGenerator = new ChartGenerator();
        this.outputDirectory = outputDirectory != null ? outputDirectory :
            System.getProperty("server.output.dir", ".") + "/logs/diagnostics";

        // Ensure output directory exists
        new File(this.outputDirectory).mkdirs();
    }

    /**
     * Generate PDF report and return as byte array
     *
     * @param report diagnostic report
     * @return PDF content as bytes
     */
    public byte[] generatePDF(DiagnosticReport report) {
        logger.info("Generating PDF report: " + report.getReportId());

        try (PDDocument document = new PDDocument()) {
            // Add cover page
            addCoverPage(document, report);

            // Add executive summary
            addExecutiveSummary(document, report);

            // Add event statistics with charts
            addEventStatistics(document, report);

            // Add root cause analyses
            addRootCauseAnalyses(document, report);

            // Add trace recommendations
            addTraceRecommendations(document, report);

            // Add recommended actions
            addRecommendedActions(document, report);

            // Convert to bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            byte[] pdfBytes = baos.toByteArray();

            // Also save to file
            savePDFToFile(report, pdfBytes);

            logger.info("PDF report generated successfully: " + pdfBytes.length + " bytes");
            return pdfBytes;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error generating PDF report", e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    /**
     * Add cover page
     */
    private void addCoverPage(PDDocument document, DiagnosticReport report) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float yPosition = page.getMediaBox().getHeight() - 150;

            // Title
            content.setFont(FONT_TITLE, FONT_SIZE_TITLE);
            content.setNonStrokingColor(COLOR_TITLE);
            content.beginText();
            content.newLineAtOffset(MARGIN, yPosition);
            content.showText("Open Liberty");
            content.endText();

            yPosition -= 40;
            content.beginText();
            content.newLineAtOffset(MARGIN, yPosition);
            content.showText("Diagnostic Summary Report");
            content.endText();

            // Report details
            yPosition -= 80;
            content.setFont(FONT_BODY, FONT_SIZE_BODY);
            content.setNonStrokingColor(Color.BLACK);

            String[] details = {
                "Report ID: " + report.getReportId(),
                "Generated: " + formatTimestamp(report.getGeneratedAt()),
                "Server: " + report.getServerName(),
                "Liberty Version: " + report.getLibertyVersion(),
                "Java Version: " + report.getJavaVersion(),
                "Trigger Type: " + report.getTriggerType(),
                "Analysis Window: " + formatTimestamp(report.getAnalysisWindowStart()) +
                    " to " + formatTimestamp(report.getAnalysisWindowEnd())
            };

            for (String detail : details) {
                content.beginText();
                content.newLineAtOffset(MARGIN, yPosition);
                content.showText(detail);
                content.endText();
                yPosition -= 20;
            }

            // Footer
            content.setFont(FONT_BODY, FONT_SIZE_BODY - 2);
            content.setNonStrokingColor(Color.GRAY);
            content.beginText();
            content.newLineAtOffset(MARGIN, 50);
            content.showText("Generated by Open Liberty Diagnostic Summary Feature");
            content.endText();
        }
    }

    /**
     * Add executive summary section
     */
    private void addExecutiveSummary(PDDocument document, DiagnosticReport report) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float yPosition = page.getMediaBox().getHeight() - MARGIN;

            // Section heading
            yPosition = addSectionHeading(content, "Executive Summary", yPosition);
            yPosition -= 20;

            // Summary text
            content.setFont(FONT_BODY, FONT_SIZE_BODY);
            content.setNonStrokingColor(Color.BLACK);

            String summary = report.getExecutiveSummary();
            yPosition = addWrappedText(content, summary, MARGIN, yPosition,
                page.getMediaBox().getWidth() - 2 * MARGIN);
        }
    }

    /**
     * Add event statistics section with charts
     */
    private void addEventStatistics(PDDocument document, DiagnosticReport report) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float yPosition = page.getMediaBox().getHeight() - MARGIN;

            // Section heading
            yPosition = addSectionHeading(content, "Event Statistics", yPosition);
            yPosition -= 20;

            // Statistics
            content.setFont(FONT_BODY, FONT_SIZE_BODY);
            String[] stats = {
                "Total Events: " + report.getEvents().size(),
                "Unique Signatures: " + report.getGroupedEvents().size(),
                "Analysis Period: " + formatDuration(report.getAnalysisWindowStart(),
                    report.getAnalysisWindowEnd())
            };

            for (String stat : stats) {
                content.beginText();
                content.newLineAtOffset(MARGIN, yPosition);
                content.showText(stat);
                content.endText();
                yPosition -= 20;
            }

            yPosition -= 20;

            // Add severity distribution chart if available
            if (!report.getEvents().isEmpty()) {
                try {
                    byte[] chartImage = chartGenerator.generateSeverityChart(report.getEvents());
                    PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, chartImage, "chart");

                    float chartWidth = 400;
                    float chartHeight = 300;
                    content.drawImage(pdImage, MARGIN, yPosition - chartHeight, chartWidth, chartHeight);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Could not generate severity chart", e);
                }
            }
        }
    }

    /**
     * Add root cause analyses section
     */
    private void addRootCauseAnalyses(PDDocument document, DiagnosticReport report) throws IOException {
        List<RootCauseAnalysis> analyses = report.getRootCauseAnalyses();
        if (analyses.isEmpty()) {
            return;
        }

        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float yPosition = page.getMediaBox().getHeight() - MARGIN;

            // Section heading
            yPosition = addSectionHeading(content, "Root Cause Analysis", yPosition);
            yPosition -= 30;

            for (int i = 0; i < analyses.size() && i < 5; i++) {
                RootCauseAnalysis analysis = analyses.get(i);

                // Check if we need a new page
                if (yPosition < 200) {
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    PDPageContentStream newContent = new PDPageContentStream(document, page);
                    yPosition = page.getMediaBox().getHeight() - MARGIN;
                    return; // Will continue on next page
                }

                // Analysis number and root cause
                content.setFont(FONT_HEADING, FONT_SIZE_SUBHEADING);
                content.setNonStrokingColor(COLOR_HEADING);
                content.beginText();
                content.newLineAtOffset(MARGIN, yPosition);
                content.showText((i + 1) + ". " + analysis.getRootCause());
                content.endText();
                yPosition -= 25;

                // Confidence
                content.setFont(FONT_BODY, FONT_SIZE_BODY);
                content.setNonStrokingColor(getConfidenceColor(analysis.getConfidenceScore()));
                content.beginText();
                content.newLineAtOffset(MARGIN + 20, yPosition);
                content.showText("Confidence: " + analysis.getConfidenceScore() + "%");
                content.endText();
                yPosition -= 20;

                // Explanation
                content.setNonStrokingColor(Color.BLACK);
                yPosition = addWrappedText(content, analysis.getExplanation(), MARGIN + 20, yPosition,
                    page.getMediaBox().getWidth() - 2 * MARGIN - 20);
                yPosition -= 15;

                // Suggested fixes
                content.setFont(FONT_BODY, FONT_SIZE_BODY);
                content.beginText();
                content.newLineAtOffset(MARGIN + 20, yPosition);
                content.showText("Suggested Fixes:");
                content.endText();
                yPosition -= 15;

                for (String fix : analysis.getSuggestedFixes()) {
                    content.beginText();
                    content.newLineAtOffset(MARGIN + 40, yPosition);
                    content.showText("• " + fix);
                    content.endText();
                    yPosition -= 15;
                }

                yPosition -= 20;
            }
        }
    }

    /**
     * Add trace recommendations section
     */
    private void addTraceRecommendations(PDDocument document, DiagnosticReport report) throws IOException {
        List<TraceRecommendation> recommendations = report.getTraceRecommendations();
        if (recommendations.isEmpty()) {
            return;
        }

        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float yPosition = page.getMediaBox().getHeight() - MARGIN;

            // Section heading
            yPosition = addSectionHeading(content, "Trace Recommendations", yPosition);
            yPosition -= 30;

            for (int i = 0; i < recommendations.size() && i < 5; i++) {
                TraceRecommendation rec = recommendations.get(i);

                // Check if we need a new page
                if (yPosition < 250) {
                    break; // Stop if not enough space
                }

                // Recommendation number
                content.setFont(FONT_HEADING, FONT_SIZE_SUBHEADING);
                content.setNonStrokingColor(COLOR_HEADING);
                content.beginText();
                content.newLineAtOffset(MARGIN, yPosition);
                content.showText((i + 1) + ". Trace Specification");
                content.endText();
                yPosition -= 25;

                // Trace string (code block)
                yPosition = addCodeBlock(content, rec.getTraceString(), MARGIN + 20, yPosition,
                    page.getMediaBox().getWidth() - 2 * MARGIN - 20);
                yPosition -= 15;

                // Details
                content.setFont(FONT_BODY, FONT_SIZE_BODY);
                content.setNonStrokingColor(Color.BLACK);

                String[] details = {
                    "Confidence: " + rec.getConfidenceScore() + "%",
                    "Log Volume: " + rec.getLogVolumeEstimate(),
                    "Performance Impact: " + rec.getPerformanceImpact(),
                    "Reason: " + rec.getReason()
                };

                for (String detail : details) {
                    content.beginText();
                    content.newLineAtOffset(MARGIN + 20, yPosition);
                    content.showText(detail);
                    content.endText();
                    yPosition -= 15;
                }

                yPosition -= 20;
            }
        }
    }

    /**
     * Add recommended actions section
     */
    private void addRecommendedActions(PDDocument document, DiagnosticReport report) throws IOException {
        List<String> actions = report.getRecommendedActions();
        if (actions.isEmpty()) {
            return;
        }

        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float yPosition = page.getMediaBox().getHeight() - MARGIN;

            // Section heading
            yPosition = addSectionHeading(content, "Recommended Actions", yPosition);
            yPosition -= 30;

            content.setFont(FONT_BODY, FONT_SIZE_BODY);
            content.setNonStrokingColor(Color.BLACK);

            for (int i = 0; i < actions.size(); i++) {
                content.beginText();
                content.newLineAtOffset(MARGIN, yPosition);
                content.showText((i + 1) + ". " + actions.get(i));
                content.endText();
                yPosition -= 20;
            }
        }
    }

    /**
     * Helper: Add section heading
     */
    private float addSectionHeading(PDPageContentStream content, String heading, float yPosition)
            throws IOException {
        content.setFont(FONT_HEADING, FONT_SIZE_HEADING);
        content.setNonStrokingColor(COLOR_HEADING);
        content.beginText();
        content.newLineAtOffset(MARGIN, yPosition);
        content.showText(heading);
        content.endText();
        return yPosition - 30;
    }

    /**
     * Helper: Add wrapped text
     */
    private float addWrappedText(PDPageContentStream content, String text, float x, float y, float width)
            throws IOException {
        content.setFont(FONT_BODY, FONT_SIZE_BODY);
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String testLine = line.length() == 0 ? word : line + " " + word;
            float textWidth = FONT_BODY.getStringWidth(testLine) / 1000 * FONT_SIZE_BODY;

            if (textWidth > width) {
                content.beginText();
                content.newLineAtOffset(x, y);
                content.showText(line.toString());
                content.endText();
                y -= FONT_SIZE_BODY * LINE_SPACING;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }

        if (line.length() > 0) {
            content.beginText();
            content.newLineAtOffset(x, y);
            content.showText(line.toString());
            content.endText();
            y -= FONT_SIZE_BODY * LINE_SPACING;
        }

        return y;
    }

    /**
     * Helper: Add code block
     */
    private float addCodeBlock(PDPageContentStream content, String code, float x, float y, float width)
            throws IOException {
        // Draw background
        content.setNonStrokingColor(COLOR_CODE_BG);
        content.addRect(x - 5, y - 15, width + 10, 25);
        content.fill();

        // Draw text
        content.setFont(FONT_CODE, FONT_SIZE_CODE);
        content.setNonStrokingColor(Color.BLACK);
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(code);
        content.endText();

        return y - 25;
    }

    /**
     * Helper: Get color for confidence score
     */
    private Color getConfidenceColor(int confidence) {
        if (confidence >= 80) return COLOR_LOW;
        if (confidence >= 60) return COLOR_MEDIUM;
        if (confidence >= 40) return COLOR_HIGH;
        return COLOR_CRITICAL;
    }

    /**
     * Helper: Format timestamp
     */
    private String formatTimestamp(java.time.Instant instant) {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .format(instant.atZone(java.time.ZoneId.systemDefault()));
    }

    /**
     * Helper: Format duration
     */
    private String formatDuration(java.time.Instant start, java.time.Instant end) {
        long hours = java.time.Duration.between(start, end).toHours();
        return hours + " hours";
    }

    /**
     * Save PDF to file
     */
    private void savePDFToFile(DiagnosticReport report, byte[] pdfBytes) {
        try {
            String filename = String.format("diagnostic-report-%s-%s.pdf",
                report.getServerName(),
                report.getReportId().substring(0, 8));

            File outputFile = new File(outputDirectory, filename);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(pdfBytes);
            }

            logger.info("PDF report saved to: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not save PDF to file", e);
        }
    }
}

// Made with Bob
