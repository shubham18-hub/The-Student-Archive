package com.example.ui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SearchService handles all database queries for the Swing desktop UI.
public class SearchService {

    // Searches for academic materials matching the search term'
    public static List<Map<String, String>> searchMaterials(String searchTerm) {
        // We store results as a List of Maps
        // Each Map represents one row: {"id"="1", "title"="Civil Exam", "department"="B TECH"}
        List<Map<String, String>> results = new ArrayList<>();

        // plainto_tsquery converts "civil engineering" into a search query
        // document_vector @@ query checks if the document matches
        // ts_rank gives a relevance score — higher means more relevant
        String sql = "SELECT id, title, department, file_path "
                   + "FROM academic_materials "
                   + "WHERE document_vector @@ plainto_tsquery('english', ?) "
                   + "ORDER BY ts_rank(document_vector, plainto_tsquery('english', ?)) DESC "
                   + "LIMIT 50";

        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Set the ? placeholders — this prevents SQL injection attacks
            // PreparedStatement treats it as plain text, not SQL code
            stmt.setString(1, searchTerm);
            stmt.setString(2, searchTerm);

            ResultSet rs = stmt.executeQuery();

            // Loop through each result row
            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("id", rs.getString("id"));
                row.put("title", rs.getString("title"));
                row.put("department", rs.getString("department"));
                row.put("file_path", rs.getString("file_path"));
                results.add(row);
            }
            rs.close();

            System.out.println("Search found " + results.size() + " results for: " + searchTerm);

        } catch (SQLException e) {
            System.out.println("Search failed: " + e.getMessage());
        }

        return results;
    }

    // Loads the  recently added materials 
    public static List<Map<String, String>> getAllMaterials() {
        List<Map<String, String>> results = new ArrayList<>();

        String sql = "SELECT id, title, department, file_path "
                   + "FROM academic_materials "
                   + "ORDER BY created_at DESC "
                   + "LIMIT 100";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("id", rs.getString("id"));
                row.put("title", rs.getString("title"));
                row.put("department", rs.getString("department"));
                row.put("file_path", rs.getString("file_path"));
                results.add(row);
            }

        } catch (SQLException e) {
            System.out.println("Failed to load materials: " + e.getMessage());
        }

        return results;
    }

    // Returns the total number of PDFs stored in the database
    // Shown in the status bar at the bottom of the Swing window
    public static int countMaterials() {
        String sql = "SELECT COUNT(*) AS total FROM academic_materials";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            System.out.println("Failed to count materials: " + e.getMessage());
        }

        return 0;
    }


}
