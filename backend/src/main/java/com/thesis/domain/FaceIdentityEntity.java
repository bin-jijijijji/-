package com.thesis.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "face_identities")
public class FaceIdentityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    // "whitelist" or "blacklist"
    @Column(name = "list_type", nullable = false, length = 20)
    private String listType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getListType() {
        return listType;
    }

    public void setListType(String listType) {
        this.listType = listType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

