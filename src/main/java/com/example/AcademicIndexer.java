package com.example;
import java.io.File;
import java.sql.*;

public class AcademicIndexer {
    // Database Credentials
    static final String DB_URL = "jdbc:postgresql://localhost:5432/resource_engine";
    static final String USER = "postgres";
    static final String PASS = "123456789";

    // Path inside the Docker container
    static final String DOCKER_ROOT = "/home/academic_files"; 
    // Physical path for Java to scan (since Java is running on Windows)
    static final String WINDOWS_ROOT = "D:\\my-pdf-db\\JAVA DATABASE";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            System.out.println("Connected to Docker Postgres!");
            scanFolder(new File(WINDOWS_ROOT), conn);
            System.out.println("Indexing Complete!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void scanFolder(File root, Connection conn) {
        File[] files = root.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanFolder(file, conn); // Recursive call for subfolders
            } else if (file.getName().toLowerCase().endsWith(".pdf")) {
                saveToDatabase(file, conn);
            }
        }
    }

    private static void saveToDatabase(File file, Connection conn) {
        String sql = "INSERT INTO academic_archive (course, branch, semester, file_name, file_path) VALUES (?, ?, ?, ?, ?) ON CONFLICT (file_path) DO NOTHING";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Logic to extract info from path: D:\my-pdf-db\JAVA DATABASE\B TECH\CSE\Sem 4\file.pdf
            String relativePath = file.getAbsolutePath().replace(WINDOWS_ROOT, "");
            String[] parts = relativePath.split("\\\\"); 
            
            // parts[1] = Course, parts[2] = Branch, parts[3] = Semester
            pstmt.setString(1, parts.length > 1 ? parts[1] : "UNKNOWN");
            pstmt.setString(2, parts.length > 2 ? parts[2] : "GENERAL");
            pstmt.setInt(3, extractSemester(parts.length > 3 ? parts[3] : "0"));
            pstmt.setString(4, file.getName());
            // Path as Docker sees it
            pstmt.setString(5, DOCKER_ROOT + relativePath.replace("\\", "/"));

            pstmt.executeUpdate();
            System.out.println("Indexed: " + file.getName());
        } catch (SQLException e) {
            System.out.println("Error indexing: " + file.getName());
        }
    }

    private static int extractSemester(String semString) {
        // Simple logic to turn "Sem 4" or "4" into an integer
        return Integer.parseInt(semString.replaceAll("[^0-9]", "0").trim());
    }
}