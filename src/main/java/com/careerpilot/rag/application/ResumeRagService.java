package com.careerpilot.rag.application;

import com.careerpilot.rag.domain.ResumeChunk;
import com.careerpilot.rag.domain.ResumeChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResumeRagService {

    private static final Logger log = LoggerFactory.getLogger(ResumeRagService.class);
    private static final int VECTOR_DIMENSIONS = 128;

    public record ScoredChunk(UUID resumeId, int chunkIndex, String content, double similarityScore) {}

    private final ResumeChunkRepository chunkRepository;

    public ResumeRagService(ResumeChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    @Transactional
    public void ingestResumeChunks(UUID userId, UUID resumeId, String rawContent) {
        chunkRepository.deleteByUserIdAndResumeId(userId, resumeId);

        String[] rawParagraphs = rawContent.split("\\r?\\n+");
        List<ResumeChunk> entities = new ArrayList<>();

        int index = 0;
        for (String paragraph : rawParagraphs) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty()) {
                double[] vector = computeEmbeddingVector(trimmed);
                String serializedVector = serializeVector(vector);
                entities.add(new ResumeChunk(userId, resumeId, index++, trimmed, serializedVector));
            }
        }

        chunkRepository.saveAll(entities);
        log.info("[RAG Module] Persisted {} vector chunks ({} dims) in database for user={} resume={}",
                entities.size(), VECTOR_DIMENSIONS, userId, resumeId);
    }

    @Transactional(readOnly = true)
    public List<String> retrieveRelevantChunks(UUID userId, UUID resumeId) {
        return chunkRepository.findByUserIdAndResumeIdOrderByChunkIndexAsc(userId, resumeId)
                .stream()
                .map(ResumeChunk::getContent)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScoredChunk> searchSimilarChunks(UUID userId, UUID resumeId, String query, int topK) {
        List<ResumeChunk> chunks = chunkRepository.findByUserIdAndResumeIdOrderByChunkIndexAsc(userId, resumeId);
        if (chunks.isEmpty()) {
            return Collections.emptyList();
        }

        double[] queryVector = computeEmbeddingVector(query);
        return chunks.stream()
                .map(chunk -> {
                    double[] chunkVector = deserializeVector(chunk.getEmbeddingData());
                    double sim = cosineSimilarity(queryVector, chunkVector);
                    return new ScoredChunk(
                            chunk.getResumeId(),
                            chunk.getChunkIndex(),
                            chunk.getContent(),
                            Math.round(sim * 1000.0) / 1000.0
                    );
                })
                .sorted(Comparator.comparingDouble(ScoredChunk::similarityScore).reversed())
                .limit(Math.max(1, topK))
                .toList();
    }

    double[] computeEmbeddingVector(String text) {
        double[] vector = new double[VECTOR_DIMENSIONS];
        String[] tokens = text.toLowerCase().replaceAll("[^a-z0-9\\s+#.]", " ").split("\\s+");

        for (String token : tokens) {
            if (token.length() < 2) {
                continue;
            }
            int bucket = Math.floorMod(token.hashCode(), VECTOR_DIMENSIONS);
            vector[bucket] += 1.0;

            for (int i = 0; i <= token.length() - 3; i++) {
                String trigram = token.substring(i, i + 3);
                int triBucket = Math.floorMod(trigram.hashCode(), VECTOR_DIMENSIONS);
                vector[triBucket] += 0.5;
            }
        }

        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    double cosineSimilarity(double[] a, double[] b) {
        int len = Math.min(a.length, b.length);
        double dot = 0.0;
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
        }
        return Math.max(0.0, Math.min(1.0, dot));
    }

    private String serializeVector(double[] vector) {
        return Arrays.stream(vector)
                .mapToObj(d -> String.format(Locale.ROOT, "%.6f", d))
                .collect(Collectors.joining(","));
    }

    private double[] deserializeVector(String data) {
        if (data == null || data.isBlank()) {
            return new double[VECTOR_DIMENSIONS];
        }
        String[] parts = data.split(",");
        double[] vec = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vec[i] = Double.parseDouble(parts[i]);
        }
        return vec;
    }
}
