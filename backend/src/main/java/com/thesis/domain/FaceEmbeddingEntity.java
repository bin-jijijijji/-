package com.thesis.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "face_embeddings")
public class FaceEmbeddingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "face_identity_id", nullable = false)
    private FaceIdentityEntity faceIdentity;

    @Lob
    @Column(name = "embedding_vector", nullable = false, columnDefinition = "TEXT")
    private String embeddingVectorJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public FaceIdentityEntity getFaceIdentity() {
        return faceIdentity;
    }

    public void setFaceIdentity(FaceIdentityEntity faceIdentity) {
        this.faceIdentity = faceIdentity;
    }

    public String getEmbeddingVectorJson() {
        return embeddingVectorJson;
    }

    public void setEmbeddingVectorJson(String embeddingVectorJson) {
        this.embeddingVectorJson = embeddingVectorJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

