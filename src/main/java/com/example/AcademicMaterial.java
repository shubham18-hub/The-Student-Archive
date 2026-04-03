package com.example;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// @Entity tells JPA (Java Persistence API) that this class maps to a database table.

@Entity
@Table(name = "academic_materials") // maps to the table named "academic_materials" in PostgreSQL
public class AcademicMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto-increment: 1, 2, 3, 4...
    private Long id;

    @Column(nullable = false) // this column cannot be empty/null in the database
    private String title;

    @Column(nullable = false)
    private String department;

    // The Java variable is called "filePath" (camelCase)
    // but the database column is called "file_path" (snake_case)
    // @JsonProperty makes the JSON output use "file_path" so JavaScript can read it
    @Column(name = "file_path", nullable = false)
    @JsonProperty("file_path")
    private String filePath;

    // SHA-256 hash of the PDF content — used to detect duplicate files
    // unique = true means no two rows can have the same hash
    @Column(name = "content_hash", nullable = false, unique = true)
    private String contentHash;

    // tsvector is a special PostgreSQL type for full-text search
    // It stores the words from the PDF in a searchable format
    @Column(name = "document_vector", columnDefinition = "tsvector")
    private String documentVector;

    // This column is set automatically by the database when a row is inserted
    // insertable = false means we never set it manually
    @Column(name = "created_at", insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    // Getters and Setters — standard Java way to read/write private fields
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
