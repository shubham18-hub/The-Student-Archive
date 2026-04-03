package com.example;

import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

// CommandLineRunner is a Spring Boot interface.
// Any class that implements it will have its run() method called
// automatically when the application starts — after everything is ready.
// This is how we automatically index all PDFs on startup.
@Service
public class PDFToDatabase implements CommandLineRunner {

    // Spring automatically injects JdbcTemplate — we never call new JdbcTemplate()
    // JdbcTemplate is Spring's helper for running SQL queries safely
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // @Value reads the value from application.properties
    // The part after : is the default value if the property is not set
    @Value("${file.indexing.path:D:/my-pdf-db/JAVA DATABASE}")
    private String rootPath;

    // This method runs once when the app starts
    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting PDF ingestion from: " + rootPath);

        File rootDir = new File(rootPath);

        // Check if the folder exists
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.out.println("Folder not found: " + rootPath);
            return;
        }

        // Loop through each department folder (B TECH, M TECH, BBA, etc.)
        File[] deptFolders = rootDir.listFiles(File::isDirectory);
        if (deptFolders != null) {
            for (File folder : deptFolders) {
                processDepartment(folder, folder.getName());
            }
        }

        System.out.println("All PDFs processed.");
    }

    // Recursively goes through all subfolders to find PDF files
    private void processDepartment(File folder, String departmentName) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Go deeper into subfolders
                processDepartment(file, departmentName);
            } else if (file.getName().toLowerCase().endsWith(".pdf")) {
                // Found a PDF — process it
                ingestPdf(file, departmentName);
            }
        }
    }

    // Reads one PDF file, extracts text, and saves to database (skips duplicates via content_hash)
    private void ingestPdf(File file, String department) {
        try {
            // Compute SHA-256 hash of the file bytes to detect duplicates
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            String contentHash = sb.toString();

            // Skip if this exact file was already indexed
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_materials WHERE content_hash = ?",
                Integer.class, contentHash);
            if (count != null && count > 0) {
                System.out.println("  Skipped (duplicate): " + file.getName());
                return;
            }

            // Extract text from the PDF using Apache PDFBox
            String extractedText = "";
            try (PDDocument document = PDDocument.load(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String rawText = stripper.getText(document);
                extractedText = (rawText != null) ? rawText.replace("\0", " ") : "";
            }

            String title = file.getName().replace(".pdf", "");
            // Limit text to 10000 characters — PostgreSQL GIN index has a size limit
            String safeText = extractedText.substring(0, Math.min(extractedText.length(), 10000));

            String sql = "INSERT INTO academic_materials (title, department, file_path, content_hash, document_vector) "
                       + "VALUES (?, ?, ?, ?, to_tsvector('english', CAST(? AS TEXT)))";

            jdbcTemplate.update(sql, title, department, file.getAbsolutePath(), contentHash, safeText);
            System.out.println("  Added: " + title);

        } catch (Exception e) {
            System.out.println("  Error with file " + file.getName() + ": " + e.getMessage());
        }
    }
}
