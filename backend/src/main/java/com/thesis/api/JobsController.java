package com.thesis.api;

import com.thesis.domain.RecognitionJobEntity;
import com.thesis.domain.RoiRectangleEntity;
import com.thesis.domain.VideoAssetEntity;
import com.thesis.repo.RecognitionJobRepository;
import com.thesis.repo.RoiRectangleRepository;
import com.thesis.repo.VideoAssetRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class JobsController {

    private final RecognitionJobRepository jobRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final RoiRectangleRepository roiRectangleRepository;

    @Value("${app.recognition.base-url}")
    private String recognitionBaseUrl;

    @Value("${app.recognition.service-token}")
    private String recognitionToken;

    public JobsController(
            RecognitionJobRepository jobRepository,
            VideoAssetRepository videoAssetRepository,
            RoiRectangleRepository roiRectangleRepository
    ) {
        this.jobRepository = jobRepository;
        this.videoAssetRepository = videoAssetRepository;
        this.roiRectangleRepository = roiRectangleRepository;
    }

    public static class StartJobRequest {
        public Long videoAssetId;
        public Long roiRectangleId;
        public double threshold;
    }

    public static class JobView {
        public Long id;
        public String status;
        public double threshold;
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping("/jobs/start")
    public ResponseEntity<ApiResponse<JobView>> startJob(
            @RequestBody StartJobRequest req,
            Authentication authentication
    ) {
        if (req.videoAssetId == null || req.roiRectangleId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.err("videoAssetId/roiRectangleId不能为空"));
        }
        if (req.threshold <= 0 || req.threshold > 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.err("threshold必须在(0,1]"));
        }

        VideoAssetEntity videoAsset = videoAssetRepository.findById(req.videoAssetId).orElse(null);
        RoiRectangleEntity roi = roiRectangleRepository.findById(req.roiRectangleId).orElse(null);
        if (videoAsset == null || roi == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.err("videoAsset或roi不存在"));
        }
        if (roi.getVideoAsset() == null || !roi.getVideoAsset().getId().equals(videoAsset.getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.err("ROI必须属于该videoAsset"));
        }

        String createdBy = authentication == null ? "unknown" : authentication.getName();

        RecognitionJobEntity job = new RecognitionJobEntity();
        job.setVideoAsset(videoAsset);
        job.setRoiRectangle(roi);
        job.setThreshold(req.threshold);
        job.setCreatedBy(createdBy);
        job.setStatus("RUNNING");
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);

        // call recognition-service to run asynchronously
        String url = recognitionBaseUrl + "/api/jobs/" + job.getId() + "/run";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Recognition-Token", recognitionToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // We don't block on long processing: assume recognition-service starts processing quickly.
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            // service may be down; keep job record but mark failed later.
        }

        JobView v = new JobView();
        v.id = job.getId();
        v.status = job.getStatus();
        v.threshold = job.getThreshold();
        return ResponseEntity.ok(ApiResponse.ok(v));
    }
}

