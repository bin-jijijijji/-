package com.thesis.api;

public class RecognitionEventCreateRequest {
    private Long jobId;
    private String eventType; // blacklist_match / whitelist_match
    private Long faceIdentityId; // optional
    private String matchedName;
    private double score;
    private int frameIndex;
    private long timestampMs;
    private String snapshotPath; // relative path or filename under snapshot dir

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getFaceIdentityId() {
        return faceIdentityId;
    }

    public void setFaceIdentityId(Long faceIdentityId) {
        this.faceIdentityId = faceIdentityId;
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
}

