package com.thesis.repo;

import com.thesis.domain.RoiRectangleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoiRectangleRepository extends JpaRepository<RoiRectangleEntity, Long> {
    List<RoiRectangleEntity> findByVideoAsset_Id(Long videoAssetId);
}

