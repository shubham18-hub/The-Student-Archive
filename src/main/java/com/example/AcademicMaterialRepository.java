package com.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Spring auto-generates save, findById, findAll, delete, etc. at runtime
@Repository
public interface AcademicMaterialRepository extends JpaRepository<AcademicMaterial, Long> {
}
