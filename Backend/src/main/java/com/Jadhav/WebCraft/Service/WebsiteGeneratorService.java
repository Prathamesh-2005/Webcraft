package com.Jadhav.WebCraft.Service;

import com.Jadhav.WebCraft.dto.GenerateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WebsiteGeneratorService {
    @Autowired
    private GoogleGenAIService googleGenAIService;

    @Autowired
    private ObjectMapper objectMapper;

    public GenerateResponse generateWebsite(String prompt) throws Exception {
        try {
            String aiResponse = googleGenAIService.generateContent(prompt);

            String cleanedResponse = aiResponse
                    .replaceAll("```(?:json|js|javascript)?", "")
                    .replaceAll("```", "")
                    .trim();

            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);

            String html = jsonNode.get("html") != null ? jsonNode.get("html").asText() : null;
            String css = jsonNode.get("css") != null ? jsonNode.get("css").asText() : null;
            String js = jsonNode.get("js") != null ? jsonNode.get("js").asText() : null;

            if (html == null || css == null || js == null) {
                throw new IllegalArgumentException("Missing required code fields in AI response");
            }

            return new GenerateResponse(html, css, js);

        } catch (Exception e) {
            throw new Exception("Failed to generate website: " + e.getMessage(), e);
        }
    }

}
