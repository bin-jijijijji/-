package com.thesis.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class FileServeController {

    @Value("${app.uploads.snapshot-dir}")
    private String snapshotDir;

    @Value("${app.uploads.video-dir}")
    private String videoDir;

    @GetMapping("/files/snapshots/{filename:.+}")
    public ResponseEntity<Resource> serveSnapshot(@PathVariable String filename) throws MalformedURLException {
        return serveFromDir(snapshotDir, filename);
    }

    @GetMapping("/files/videos/{filename:.+}")
    public ResponseEntity<Resource> serveVideo(@PathVariable String filename) throws MalformedURLException {
        return serveFromDir(videoDir, filename);
    }

    private ResponseEntity<Resource> serveFromDir(String dir, String filename) throws MalformedURLException {
        if (filename == null || filename.isEmpty()) return ResponseEntity.notFound().build();

        Path base = Path.of(dir).toAbsolutePath().normalize();
        Path target = base.resolve(filename).normalize();
        if (!target.startsWith(base)) {
            return ResponseEntity.status(403).build();
        }
        if (!Files.exists(target) || Files.isDirectory(target)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(target.toUri());
        String contentType = null;
        try {
            contentType = Files.probeContentType(target);
        } catch (Exception ignored) {
            // fall back to octet-stream
        }
        MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(resource);
    }
}

