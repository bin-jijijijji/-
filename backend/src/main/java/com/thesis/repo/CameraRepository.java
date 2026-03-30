package com.thesis.repo;

import com.thesis.domain.CameraEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CameraRepository extends JpaRepository<CameraEntity, Long> {
    Optional<CameraEntity> findByName(String name);
}

