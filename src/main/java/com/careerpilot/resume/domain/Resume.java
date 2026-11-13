package com.careerpilot.resume.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "version_name", nullable = false)
    private String versionName;

    @Column(name = "raw_content", nullable = false, columnDefinition = "TEXT")
    private String rawContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "embedding_status", nullable = false)
    private EmbeddingStatus embeddingStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Resume() {
    }

    public Resume(UUID userId, String versionName, String rawContent) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.versionName = versionName;
        this.rawContent = rawContent;
        this.embeddingStatus = EmbeddingStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void markEmbedded() {
        this.embeddingStatus = EmbeddingStatus.COMPLETED;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getVersionName() { return versionName; }
    public String getRawContent() { return rawContent; }
    public EmbeddingStatus getEmbeddingStatus() { return embeddingStatus; }
}
