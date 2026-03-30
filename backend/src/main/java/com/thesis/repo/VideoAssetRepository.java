package com.thesis.repo;

import com.thesis.domain.VideoAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoAssetRepository extends JpaRepository<VideoAssetEntity, Long> {
    List<VideoAssetEntity> findByCamera_Id(Long cameraId);
}

