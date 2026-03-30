package com.thesis.repo;

import com.thesis.domain.RecognitionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecognitionEventRepository extends JpaRepository<RecognitionEventEntity, Long> {
    List<RecognitionEventEntity> findTop50ByOrderByIdDesc();
    List<RecognitionEventEntity> findTop50ByJob_IdOrderByIdDesc(Long jobId);
}

