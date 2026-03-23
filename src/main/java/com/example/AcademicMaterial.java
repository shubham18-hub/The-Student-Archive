package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "academic_materials")
public class AcademicMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String department;

    @Column(name = "file_path", nullable = false)
    @JsonProperty("file_path") // FIX: Maps Java filePath to HTML file_path
    private String filePath;

    @Column(name = "content_hash", nullable = false, unique = true)
    private String contentHash;

    @Column(name = "document_vector", columnDefinition = "tsvector")
    private String documentVector;

    @Column(name = "created_at", insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    // Standard Getters and Setters (Keep your existing ones)
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    // ... include other getters/setters as you had them
}