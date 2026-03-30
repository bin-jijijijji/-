package com.thesis.api;

import com.thesis.domain.RoiRectangleEntity;
import com.thesis.domain.VideoAssetEntity;
import com.thesis.repo.RoiRectangleRepository;
import com.thesis.repo.VideoAssetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RoiController {

    private final VideoAssetRepository videoAssetRepository;
    private final RoiRectangleRepository roiRectangleRepository;

    public RoiController(VideoAssetRepository videoAssetRepository, RoiRectangleRepository roiRectangleRepository) {
        this.videoAssetRepository = videoAssetRepository;
        this.roiRectangleRepository = roiRectangleRepository;
    }

    public static class RoiView {
        public Long id;
        public String name;
        public double x1;
        public double y1;
        public double x2;
        public double y2;
    }

    public static class CreateRoiRequest {
        public String name;
        public double x1;
        public double y1;
        public double x2;
        public double y2;
    }

    @GetMapping("/video-assets/{videoAssetId}/rois")
    public ApiResponse<List<RoiView>> listRois(@PathVariable Long videoAssetId) {
        List<RoiRectangleEntity> list = roiRectangleRepository.findByVideoAsset_Id(videoAssetId);
        List<RoiView> res = list.stream().map(e -> {
            RoiView v = new RoiView();
            v.id = e.getId();
            v.name = e.getName();
            v.x1 = e.getX1();
            v.y1 = e.getY1();
            v.x2 = e.getX2();
            v.y2 = e.getY2();
            return v;
        }).toList();
        return ApiResponse.ok(res);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping("/video-assets/{videoAssetId}/rois")
    public ResponseEntity<ApiResponse<RoiView>> createRoi(
            @PathVariable Long videoAssetId,
            @RequestBody CreateRoiRequest req
    ) {
        VideoAssetEntity videoAsset = videoAssetRepository.findById(videoAssetId).orElse(null);
        if (videoAsset == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.err("videoAsset不存在"));
        }
        double x1 = req.x1;
        double y1 = req.y1;
        double x2 = req.x2;
        double y2 = req.y2;

        // normalize order
        if (x1 > x2) { double t = x1; x1 = x2; x2 = t; }
        if (y1 > y2) { double t = y1; y1 = y2; y2 = t; }

        if (!in01(x1) || !in01(y1) || !in01(x2) || !in01(y2) || (x2 - x1) < 0.001 || (y2 - y1) < 0.001) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.err("ROI坐标必须在[0,1]且尺寸非0"));
        }

        RoiRectangleEntity roi = new RoiRectangleEntity();
        roi.setVideoAsset(videoAsset);
        roi.setName(req.name == null || req.name.trim().isEmpty() ? "ROI-1" : req.name.trim());
        roi.setX1(x1);
        roi.setY1(y1);
        roi.setX2(x2);
        roi.setY2(y2);

        roiRectangleRepository.save(roi);

        RoiView v = new RoiView();
        v.id = roi.getId();
        v.name = roi.getName();
        v.x1 = roi.getX1();
        v.y1 = roi.getY1();
        v.x2 = roi.getX2();
        v.y2 = roi.getY2();
        return ResponseEntity.ok(ApiResponse.ok(v));
    }

    private boolean in01(double v) {
        return v >= 0.0 && v <= 1.0;
    }
}

