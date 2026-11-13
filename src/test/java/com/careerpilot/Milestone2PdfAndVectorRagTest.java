package com.careerpilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Milestone2PdfAndVectorRagTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void pdfUploadAndVectorRag_parsesPdfChunksVectorsAndRanksSemanticSearchByCosineSimilarity() throws Exception {
        // 0. Verify Frontend UI is publicly accessible at /index.html
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk());

        // 1. Register user and obtain JWT
        String registerPayload = """
                {
                  "email": "rag-engineer@careerpilot.ai",
                  "password": "StrongPassword123!",
                  "fullName": "Murad Baloch"
                }
                """;
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String jwtToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        // 2. Generate a real 2-section PDF Resume in memory using Apache PDFBox
        byte[] pdfBytes = createSampleResumePdf();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "murad-resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                pdfBytes
        );

        // 3. Upload PDF Resume via Multipart endpoint /api/v1/resumes/upload-pdf
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/resumes/upload-pdf")
                        .file(pdfFile)
                        .param("versionName", "PDF Backend & AI Resume v1")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andReturn();

        String resumeId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .get("id").asText();

        // 4. Verify Resume status transitioned to COMPLETED after RAG event ingestion
        mockMvc.perform(get("/api/v1/resumes/" + resumeId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embeddingStatus").value("COMPLETED"));

        // 5. Query Semantic Vector Search (/api/v1/rag/search) for backend microservices skills
        MvcResult searchResult = mockMvc.perform(get("/api/v1/rag/search")
                        .param("resumeId", resumeId)
                        .param("query", "Java Spring Boot PostgreSQL backend architecture")
                        .param("topK", "2")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();

        JsonNode results = objectMapper.readTree(searchResult.getResponse().getContentAsString());
        String topChunkText = results.get(0).get("content").asText();
        double topScore = results.get(0).get("similarityScore").asDouble();
        double secondScore = results.get(1).get("similarityScore").asDouble();

        assertThat(topChunkText).contains("Java", "Spring Boot", "PostgreSQL");
        assertThat(topScore).isGreaterThan(secondScore);
    }

    private byte[] createSampleResumePdf() throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream contents = new PDPageContentStream(doc, page)) {
                contents.beginText();
                contents.setFont(font, 12);
                contents.newLineAtOffset(50, 700);
                contents.showText("Backend Experience: Built scalable Java and Spring Boot services with PostgreSQL.");
                contents.newLineAtOffset(0, -30);
                contents.showText("");
                contents.newLineAtOffset(0, -30);
                contents.showText("Education: Bachelor of Computer Science and Mathematics.");
                contents.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
