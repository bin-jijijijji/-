package com.thesis.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "recognition_jobs")
public class RecognitionJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "video_asset_id", nullable = false)
    private VideoAssetEntity videoAsset;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "roi_rectangle_id", nullable = false)
    private RoiRectangleEntity roiRectangle;

    @Column(nullable = false)
    private double threshold;

    @Column(nullable = false, length = 20)
    private String status; // PENDING / RUNNING / FINISHED / FAILED

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null || status.isEmpty()) status = "PENDING";
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

    public RoiRectangleEntity getRoiRectangle() {
        return roiRectangle;
    }

    public void setRoiRectangle(RoiRectangleEntity roiRectangle) {
        this.roiRectangle = roiRectangle;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}

