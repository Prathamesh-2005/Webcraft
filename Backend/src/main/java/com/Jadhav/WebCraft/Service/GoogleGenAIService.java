package com.Jadhav.WebCraft.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

@Service
public class GoogleGenAIService {
    @Value("${google.ai.api.key}")
    private String apiKey;

    public String generateContent(String prompt) throws Exception {
        Client client = Client.builder()
                .apiKey(apiKey)
                .build();

        String fullPrompt = String.format("""
            You are an expert web developer. Create a complete, functional website based on the following description: 
            "%s"
            Respond ONLY with a valid JSON object with these keys:
            - html: complete HTML code
            - css: complete CSS code
            - js: complete JavaScript code
            Requirements:
            1. The website must be fully responsive
            2. Use modern, clean design
            3. Include all necessary functionality
            4. No placeholders - use actual content
            5. No explanations or markdown formatting. Only return a raw JSON object.
            """, prompt);

        GenerateContentResponse response = client.models.generateContent(
                "gemini-1.5-flash",
                fullPrompt,
                null
        );

        return response.text();
    }
}