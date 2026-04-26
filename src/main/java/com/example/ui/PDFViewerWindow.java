package com.example.ui;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Embedded PDF viewer window.
 *
 * Renders each PDF page as a BufferedImage using PDFBox PDFRenderer.
 * No external application needed — runs entirely inside the JVM.
 *
 * Controls:
 *   ← Prev / Next →   navigate pages
 *   Zoom In / Out      scale the rendered image
 *   Page indicator     shows "Page X of Y"
 */
public class PDFViewerWindow extends JFrame {

    private PDDocument document;
    private PDFRenderer renderer;
    private int currentPage = 0;
    private int totalPages  = 0;
    private float zoom      = 1.5f;   // default DPI scale (96 * 1.5 = 144 DPI)

    private JLabel pageLabel;
    private JLabel imageLabel;
    private JScrollPane scrollPane;
    private JButton prevButton;
    private JButton nextButton;
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JLabel statusLabel;

    public PDFViewerWindow(String filePath) {
        setTitle("PDF Viewer — " + new File(filePath).getName());
        setSize(900, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        setVisible(true);
        loadPDF(filePath);
    }

    private void buildUI() {
        setLayout(new BorderLayout(5, 5));
        getContentPane().setBackground(new Color(60, 60, 60));

        // ── Toolbar ──────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        toolbar.setBackground(new Color(45, 45, 45));

        prevButton = makeButton("◀  Prev", new Color(70, 130, 180));
        prevButton.addActionListener(e -> navigate(-1));

        nextButton = makeButton("Next  ▶", new Color(70, 130, 180));
        nextButton.addActionListener(e -> navigate(1));

        zoomInButton  = makeButton("🔍 +", new Color(46, 139, 87));
        zoomInButton.addActionListener(e -> changeZoom(0.25f));

        zoomOutButton = makeButton("🔍 −", new Color(139, 90, 43));
        zoomOutButton.addActionListener(e -> changeZoom(-0.25f));

        pageLabel = new JLabel("Page — of —");
        pageLabel.setForeground(Color.WHITE);
        pageLabel.setFont(new Font("Arial", Font.BOLD, 13));

        toolbar.add(prevButton);
        toolbar.add(pageLabel);
        toolbar.add(nextButton);
        toolbar.add(Box.createHorizontalStrut(20));
        toolbar.add(zoomOutButton);
        toolbar.add(zoomInButton);
        add(toolbar, BorderLayout.NORTH);

        // ── Page display ─────────────────────────────────────────────────────
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(60, 60, 60));
        centerPanel.add(imageLabel, BorderLayout.CENTER);

        scrollPane = new JScrollPane(centerPanel);
        scrollPane.setBackground(new Color(60, 60, 60));
        scrollPane.getViewport().setBackground(new Color(60, 60, 60));
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // ── Status bar ───────────────────────────────────────────────────────
        statusLabel = new JLabel("Loading...");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusLabel.setBorder(new EmptyBorder(4, 10, 4, 10));
        statusLabel.setForeground(new Color(180, 180, 180));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(45, 45, 45));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(110, 32));
        return btn;
    }

    private void loadPDF(String filePath) {
        statusLabel.setText("Loading PDF...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    document   = PDDocument.load(new File(filePath));
                    renderer   = new PDFRenderer(document);
                    totalPages = document.getNumberOfPages();
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(PDFViewerWindow.this,
                            "Failed to load PDF:\n" + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }

            @Override
            protected void done() {
                if (document != null) {
                    currentPage = 0;
                    renderPage();
                }
            }
        }.execute();
    }

    private void renderPage() {
        if (document == null || renderer == null) return;

        statusLabel.setText("Rendering page " + (currentPage + 1) + " of " + totalPages + "...");
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);

        final int pageToRender = currentPage;
        final float currentZoom = zoom;

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                // PDFBox renders at 72 DPI by default; zoom scales it up
                return renderer.renderImage(pageToRender, currentZoom);
            }

            @Override
            protected void done() {
                try {
                    BufferedImage img = get();
                    imageLabel.setIcon(new ImageIcon(img));
                    pageLabel.setText("Page " + (currentPage + 1) + " of " + totalPages);
                    statusLabel.setText("Page " + (currentPage + 1) + " of " + totalPages
                        + "  |  Zoom: " + (int)(zoom * 100) + "%");
                    prevButton.setEnabled(currentPage > 0);
                    nextButton.setEnabled(currentPage < totalPages - 1);
                    // Scroll back to top when page changes
                    SwingUtilities.invokeLater(() ->
                        scrollPane.getVerticalScrollBar().setValue(0));
                } catch (Exception e) {
                    statusLabel.setText("Render error: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void navigate(int delta) {
        int next = currentPage + delta;
        if (next >= 0 && next < totalPages) {
            currentPage = next;
            renderPage();
        }
    }

    private void changeZoom(float delta) {
        float newZoom = zoom + delta;
        if (newZoom < 0.5f || newZoom > 4.0f) return;
        zoom = newZoom;
        renderPage();
    }

    @Override
    public void dispose() {
        // Always close the PDDocument to release file handles
        if (document != null) {
            try { document.close(); } catch (IOException ignored) {}
        }
        super.dispose();
    }
}
