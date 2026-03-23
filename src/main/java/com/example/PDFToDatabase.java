package com.example;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PDFToDatabase implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting bulk PDF ingestion...");
        File rootDir = new File("D:/my-pdf-db/JAVA DATABASE");
        
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.out.println("❌ Could not find directory: D:/my-pdf-db/JAVA DATABASE");
            return;
        }

        File[] deptFolders = rootDir.listFiles(File::isDirectory);
        if (deptFolders != null) {
            for (File folder : deptFolders) {
                processDepartment(folder, folder.getName()); 
            }
        }
        System.out.println("✅ All departments processed successfully!");
        System.out.println("✅ Backend is now staying alive on the configured port.");
    }

    private void processDepartment(File folder, String rootDepartment) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processDepartment(file, rootDepartment);
                } else if (file.getName().toLowerCase().endsWith(".pdf")) {
                    System.out.println("Processing [" + rootDepartment + "]: " + file.getName());
                    ingestPdf(file, rootDepartment);
                }
            }
        }
    }

    private void ingestPdf(File file, String department) {
        try {
            // 1. Calculate SHA-256 Hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] byteArray = new byte[1024];
                int bytesCount;
                while ((bytesCount = fis.read(byteArray)) != -1) {
                    digest.update(byteArray, 0, bytesCount);
                }
            }
            
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            String contentHash = sb.toString();

            // 2. Extract text using PDFBox
            String cleanText = "";
            try (PDDocument document = PDDocument.load(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String rawText = stripper.getText(document);
                cleanText = (rawText != null) ? rawText.replace("\0", " ") : "";
            }
            
            String title = file.getName().replace(".pdf", "");

            // 3. Truncate text to avoid PostgreSQL Index Row Limit (8191 bytes)
            String safeText = cleanText.substring(0, Math.min(cleanText.length(), 10000));

            // 4. Insert into Database
            String sql = "INSERT INTO academic_materials (title, department, file_path, content_hash, document_vector) " +
                         "VALUES (?, ?, ?, ?, to_tsvector('english', CAST(? AS TEXT))) " +
                         "ON CONFLICT (content_hash) DO NOTHING";

            int rowsAffected = jdbcTemplate.update(sql, title, department, file.getAbsolutePath(), contentHash, safeText);
            
            if (rowsAffected > 0) {
                System.out.println("   -> SUCCESS: Added to database.");
            } else {
                System.out.println("   -> SKIPPED: Duplicate content already exists.");
            }

        } catch (Exception e) {
            System.out.println("   -> ❌ Error processing " + file.getName() + ": " + e.getMessage());
        }
    }
} // Final closing brace for the class