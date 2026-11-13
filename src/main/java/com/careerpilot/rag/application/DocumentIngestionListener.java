package com.careerpilot.rag.application;

import com.careerpilot.resume.application.ResumeService;
import com.careerpilot.shared.event.ResumeUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentIngestionListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionListener.class);

    private final ResumeRagService resumeRagService;
    private final ResumeService resumeService;

    public DocumentIngestionListener(ResumeRagService resumeRagService, ResumeService resumeService) {
        this.resumeRagService = resumeRagService;
        this.resumeService = resumeService;
    }

    @EventListener
    public void onResumeUploaded(ResumeUploadedEvent event) {
        log.info("[RAG Listener] Received ResumeUploadedEvent for resumeId={}", event.resumeId());
        resumeRagService.ingestResumeChunks(event.userId(), event.resumeId(), event.rawContent());
        resumeService.markResumeEmbedded(event.resumeId());
    }
}
