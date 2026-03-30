package com.thesis.api;

import com.thesis.domain.FaceIdentityEntity;
import com.thesis.repo.FaceIdentityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api")
public class IdentitiesController {

    private final FaceIdentityRepository faceIdentityRepository;

    @Value("${app.recognition.base-url}")
    private String recognitionBaseUrl;

    @Value("${app.recognition.service-token}")
    private String recognitionToken;

    @Value("${app.uploads.video-dir}")
    private String uploadsBaseDir;

    public IdentitiesController(FaceIdentityRepository faceIdentityRepository) {
        this.faceIdentityRepository = faceIdentityRepository;
    }

    public static class IdentityIngestRequest {
        public String name;
        public String listType; // whitelist/blacklist
        public List<String> imagePaths;
    }

    public static class IdentityIngestResponse {
        public boolean ok;
        public Long faceIdentityId;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/identities")
    public ApiResponse<List<FaceIdentityView>> listIdentities() {
        return ApiResponse.ok(faceIdentityRepository.findAll().stream().map(FaceIdentityView::from).toList());
    }

    public static class FaceIdentityView {
        public Long id;
        public String name;
        public String listType;
        public static FaceIdentityView from(FaceIdentityEntity e) {
            FaceIdentityView v = new FaceIdentityView();
            v.id = e.getId();
            v.name = e.getName();
            v.listType = e.getListType();
            return v;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/identities", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> ingestIdentity(
            @RequestParam("name") String name,
            @RequestParam("listType") String listType,
            @RequestPart("images") MultipartFile[] images
    ) throws Exception {

        String safeName = name == null ? "" : name.trim();
        String safeType = listType == null ? "" : listType.trim().toLowerCase(Locale.ROOT);
        if (safeName.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.err("name不能为空"));
        if (!("whitelist".equals(safeType) || "blacklist".equals(safeType))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.err("listType必须是whitelist或blacklist"));
        }
        if (images == null || images.length == 0) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.err("至少上传一张图片"));

        String facesDir = Path.of(uploadsBaseDir, "faces").toAbsolutePath().toString();
        Files.createDirectories(Path.of(facesDir));
        String jobFolder = UUID.randomUUID().toString();
        Path targetDir = Path.of(facesDir, jobFolder);
        Files.createDirectories(targetDir);

        List<String> savedPaths = new ArrayList<>();
        for (MultipartFile file : images) {
            if (file == null || file.isEmpty()) continue;
            String original = file.getOriginalFilename() == null ? "img.jpg" : file.getOriginalFilename();
            String ext = "";
            int lastDot = original.lastIndexOf('.');
            if (lastDot >= 0) ext = original.substring(lastDot);
            if (ext.isEmpty()) ext = ".jpg";

            String filename = UUID.randomUUID().toString() + ext;
            Path out = targetDir.resolve(filename);
            file.transferTo(out);
            savedPaths.add(out.toAbsolutePath().toString());
        }

        if (savedPaths.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.err("图片未保存成功"));

        IdentityIngestRequest req = new IdentityIngestRequest();
        req.name = safeName;
        req.listType = safeType;
        req.imagePaths = savedPaths;

        String url = recognitionBaseUrl + "/api/identities/ingest";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Recognition-Token", recognitionToken);

        HttpEntity<IdentityIngestRequest> entity = new HttpEntity<>(req, headers);
        ResponseEntity<ApiResponse> resp = restTemplate.exchange(url, HttpMethod.POST, entity, ApiResponse.class);

        return ResponseEntity.ok(resp.getBody());
    }
}

