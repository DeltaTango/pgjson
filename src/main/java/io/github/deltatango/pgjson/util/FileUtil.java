package io.github.deltatango.pgjson.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Slf4j
public class FileUtil {

    /** Maximum file size allowed for reading (10 MB). */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Validates and normalizes a file path to prevent path traversal attacks.
     *
     * @param fileName the file path to validate
     * @return the normalized, canonical Path
     * @throws IllegalArgumentException if the path is null, empty, or contains traversal sequences
     * @throws IOException if the path cannot be resolved
     */
    private Path validateAndNormalizePath(String fileName) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("File name validation failed: null or empty");
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        Path path = Paths.get(fileName).normalize().toAbsolutePath();

        // Check for path traversal: the normalized path should not escape the intended scope
        // The path should not contain ".." after normalization
        if (path.toString().contains("..")) {
            log.warn("Path traversal detected in file name: {}", fileName);
            throw new IllegalArgumentException("Path traversal detected in file name: " + fileName);
        }

        // Validate file size
        if (Files.exists(path)) {
            long fileSize = Files.size(path);
            if (fileSize > MAX_FILE_SIZE) {
                log.warn("File size {} exceeds maximum allowed size {} bytes: {}", fileSize, MAX_FILE_SIZE, path);
                throw new IllegalArgumentException(
                        "File size exceeds maximum allowed size (" + MAX_FILE_SIZE + " bytes): " + fileSize);
            }
        }

        return path;
    }

    public String readBytesAsStringFromFile(String fileName) throws IOException {
        Path path = validateAndNormalizePath(fileName);
        byte[] bytes = Files.readAllBytes(path);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public String readStringFromFile(String fileName) throws IOException {
        Path path = validateAndNormalizePath(fileName);
        return new String(Files.readAllBytes(path));
    }
}
