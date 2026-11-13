package com.careerpilot.resume.application;

import com.careerpilot.resume.domain.Resume;
import com.careerpilot.resume.domain.ResumeRepository;
import com.careerpilot.shared.event.ResumeUploadedEvent;
import com.careerpilot.shared.exception.ResourceNotFoundException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ResumeService(ResumeRepository resumeRepository, ApplicationEventPublisher eventPublisher) {
        this.resumeRepository = resumeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Resume uploadResume(UUID userId, String versionName, String rawContent) {
        Resume resume = new Resume(userId, versionName, rawContent);
        Resume saved = resumeRepository.save(resume);

        eventPublisher.publishEvent(new ResumeUploadedEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getVersionName(),
                saved.getRawContent()
        ));
        return saved;
    }

    @Transactional
    public Resume uploadPdfResume(UUID userId, String versionName, byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("Uploaded PDF file is empty");
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(document).trim();
            if (extractedText.isBlank()) {
                throw new IllegalArgumentException("Could not extract any text from PDF resume");
            }
            return uploadResume(userId, versionName, extractedText);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to parse PDF file: " + ex.getMessage(), ex);
        }
    }

    @Transactional
    public void markResumeEmbedded(UUID resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found: " + resumeId));
        resume.markEmbedded();
        resumeRepository.save(resume);
    }

    @Transactional(readOnly = true)
    public Resume getResume(UUID resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found: " + resumeId));
    }

    @Transactional(readOnly = true)
    public List<Resume> listResumesForUser(UUID userId) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void deleteResumeForUser(UUID userId, UUID resumeId) {
        Resume resume = getResume(resumeId);
        if (!resume.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Resume not found: " + resumeId);
        }
        resumeRepository.delete(resume);
    }
}
