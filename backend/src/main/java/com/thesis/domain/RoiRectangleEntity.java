package com.thesis.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "roi_rectangles")
public class RoiRectangleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "video_asset_id", nullable = false)
    private VideoAssetEntity videoAsset;

    @Column(nullable = false, length = 100)
    private String name;

    // Normalized coordinates [0,1]
    @Column(name = "x1", nullable = false)
    private Double x1;
    @Column(name = "y1", nullable = false)
    private Double y1;
    @Column(name = "x2", nullable = false)
    private Double x2;
    @Column(name = "y2", nullable = false)
    private Double y2;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public VideoAssetEntity getVideoAsset() {
        return videoAsset;
    }

    public void setVideoAsset(VideoAssetEntity videoAsset) {
        this.videoAsset = videoAsset;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getX1() {
        return x1;
    }

    public void setX1(Double x1) {
        this.x1 = x1;
    }

    public Double getY1() {
        return y1;
    }

    public void setY1(Double y1) {
        this.y1 = y1;
    }

    public Double getX2() {
        return x2;
    }

    public void setX2(Double x2) {
        this.x2 = x2;
    }

    public Double getY2() {
        return y2;
    }

    public void setY2(Double y2) {
        this.y2 = y2;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

