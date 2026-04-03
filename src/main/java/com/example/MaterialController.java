package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Admin endpoint — only for management operations

@RestController
@RequestMapping("/api/admin")
public class MaterialController {

    @Autowired
    private AcademicMaterialService service;

    @PostMapping("/rename-all")
    public ResponseEntity<String> renameAll() {
        try {
            service.renameAllFiles();
            return ResponseEntity.ok("All files renamed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
