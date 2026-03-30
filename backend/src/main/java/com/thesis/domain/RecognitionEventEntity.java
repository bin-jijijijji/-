package com.thesis.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "recognition_events")
public class RecognitionEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recognition_job_id", nullable = false)
    private RecognitionJobEntity job;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // blacklist_match / whitelist_match

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "face_identity_id")
    private FaceIdentityEntity faceIdentity;

    @Column(name = "matched_name", length = 120)
    private String matchedName;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "frame_index", nullable = false)
    private int frameIndex;

    @Column(name = "timestamp_ms", nullable = false)
    private long timestampMs;

    @Column(name = "snapshot_path", length = 500)
    private String snapshotPath;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public RecognitionJobEntity getJob() {
        return job;
    }

    public void setJob(RecognitionJobEntity job) {
        this.job = job;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public FaceIdentityEntity getFaceIdentity() {
        return faceIdentity;
    }

    public void setFaceIdentity(FaceIdentityEntity faceIdentity) {
        this.faceIdentity = faceIdentity;
    }

    public String getMatchedName() {
        return matchedName;
    }

    public void setMatchedName(String matchedName) {
        this.matchedName = matchedName;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(int frameIndex) {
        this.frameIndex = frameIndex;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public void setTimestampMs(long timestampMs) {
        this.timestampMs = timestampMs;
    }

    public String getSnapshotPath() {
        return snapshotPath;
    }

    public void setSnapshotPath(String snapshotPath) {
        this.snapshotPath = snapshotPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

