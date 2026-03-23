package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Service
public class AcademicMaterialService {

    @Autowired
    private AcademicMaterialRepository repository;

   public String renameFilesBySubject() {
    List<AcademicMaterial> materials = repository.findAll();
    int successCount = 0;
    int failCount = 0;

    for (AcademicMaterial item : materials) {
        // 1. Convert DB path to a normalized Java Path
        String dbPath = item.getFilePath().replace("\\", "/"); 
        Path sourcePath = Paths.get(dbPath).toAbsolutePath().normalize();

        // 2. Debug Log: See what Java is actually looking for
        System.out.println("🔍 Checking path: " + sourcePath.toString());

        if (Files.exists(sourcePath)) {
            try {
                String cleanTitle = item.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
                String newFileName = cleanTitle + "_" + item.getId() + ".pdf";
                Path targetPath = sourcePath.resolveSibling(newFileName);

                Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

                // Update DB with the new normalized path
                item.setFilePath(targetPath.toString().replace("\\", "/"));
                repository.save(item);
                successCount++;
            } catch (IOException e) {
                System.err.println("❌ Move failed: " + e.getMessage());
                failCount++;
            }
        } else {
            System.err.println("⚠️ Still Not Found: " + sourcePath.toString());
            failCount++;
        }
    }
    return "Result -> Success: " + successCount + " | Failed: " + failCount;
}
}