package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // FIX: Prevents UI from being blocked
public class MaterialController {

    @Autowired
    private AcademicMaterialService materialService;

    // Admin Task: Run this once to rename everything
    @GetMapping("/admin/rename-all")
    public ResponseEntity<String> renameAll() {
        String result = materialService.renameFilesBySubject();
        return ResponseEntity.ok(result);
    }
}