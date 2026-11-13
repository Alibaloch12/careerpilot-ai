package com.careerpilot.rag.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ResumeChunkRepository extends JpaRepository<ResumeChunk, UUID> {
    List<ResumeChunk> findByUserIdAndResumeIdOrderByChunkIndexAsc(UUID userId, UUID resumeId);
    void deleteByUserIdAndResumeId(UUID userId, UUID resumeId);
}
