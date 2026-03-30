package com.thesis.repo;

import com.thesis.domain.FaceEmbeddingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaceEmbeddingRepository extends JpaRepository<FaceEmbeddingEntity, Long> {
    List<FaceEmbeddingEntity> findByFaceIdentity_Id(Long faceIdentityId);
}

