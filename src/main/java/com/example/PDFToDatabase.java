package com.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.regex.*;

import com.example.nlp.NLPQueryProcessor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Runs on startup — walks the PDF folder and indexes everything into the database.
 *
 * TWO-STRATEGY INDEXING:
 *
 *   Strategy A — Text-based PDF (normal PDF with embedded text)
 *     PDFTextStripper extracts the text directly.
 *     Indexed content = extracted text (capped at 10,000 chars).
 *
 *   Strategy B — Scanned PDF (image-only, no embedded text)
 *     PDFTextStripper returns blank or near-blank.
 *     Raw metadata is built from filename + folder path, then passed through
 *     NLPQueryProcessor.enrichMetadata() which applies:
 *       - Stopword removal
 *       - Suffix stemming
 *       - Full synonym expansion (degree names, semester numbers, exam types, subjects)
 *     Result: a rich NLP-enriched word list indexed via to_tsvector().
 *
 *     Example:
 *       File:   "1ST SEM END B.C.A. 2018.pdf"
 *       Folder: "BCA BSC IT MCA MSC IT / 1ST SEMESTER / END TERM / 2018"
 *       NLP output: "2018 1st sem end bca bsc it mca msc semester term first one
 *                    bachelor computer application master science information technology
 *                    endterm final annual"
 *
 *     This makes the paper findable by:
 *       "bca 2018", "bachelor computer application", "first semester end term",
 *       "end term 2018 bca", "mca 1st sem" — all return this paper.
 */
