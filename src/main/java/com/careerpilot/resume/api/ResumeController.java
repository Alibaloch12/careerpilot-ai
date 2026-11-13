package com.careerpilot.resume.api;

import com.careerpilot.resume.application.ResumeService;
import com.careerpilot.resume.domain.EmbeddingStatus;
import com.careerpilot.resume.domain.Resume;
import com.careerpilot.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    public record UploadResumeRequest(
            @NotBlank String versionName,
            @NotBlank String rawContent
    ) {}

    public record ResumeResponse(
            UUID id,
            UUID userId,
            String versionName,
            String rawContent,
            EmbeddingStatus embeddingStatus
    ) {
        public static ResumeResponse from(Resume r) {
            return new ResumeResponse(
                    r.getId(),
                    r.getUserId(),
                    r.getVersionName(),
                    r.getRawContent(),
                    r.getEmbeddingStatus()
            );
        }
    }

    @GetMapping
    public List<ResumeResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return resumeService.listResumesForUser(currentUser.userId())
                .stream()
                .map(ResumeResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponse upload(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UploadResumeRequest request
    ) {
        Resume resume = resumeService.uploadResume(
                currentUser.userId(),
                request.versionName(),
                request.rawContent()
        );
        return ResumeResponse.from(resume);
    }

    @PostMapping(value = "/upload-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponse uploadPdf(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam("versionName") String versionName,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        Resume resume = resumeService.uploadPdfResume(
                currentUser.userId(),
                versionName,
                file.getBytes()
        );
        return ResumeResponse.from(resume);
    }

    @GetMapping("/{id}")
    public ResumeResponse getById(@PathVariable UUID id) {
        return ResumeResponse.from(resumeService.getResume(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id
    ) {
        resumeService.deleteResumeForUser(currentUser.userId(), id);
    }
}
