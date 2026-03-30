package com.thesis.api;

import com.thesis.domain.CameraEntity;
import com.thesis.domain.VideoAssetEntity;
import com.thesis.repo.CameraRepository;
import com.thesis.repo.VideoAssetRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class VideoAssetsController {

    private final CameraRepository cameraRepository;
    private final VideoAssetRepository videoAssetRepository;

    @Value("${app.uploads.video-dir}")
    private String videoDir;

    public VideoAssetsController(CameraRepository cameraRepository, VideoAssetRepository videoAssetRepository) {
        this.cameraRepository = cameraRepository;
        this.videoAssetRepository = videoAssetRepository;
    }

    public static class VideoAssetView {
        public Long id;
        public String cameraName;
        public String title;
        public String fileUrl;
        public LocalDateTime createdAt;
    }

    @GetMapping("/video-assets")
    public ApiResponse<List<VideoAssetView>> listVideoAssets() {
        List<VideoAssetEntity> assets = videoAssetRepository.findAll();
        List<VideoAssetView> res = assets.stream().map(a -> {
            VideoAssetView v = new VideoAssetView();
            v.id = a.getId();
            v.cameraName = a.getCamera().getName();
            v.title = a.getTitle();
            Path p = Paths.get(a.getFilePath());
            v.fileUrl = "/files/videos/" + p.getFileName().toString();
            v.createdAt = a.getCreatedAt();
            return v;
        }).toList();
        return ApiResponse.ok(res);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/video-assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VideoAssetView>> uploadVideoAsset(
            @RequestParam("cameraName") String cameraName,
            @RequestParam("title") String title,
            @RequestPart("video") MultipartFile video
    ) throws Exception {

        if (video == null || video.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.err("video文件不能为空"));
        }
        if (cameraName == null || cameraName.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.err("cameraName不能为空"));
        }

        String safeTitle = title == null ? "" : title.trim();
        if (safeTitle.isEmpty()) safeTitle = video.getOriginalFilename();

        CameraEntity camera = cameraRepository.findByName(cameraName.trim())
                .orElseGet(() -> {
                    CameraEntity c = new CameraEntity();
                    c.setName(cameraName.trim());
                    c.setDescription("");
                    return cameraRepository.save(c);
                });

        Files.createDirectories(Path.of(videoDir));

        String originalName = StringUtils.hasText(video.getOriginalFilename()) ? video.getOriginalFilename() : "video.mp4";
        String ext = "";
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot >= 0) ext = originalName.substring(lastDot);
        String filename = System.currentTimeMillis() + "_" + camera.getName() + "_" + originalName.replaceAll("\\s+", "_") + ext;
        filename = filename.replaceAll("[^a-zA-Z0-9_\\.-]", "_");

        Path target = Path.of(videoDir, filename);
        video.transferTo(target);

        VideoAssetEntity asset = new VideoAssetEntity();
        asset.setCamera(camera);
        asset.setTitle(safeTitle);
        asset.setFilePath(target.toAbsolutePath().toString());
        asset.setOriginalFilename(originalName);
        videoAssetRepository.save(asset);

        VideoAssetView v = new VideoAssetView();
        v.id = asset.getId();
        v.cameraName = camera.getName();
        v.title = asset.getTitle();
        v.fileUrl = "/files/videos/" + filename;
        v.createdAt = asset.getCreatedAt();
        return ResponseEntity.ok(ApiResponse.ok(v));
    }
}

