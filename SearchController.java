package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allows web frontends to talk to this API
public class SearchController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/search")
    public List<Map<String, Object>> searchMaterials(@RequestParam String query) {
        
        System.out.println("🔍 New Search Request: " + query);

        // This SQL query uses PostgreSQL's full-text search (@@) and the GIN index we built.
        // It calculates a relevance score using ts_rank and orders the best matches at the top.
        String sql = "SELECT title, department, file_path, " +
                     "ts_rank(document_vector, plainto_tsquery('english', ?)) AS rank_score " +
                     "FROM academic_materials " +
                     "WHERE document_vector @@ plainto_tsquery('english', ?) " +
                     "ORDER BY rank_score DESC " +
                     "LIMIT 10";

        // Execute the query, passing the user's search term into the two '?' placeholders
        return jdbcTemplate.queryForList(sql, query, query);
    }
}