package com.ttclub.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;

/**
 * Very small disk-based file storage.<br>
 * Files are created under <code>${uploads.dir}</code> (default: “uploads/”)
 * and served statically via WebMvc.
 */
@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${uploads.dir:uploads}") String dir)
            throws IOException {

        this.root = Paths.get(dir).toAbsolutePath();
        Files.createDirectories(root);
    }

    /** Saves the file and returns a public URL “/uploads   /&lt;uuid&gt;_&lt;name&gt;” */
    public String store(MultipartFile in) {
        try {
            String fname = UUID.randomUUID() + "_" +
                    StringUtils.cleanPath(Objects.requireNonNull(in.getOriginalFilename()));

            Path dst = root.resolve(fname);
            in.transferTo(dst);

            return "/uploads/" + fname;
        } catch (IOException ex) {
            throw new RuntimeException("Unable to save file", ex);
        }
    }

    /** Silently removes a previously stored file, if still present. */
    public void delete(String url) {
        if (url == null || !url.startsWith("/uploads/")) return;

        String fname = url.substring("/uploads/".length());
        Path target  = root.resolve(fname);

        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            /* best-effort: log and continue */
            System.err.println("[FileStorage] Failed to delete " + target + ": " + ex.getMessage());
        }
    }
}
