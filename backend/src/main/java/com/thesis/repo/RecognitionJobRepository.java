package com.thesis.repo;

import com.thesis.domain.RecognitionJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecognitionJobRepository extends JpaRepository<RecognitionJobEntity, Long> {
    List<RecognitionJobEntity> findByCreatedByOrderByIdDesc(String createdBy);
}

