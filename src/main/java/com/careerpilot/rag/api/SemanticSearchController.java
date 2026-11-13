package com.careerpilot.rag.api;

import com.careerpilot.rag.application.ResumeRagService;
import com.careerpilot.shared.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rag")
public class SemanticSearchController {

    private final ResumeRagService resumeRagService;

    public SemanticSearchController(ResumeRagService resumeRagService) {
        this.resumeRagService = resumeRagService;
    }

    @GetMapping("/search")
    public List<ResumeRagService.ScoredChunk> search(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam UUID resumeId,
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK
    ) {
        return resumeRagService.searchSimilarChunks(currentUser.userId(), resumeId, query, topK);
    }
}