@Service
public class PDFToDatabase implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${file.indexing.path:D:/my-pdf-db/JAVA DATABASE}")
    private String rootPath;

    // A PDF is considered "scanned / image-based" if extracted text is shorter than this
    private static final int TEXT_THRESHOLD = 50;

    // A PDF with ZERO meaningful text (only whitespace/newlines) is a pure image PDF → delete
    // A PDF with some text (1–49 chars) is a scanned PDF with partial structure → NLP index
    private static final int IMAGE_THRESHOLD = 1;

    // counters for the summary report
    private int countText    = 0;
    private int countScanned = 0;
    private int countDeleted = 0;
    private int countSkipped = 0;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting PDF ingestion from: " + rootPath);

        File rootDir = new File(rootPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.out.println("Folder not found: " + rootPath);
            return;
        }

        File[] deptFolders = rootDir.listFiles(File::isDirectory);
        if (deptFolders != null) {
            for (File folder : deptFolders) {
                processDepartment(folder, folder.getName());
            }
        }

        System.out.println("\n══════════════════════════════════════════");
        System.out.println("  PDF Ingestion Complete");
        System.out.println("  Text-based indexed : " + countText);
        System.out.println("  Scanned NLP indexed: " + countScanned);
        System.out.println("  Pure image DELETED : " + countDeleted);
        System.out.println("  Duplicates skipped : " + countSkipped);
        System.out.println("══════════════════════════════════════════\n");
    }

    // Walks subfolders recursively to find PDFs
    private void processDepartment(File folder, String departmentName) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                processDepartment(file, departmentName);
            } else if (file.getName().toLowerCase().endsWith(".pdf")) {
                ingestPdf(file, departmentName);
            }
        }
    }

    private void ingestPdf(File file, String department) {
        try {
            // ── 1. SHA-256 duplicate check ────────────────────────────────────
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            String contentHash = sb.toString();

            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_materials WHERE content_hash = ?",
                Integer.class, contentHash);
            if (count != null && count > 0) {
                System.out.println("  Skipped (duplicate): " + file.getName());
                countSkipped++;
                return;
            }

            // ── 2. Try to extract embedded text ──────────────────────────────
            String extractedText = "";
            try (PDDocument document = PDDocument.load(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String rawText = stripper.getText(document);
                extractedText = (rawText != null) ? rawText.replace("\0", " ").trim() : "";
            }

            String title = file.getName().replace(".pdf", "").replace(".PDF", "");
            String indexContent;

            // ── 3. Decide strategy based on extracted text length ─────────────
            if (extractedText.length() >= TEXT_THRESHOLD) {
                // Strategy A: text-based PDF — use extracted content directly
                indexContent = extractedText.substring(0, Math.min(extractedText.length(), 10000));
                System.out.println("  [TEXT]    Added: " + title);
            } else if (isScannedPDF(extractedText)) {
                // Strategy B: scanned PDF (has image pages but some structure) — NLP metadata index
                String rawMeta = buildRawMetadata(file, department);
                indexContent   = NLPQueryProcessor.enrichMetadata(rawMeta);
                System.out.println("  [SCANNED] Added: " + title + " → NLP: " + indexContent.substring(0, Math.min(indexContent.length(), 80)) + "...");
            } else {
                // Strategy C: pure image-based PDF (completely blank, no structure at all) — DELETE
                System.out.println("  [IMAGE]   Deleting (pure image PDF, unindexable): " + file.getAbsolutePath());
                boolean deleted = file.delete();
                if (deleted) {
                    System.out.println("            Deleted successfully.");
                } else {
                    System.out.println("            WARNING: Could not delete file — check permissions.");
                }
                return;  // nothing to index
            }

            // ── 4. Insert into database ───────────────────────────────────────
            String sql = "INSERT INTO academic_materials (title, department, file_path, content_hash, document_vector) "
                       + "VALUES (?, ?, ?, ?, to_tsvector('english', CAST(? AS TEXT)))";

            jdbcTemplate.update(sql, title, department, file.getAbsolutePath(), contentHash, indexContent);

        } catch (IOException e) {
            System.out.println("  IO Error: " + file.getName() + " — " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  Error: " + file.getName() + " — " + e.getMessage());
        }
    }

    /**
     * Distinguishes between two types of "blank" PDFs:
     *
     *   Scanned PDF  — PDFTextStripper returns 1–49 chars (whitespace, page numbers,
     *                  headers from the scan). Has some structure. Worth NLP indexing.
     *
     *   Pure image PDF — PDFTextStripper returns completely empty string after trim.
     *                    Nothing to index at all. Should be deleted.
     *
     * @param extractedText the trimmed text from PDFTextStripper
     * @return true if this is a scanned PDF (index it), false if pure image (delete it)
     */
    private boolean isScannedPDF(String extractedText) {
        // Strip all whitespace and newlines — if anything remains, it's a scanned PDF
        String stripped = extractedText.replaceAll("\\s+", "");
        return stripped.length() >= IMAGE_THRESHOLD;
    }

    /**
     * Builds RAW metadata tokens from the file's name and folder path.
     * No expansion here — NLPQueryProcessor.enrichMetadata() handles that.
     *
     * Example:
     *   File:   "1ST SEM END B.C.A. 2018.pdf"
     *   Folder: "BCA BSC IT MCA MSC IT / 1ST SEMESTER / END TERM / 2018"
     *   Output: "1ST SEM END B C A 2018 2018 END TERM 1ST SEMESTER BCA BSC IT MCA MSC IT
     *            BCA BSC IT MCA MSC IT B COM B SC CS"
     */
    private String buildRawMetadata(File file, String department) {
        StringBuilder meta = new StringBuilder();

        // Filename — clean punctuation to spaces so tokens are separated
        String filename = file.getName()
            .replace(".pdf", "").replace(".PDF", "")
            .replace(".", " ").replace("_", " ").replace("-", " ")
            .replace("(", " ").replace(")", " ");
        meta.append(filename).append(" ");

        // Walk up the folder tree and collect every folder name as tokens
        File parent = file.getParentFile();
        while (parent != null && !parent.getName().equalsIgnoreCase("JAVA DATABASE")) {
            String folderName = parent.getName()
                .replace(".", " ").replace("_", " ").replace("-", " ")
                .replace("(", " ").replace(")", " ");
            meta.append(folderName).append(" ");
            parent = parent.getParentFile();
        }

        // Department name
        meta.append(department).append(" ");

        return meta.toString().trim();
    }
}
