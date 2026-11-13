package com.careerpilot.rag.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "resume_chunks")
public class ResumeChunk {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "resume_id", nullable = false)
    private UUID resumeId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "embedding_data", nullable = false, columnDefinition = "TEXT")
    private String embeddingData;

    protected ResumeChunk() {
    }

    public ResumeChunk(UUID userId, UUID resumeId, int chunkIndex, String content, String embeddingData) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.resumeId = resumeId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.embeddingData = embeddingData;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getResumeId() { return resumeId; }
    public int getChunkIndex() { return chunkIndex; }
    public String getContent() { return content; }
    public String getEmbeddingData() { return embeddingData; }
}
