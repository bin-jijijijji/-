package com.thesis.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_assets")
public class VideoAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "camera_id", nullable = false)
    private CameraEntity camera;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public CameraEntity getCamera() {
        return camera;
    }

    public void setCamera(CameraEntity camera) {
        this.camera = camera;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

