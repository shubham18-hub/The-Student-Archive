package com.example;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// @RestController means this class handles HTTP requests and returns JSON responses
// @RequestMapping("/api") means all URLs in this class start with /api
// @CrossOrigin(origins = "*") allows any website or app to call this API
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SearchController {

    // JdbcTemplate is Spring's tool for running SQL queries
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    
    // @RequestParam String query reads the "query" value from the URL
    @GetMapping("/search")
    public List<Map<String, Object>> searchMaterials(@RequestParam String query) {
        System.out.println("Search request received: " + query);

        // PostgreSQL full-text search query:
        // plainto_tsquery() converts the user's search text into a search query
        // document_vector @@ plainto_tsquery() checks if the document matches the query
  
        String sql = "SELECT title, department, file_path, "
                   + "ts_rank(document_vector, plainto_tsquery('english', ?)) AS rank_score "
                   + "FROM academic_materials "
                   + "WHERE document_vector @@ plainto_tsquery('english', ?) "
                   + "ORDER BY rank_score DESC "
                   + "LIMIT 50";

        // queryForList runs the SQL and returns each row as a Map<columnName, value>
        // Jackson (JSON library) automatically converts this List to JSON
        // The two 'query' arguments fill the two ? placeholders in the SQL
        return jdbcTemplate.queryForList(sql, query, query);
    }
}
