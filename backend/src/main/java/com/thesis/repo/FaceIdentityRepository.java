package com.thesis.repo;

import com.thesis.domain.FaceIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaceIdentityRepository extends JpaRepository<FaceIdentityEntity, Long> {
    List<FaceIdentityEntity> findByListType(String listType);
}

