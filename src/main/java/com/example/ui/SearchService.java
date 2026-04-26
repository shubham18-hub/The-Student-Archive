package com.example.ui;

import com.example.nlp.NLPQueryProcessor;

import java.sql.*;
import java.util.*;

/**
 * Handles all DB queries for the Swing UI.
 *
 * searchMaterials() now runs the raw query through NLPQueryProcessor
 * before hitting PostgreSQL, giving synonym expansion + stemming.
 *
 * searchRaw() bypasses NLP — used when the user explicitly wants exact match.
 */
public class SearchService {

    /**
     * NLP-enhanced search.
     * Expands the query with synonyms and stems, then runs to_tsquery.
     */
    public static List<Map<String, String>> searchMaterials(String rawQuery) {
        String nlpQuery = NLPQueryProcessor.process(rawQuery);
        System.out.println(NLPQueryProcessor.explain(rawQuery));
        return runSearch(nlpQuery, rawQuery);
    }

    /**
     * Exact / raw search — bypasses NLP expansion.
     * Falls back to plainto_tsquery for safety.
     */
    public static List<Map<String, String>> searchRaw(String rawQuery) {
        return runSearch(null, rawQuery);
    }

    /**
     * Core search method.
     *
     * @param nlpQuery  pre-processed tsquery string (null = use plainto_tsquery)
     * @param rawQuery  original user input (used as fallback and for logging)
     */
    private static List<Map<String, String>> runSearch(String nlpQuery, String rawQuery) {
        List<Map<String, String>> results = new ArrayList<>();

        // If NLP produced a valid expanded query, use to_tsquery (supports | and &)
        // Otherwise fall back to plainto_tsquery which is more forgiving
        boolean useNlp = nlpQuery != null && !nlpQuery.isBlank();

        String queryExpr = useNlp
            ? "to_tsquery('english', ?)"
            : "plainto_tsquery('english', ?)";

        String sql = "SELECT id, title, department, file_path "
                   + "FROM academic_materials "
                   + "WHERE document_vector @@ " + queryExpr + " "
                   + "ORDER BY ts_rank(document_vector, " + queryExpr + ") DESC "
                   + "LIMIT 100";

        String param = useNlp ? nlpQuery : rawQuery;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, param);
            stmt.setString(2, param);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new HashMap<>();
                    row.put("id",         rs.getString("id"));
                    row.put("title",      rs.getString("title"));
                    row.put("department", rs.getString("department"));
                    row.put("file_path",  rs.getString("file_path"));
                    results.add(row);
                }
            }

            System.out.println("Found " + results.size() + " results for: " + rawQuery);

        } catch (SQLException e) {
            System.out.println("Search failed (NLP query: " + param + "): " + e.getMessage());

            // If NLP-expanded query fails (e.g. bad tsquery syntax), retry with plain query
            if (useNlp) {
                System.out.println("Retrying with plain query...");
                return runSearch(null, rawQuery);
            }
        }

        return results;
    }

    public static List<Map<String, String>> getAllMaterials() {
        List<Map<String, String>> results = new ArrayList<>();

        String sql = "SELECT id, title, department, file_path "
                   + "FROM academic_materials ORDER BY created_at DESC LIMIT 100";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("id",         rs.getString("id"));
                row.put("title",      rs.getString("title"));
                row.put("department", rs.getString("department"));
                row.put("file_path",  rs.getString("file_path"));
                results.add(row);
            }

        } catch (SQLException e) {
            System.out.println("Failed to load materials: " + e.getMessage());
        }

        return results;
    }

    public static int countMaterials() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM academic_materials")) {

            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            System.out.println("Failed to count: " + e.getMessage());
        }
        return 0;
    }
}
