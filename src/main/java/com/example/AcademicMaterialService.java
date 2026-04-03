package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// @Service marks this as a business logic class

@Service
public class AcademicMaterialService {

    @Autowired
    private AcademicMaterialRepository repository;

    // Renames a single file on disk and updates the path in the database
    public void renameFile(Long id, String newName) throws IOException {
        
        // findById returns Optional<AcademicMaterial>
        // orElseThrow means: if not found, throw an error instead of returning null
        AcademicMaterial material = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Material not found with id: " + id));

        // Java NIO (New I/O) — modern way to work with files
        Path source = Paths.get(material.getFilePath()); // current file location
        Path target = source.resolveSibling(newName);    // same folder, new name

        // Move/rename the file on disk
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

        // Update the path in the database to match the new file location
        material.setFilePath(target.toString());
        repository.save(material); // save() does UPDATE if the record already exists
    }

    // Renames all files — removes special characters from filenames
    public void renameAllFiles() throws IOException {
        for (AcademicMaterial material : repository.findAll()) {
            Path path = Paths.get(material.getFilePath());
            String originalName = path.getFileName().toString();

            // Replace any character that is not a letter, number, dot, dash, or underscore
            String cleanName = originalName.replaceAll("[^a-zA-Z0-9._\\-]", "_");

            if (!cleanName.equals(originalName)) {
                Path newPath = path.resolveSibling(cleanName);
                Files.move(path, newPath, StandardCopyOption.REPLACE_EXISTING);
                material.setFilePath(newPath.toString());
                repository.save(material);
                System.out.println("Renamed: " + originalName + " -> " + cleanName);
            }
        }
    }
}
