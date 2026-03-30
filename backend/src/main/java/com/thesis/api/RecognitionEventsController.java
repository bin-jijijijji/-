package com.thesis.api;

import com.thesis.domain.*;
import com.thesis.repo.*;
import com.thesis.ws.AlarmMessage;
import com.thesis.ws.AlarmWebSocketPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RecognitionEventsController {

    private final RecognitionEventRepository eventRepository;
    private final RecognitionJobRepository jobRepository;
    private final FaceIdentityRepository faceIdentityRepository;
    private final AlarmWebSocketPublisher publisher;

    @Value("${app.recognition.service-token}")
    private String recognitionServiceToken;

    private final long snapshotBase = 0L; // placeholder to keep code simple later

    public RecognitionEventsController(
            RecognitionEventRepository eventRepository,
            RecognitionJobRepository jobRepository,
            FaceIdentityRepository faceIdentityRepository,
            AlarmWebSocketPublisher publisher
    ) {
        this.eventRepository = eventRepository;
        this.jobRepository = jobRepository;
        this.faceIdentityRepository = faceIdentityRepository;
        this.publisher = publisher;
    }

    @PostMapping("/recognition/events")
    public ResponseEntity<ApiResponse<Object>> createRecognitionEvent(
            @RequestHeader(value = "Recognition-Token", required = false) String token,
            @RequestBody RecognitionEventCreateRequest request
    ) {
        if (token == null || token.isEmpty() || !token.equals(recognitionServiceToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.err("recognition token invalid"));
        }

        if (request.getJobId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.err("jobId不能为空"));
        }
        if (request.getEventType() == null || request.getEventType().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.err("eventType不能为空"));
        }

        RecognitionJobEntity job = jobRepository.findById(request.getJobId()).orElse(null);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.err("job不存在"));
        }

        FaceIdentityEntity faceIdentity = null;
        if (request.getFaceIdentityId() != null) {
            faceIdentity = faceIdentityRepository.findById(request.getFaceIdentityId()).orElse(null);
        }

        RecognitionEventEntity entity = new RecognitionEventEntity();
        entity.setJob(job);
        entity.setEventType(request.getEventType());
        entity.setFaceIdentity(faceIdentity);
        entity.setMatchedName(request.getMatchedName());
        entity.setScore(request.getScore());
        entity.setFrameIndex(request.getFrameIndex());
        entity.setTimestampMs(request.getTimestampMs());
        entity.setSnapshotPath(normalizeSnapshotPath(request.getSnapshotPath()));

        eventRepository.save(entity);

        boolean alert = "blacklist_match".equalsIgnoreCase(request.getEventType());

        AlarmMessage msg = new AlarmMessage();
        msg.setEventId(entity.getId());
        msg.setJobId(job.getId());
        msg.setEventType(entity.getEventType());
        msg.setMatchedName(entity.getMatchedName());
        msg.setScore(entity.getScore());
        msg.setFrameIndex(entity.getFrameIndex());
        msg.setTimestampMs(entity.getTimestampMs());
        msg.setSnapshotUrl(buildSnapshotUrl(entity.getSnapshotPath()));
        msg.setAlert(alert);

        publisher.publish(msg);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/events")
    public ApiResponse<List<RecognitionEventView>> listEvents(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        if (limit <= 0) limit = 50;
        List<RecognitionEventEntity> events = eventRepository.findTop50ByOrderByIdDesc();
        // limit is applied client-side for simplicity; thesis demo scale is small.
        int to = Math.min(limit, events.size());
        return ApiResponse.ok(events.subList(0, to).stream().map(RecognitionEventView::from).toList());
    }

    private String normalizeSnapshotPath(String snapshotPath) {
        if (snapshotPath == null) return null;
        String p = snapshotPath.replace("\\", "/");
        // allow either absolute or relative; keep only last segment
        return p.startsWith("/") ? p.substring(1) : p;
    }

    private String buildSnapshotUrl(String snapshotPath) {
        if (snapshotPath == null || snapshotPath.isEmpty()) return null;
        // snapshotPath can be either a filename or a relative path; serve as-is.
        return "/files/snapshots/" + snapshotPath;
    }

    public static class RecognitionEventView {
        public Long id;
        public Long jobId;
        public String eventType;
        public String matchedName;
        public double score;
        public int frameIndex;
        public long timestampMs;
        public String snapshotUrl;

        public static RecognitionEventView from(RecognitionEventEntity e) {
            RecognitionEventView v = new RecognitionEventView();
            v.id = e.getId();
            v.jobId = e.getJob().getId();
            v.eventType = e.getEventType();
            v.matchedName = e.getMatchedName();
            v.score = e.getScore();
            v.frameIndex = e.getFrameIndex();
            v.timestampMs = e.getTimestampMs();
            v.snapshotUrl = e.getSnapshotPath() == null ? null : "/files/snapshots/" + e.getSnapshotPath();
            return v;
        }
    }
}

