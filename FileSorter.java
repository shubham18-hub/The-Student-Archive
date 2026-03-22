package com.example;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileSorter {

    public static void main(String[] args) {
        String sourceDirectory = "D:/my-pdf-db/UNSORTED_FILES"; 
        String targetDirectory = "D:/my-pdf-db/JAVA DATABASE";

        File sourceFolder = new File(sourceDirectory);
        if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
            System.out.println("❌ Could not find the source directory.");
            return;
        }

        File[] files = sourceFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        if (files == null || files.length == 0) {
            System.out.println("No PDFs found in the source directory.");
            return;
        }

        System.out.println("🚀 Starting Advanced Regex File Sorting...");

        for (File file : files) {
            String fileName = file.getName().toUpperCase(); 
            
            String course = determineCourse(fileName);
            String branch = determineBranch(fileName, course);
            String semester = determineSemester(fileName);

            Path targetDirPath = Paths.get(targetDirectory, course, branch, semester);

            try {
                Files.createDirectories(targetDirPath);
                Path sourceFilePath = file.toPath();
                Path targetFilePath = targetDirPath.resolve(file.getName());
                
                Files.move(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✅ Moved: " + file.getName() + " -> " + course + "/" + branch + "/" + semester);

            } catch (Exception e) {
                System.out.println("❌ Failed to move: " + file.getName() + " - " + e.getMessage());
            }
        }
        System.out.println("🎉 Sorting Complete!");
    }

    // --- UPGRADED REGEX METADATA EXTRACTION LOGIC ---

    private static String determineCourse(String fileName) {
        if (fileName.matches(".*\\b(B\\.?TECH|B TECH)\\b.*")) return "B TECH";
        if (fileName.matches(".*\\b(M\\.?TECH|M TECH)\\b.*")) return "M TECH";
        if (fileName.matches(".*\\b(MBA|M\\.?B\\.?A\\.?)\\b.*")) return "MBA";
        if (fileName.matches(".*\\b(BBA|B\\.?B\\.?A\\.?)\\b.*")) return "BBA";
        return "UNKNOWN_COURSE"; 
    }

    private static String determineBranch(String fileName, String course) {
        if (course.equals("B TECH") || course.equals("M TECH")) {
            // Using Regex \b ensures we match exact words or codes, ignoring substrings!
            if (fileName.matches(".*\\b(CS|CSE|COMPUTER|TCS|MCS)\\b.*")) return "CSE";
            if (fileName.matches(".*\\b(CIVIL|CE|TCE|STRUCTURAL|MSE|OCE)\\b.*")) return "CIVIL";
            if (fileName.matches(".*\\b(ME|MECHANICAL|TME|CAD|MME|DEME|ACME|OEME)\\b.*")) return "MECHANICAL";
            if (fileName.matches(".*\\b(EE|ELECTRICAL|TEE|MEV)\\b.*")) return "EE";
            if (fileName.matches(".*\\b(ECE|ELECTRONICS|TEC|VLSI|VDM)\\b.*")) return "ECE";
            if (fileName.matches(".*\\b(PETROLEUM|PE|TPE)\\b.*")) return "PETROLEUM";
            if (fileName.matches(".*\\b(BIOTECH|BT|TBT|BIOMEDICAL|BEM)\\b.*")) return "BIO TECH";
            if (fileName.matches(".*\\b(GEO INFORMATICS|GIS|MGI)\\b.*")) return "GEO INFORMATICS";
        }
        return "GENERAL"; 
    }

    private static String determineSemester(String fileName) {
        if (fileName.matches(".*\\b(1ST|FIRST)\\b.*")) return "1st Semester";
        if (fileName.matches(".*\\b(2ND|SECOND)\\b.*")) return "2nd Semester";
        if (fileName.matches(".*\\b(3RD|THIRD)\\b.*")) return "3rd Semester";
        if (fileName.matches(".*\\b(4TH|FOURTH)\\b.*")) return "4th Semester";
        if (fileName.matches(".*\\b(5TH|FIFTH)\\b.*")) return "5th Semester";
        if (fileName.matches(".*\\b(6TH|SIXTH)\\b.*")) return "6th Semester";
        if (fileName.matches(".*\\b(7TH|SEVENTH)\\b.*")) return "7th Semester";
        if (fileName.matches(".*\\b(8TH|EIGHTH)\\b.*")) return "8th Semester";
        return "All Semesters"; // Renamed from UNKNOWN to look cleaner in your folders!
    }
}