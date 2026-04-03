package com.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// This interface gives us free database operations without writing any SQL.
// By extending JpaRepository, Spring automatically creates these methods for us:
//   save(material)       → INSERT or UPDATE
//   findById(id)         → SELECT WHERE id = ?
//   findAll()            → SELECT * FROM academic_materials

// We don't write any implementation — Spring generates it at startup.
// <AcademicMaterial, Long> means: entity type is AcademicMaterial, primary key type is Long
@Repository
public interface AcademicMaterialRepository extends JpaRepository<AcademicMaterial, Long> {
    //  operations come from JpaRepository using interface 
}
