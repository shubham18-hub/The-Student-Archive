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

    // This uses the connection pool we set up in application.properties!
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // CommandLineRunner ensures this runs automatically when Spring Boot starts
    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting bulk PDF ingestion...");
        File rootDir = new File("D:/my-pdf-db/JAVA DATABASE");
        
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.out.println("❌ Could not find directory: D:/my-pdf-db/JAVA DATABASE");
            return;
        }

        // Iterate through Department folders (B TECH, MBA, etc.)
        File[] deptFolders = rootDir.listFiles(File::isDirectory);
        if (deptFolders != null) {
            for (File folder : deptFolders) {
                // Pass the folder AND the folder's name as the root department
                processDepartment(folder, folder.getName()); 
            }
        }
        System.out.println("✅ All departments processed successfully!");
    } // <--- THIS is the curly brace that was missing!

    // Notice how we pass the original department string down through the layers!
    private void processDepartment(File folder, String rootDepartment) {
        File[] files = folder.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // If it finds another folder inside, it calls ITSELF to dig deeper!
                    processDepartment(file, rootDepartment);
                } else if (file.getName().toLowerCase().endsWith(".pdf")) {
                    // If it finds a PDF, it processes it
                    System.out.println("Processing [" + rootDepartment + "]: " + file.getName());
                    ingestPdf(file, rootDepartment);
                }
            }
        }
    }

    private void ingestPdf(File file, String department) {
        try {
            // 1. Calculate SHA-256 Hash to prevent duplicates
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(file);
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            fis.close();
            
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            String contentHash = sb.toString();

            // 2. Extract text using PDFBox
            PDDocument document = PDDocument.load(file);
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);
            document.close();
            
            // Clean null characters for PostgreSQL
            String cleanText = rawText.replace("\0", " ");
            String title = file.getName().replace(".pdf", "");

           // 3. Insert into the optimized PostgreSQL schema
            String sql = "INSERT INTO academic_materials (title, department, file_path, content_hash, document_vector) " +
                         "VALUES (?, ?, ?, ?, to_tsvector('english', CAST(? AS TEXT))) " +
                         "ON CONFLICT (content_hash) DO NOTHING";

            int rowsAffected = jdbcTemplate.update(sql, title, department, file.getAbsolutePath(), contentHash, cleanText);
            
            if (rowsAffected > 0) {
                System.out.println("  -> SUCCESS: Added to database.");
            } else {
                System.out.println("  -> SKIPPED: Duplicate content already exists.");
            }

        } catch (Exception e) {
            System.out.println("  -> ❌ Error processing " + file.getName() + ": " + e.getMessage());
        }
    }
}