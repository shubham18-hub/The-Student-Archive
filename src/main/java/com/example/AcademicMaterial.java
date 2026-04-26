package com.example;

import java.time.LocalDateTime;
import jakarta.persistence.*;

// Maps to the "academic_materials" table in PostgreSQL
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
    private String filePath;

    // SHA-256 hash used to skip duplicate files on re-index
    @Column(name = "content_hash", nullable = false, unique = true)
    private String contentHash;

    // PostgreSQL tsvector column for full-text search
    @Column(name = "document_vector", columnDefinition = "tsvector")
    private String documentVector;

    @Column(name = "created_at", insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public String getDocumentVector() { return documentVector; }
    public void setDocumentVector(String documentVector) { this.documentVector = documentVector; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
