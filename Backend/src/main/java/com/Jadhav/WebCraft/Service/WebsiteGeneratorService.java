package com.Jadhav.WebCraft.Service;

import com.Jadhav.WebCraft.dto.GenerateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class WebsiteGeneratorService {
    private static final Logger logger = LoggerFactory.getLogger(WebsiteGeneratorService.class);

    @Autowired
    private GoogleGenAIService googleGenAIService;

    @Autowired
    private ObjectMapper objectMapper;

    public GenerateResponse generateWebsite(String prompt) throws Exception {
        try {
            String aiResponse = googleGenAIService.generateContent(prompt);
            logger.info("Raw AI Response length: {}", aiResponse.length());

            GenerateResponse response = parseAIResponse(aiResponse);

            if (response == null) {
                logger.warn("Could not parse AI response, creating fallback response");
                response = createFallbackResponse(prompt);
            }

            response = validateAndCleanResponse(response, prompt);

            logger.info("Successfully generated website with HTML: {} chars, CSS: {} chars, JS: {} chars",
                    response.getHtml().length(), response.getCss().length(), response.getJs().length());

            return response;
        } catch (Exception e) {
            logger.error("Website generation failed", e);
            throw new Exception("Failed to generate website: " + e.getMessage(), e);
        }
    }

    private GenerateResponse validateAndCleanResponse(GenerateResponse response, String prompt) {
        if (response == null) {
            return createFallbackResponse(prompt);
        }

        String html = cleanHtml(response.getHtml());
        if (isNullOrEmpty(html)) {
            html = createFallbackHtml(prompt);
        }

        String css = cleanCss(response.getCss());
        if (isNullOrEmpty(css)) {
            css = createFallbackCss();
        }

        String js = cleanJavaScript(response.getJs());
        if (isNullOrEmpty(js)) {
            js = createFallbackJs();
        }

        return new GenerateResponse(html, css, js);
    }

    private String cleanHtml(String html) {
        if (isNullOrEmpty(html)) return "";

        if (!html.trim().startsWith("<!DOCTYPE") && !html.trim().startsWith("<html")) {
            html = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>Generated Website</title>\n</head>\n<body>\n" + html + "\n</body>\n</html>";
        }

        html = html.replace("\\\"", "\"")
                .replace("\\'", "'")
                .replace("\\n", "\n")
                .replace("\\t", "\t");

        return html.trim();
    }

    private String cleanCss(String css) {
        if (isNullOrEmpty(css)) return "";

        css = css.replace("\\\"", "\"")
                .replace("\\'", "'")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r");

        if (!css.contains("body") && !css.contains("*")) {
            css = "body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }\n" + css;
        }

        return css.trim();
    }

    private String cleanJavaScript(String js) {
        if (isNullOrEmpty(js)) return "";

        // Remove any wrapping quotes or escapes
        js = js.replace("\\\"", "\"")
                .replace("\\'", "'")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r");

        js = fixCommonJsSyntaxErrors(js);

        return js.trim();
    }

    private String fixCommonJsSyntaxErrors(String js) {
        js = js.replaceAll("if\\s*\\([^)]*\\)\\s*\\{[^}]*$", "");

        js = js.replaceAll("function\\s+\\w+\\s*\\([^)]*\\)\\s*\\{[^}]*$", "");

        String[] lines = js.split("\n");
        StringBuilder cleanJs = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.endsWith("s") || line.endsWith("s;") || line.endsWith("s}")) {
                if (!line.equals("s") && !line.endsWith(" s")) {
                    cleanJs.append(line).append("\n");
                }
            }
        }

        return cleanJs.toString();
    }

    private GenerateResponse parseAIResponse(String aiResponse) {
        try {
            GenerateResponse result = parseAsDirectJson(aiResponse);
            if (result != null && !isEmptyResponse(result)) {
                logger.info("Successfully parsed using direct JSON strategy");
                return result;
            }
        } catch (Exception e) {
            logger.warn("Direct JSON parsing failed: {}", e.getMessage());
        }

        try {
            GenerateResponse result = parseWithImprovedRegex(aiResponse);
            if (result != null && !isEmptyResponse(result)) {
                logger.info("Successfully parsed using improved regex strategy");
                return result;
            }
        } catch (Exception e) {
            logger.warn("Improved regex parsing failed: {}", e.getMessage());
        }

        try {
            GenerateResponse result = parseWithBetterManualExtraction(aiResponse);
            if (result != null && !isEmptyResponse(result)) {
                logger.info("Successfully parsed using better manual strategy");
                return result;
            }
        } catch (Exception e) {
            logger.warn("Better manual parsing failed: {}", e.getMessage());
        }

        return null;
    }

    private GenerateResponse parseAsDirectJson(String response) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            String html = getJsonFieldValue(jsonNode, "html");
            String css = getJsonFieldValue(jsonNode, "css");
            String js = getJsonFieldValue(jsonNode, "js");

            if (html != null && css != null && js != null) {
                return new GenerateResponse(html, css, js);
            }
        } catch (Exception e) {
            logger.debug("Direct JSON parsing error: {}", e.getMessage());
            throw e;
        }
        return null;
    }

    private GenerateResponse parseWithImprovedRegex(String response) {
        try {
            String html = extractFieldWithRegex(response, "html");
            String css = extractFieldWithRegex(response, "css");
            String js = extractFieldWithRegex(response, "js");

            if (html != null && css != null && js != null) {
                return new GenerateResponse(html, css, js);
            }
        } catch (Exception e) {
            logger.error("Improved regex parsing error", e);
        }
        return null;
    }

    private GenerateResponse parseWithBetterManualExtraction(String response) {
        try {
            String html = extractFieldManually(response, "html");
            String css = extractFieldManually(response, "css");
            String js = extractFieldManually(response, "js");

            if (html != null && css != null && js != null) {
                return new GenerateResponse(html, css, js);
            }
        } catch (Exception e) {
            logger.error("Better manual parsing error", e);
        }
        return null;
    }

    private String extractFieldWithRegex(String text, String fieldName) {
        String[] patterns = {
                "\"" + fieldName + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
                "'" + fieldName + "'\\s*:\\s*'((?:[^'\\\\]|\\\\.)*)'",
                "\"" + fieldName + "\"\\s*:\\s*`([^`]*)`",
                fieldName + "\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
        };

        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern, Pattern.DOTALL);
            Matcher m = p.matcher(text);
            if (m.find()) {
                String result = m.group(1);
                return unescapeString(result);
            }
        }
        return null;
    }

    private String extractFieldManually(String response, String fieldName) {
        String fieldPattern = "\"" + fieldName + "\"";
        int fieldIndex = response.indexOf(fieldPattern);
        if (fieldIndex == -1) {
            return null;
        }

        int colonIndex = response.indexOf(":", fieldIndex);
        if (colonIndex == -1) {
            return null;
        }

        int startQuote = response.indexOf("\"", colonIndex);
        if (startQuote == -1) {
            return null;
        }

        int endQuote = findMatchingQuote(response, startQuote + 1);
        if (endQuote == -1) {
            return null;
        }

        String content = response.substring(startQuote + 1, endQuote);
        return unescapeString(content);
    }

    private int findMatchingQuote(String text, int startIndex) {
        boolean escaped = false;
        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private String unescapeString(String str) {
        if (str == null) return null;
        return str.replace("\\\"", "\"")
                .replace("\\'", "'")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\u003C", "<")
                .replace("\\u003E", ">")
                .replace("\\u0026", "&");
    }

    private String getJsonFieldValue(JsonNode jsonNode, String fieldName) {
        JsonNode fieldNode = jsonNode.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return "";
        }
        return fieldNode.asText();
    }

    private boolean isEmptyResponse(GenerateResponse response) {
        return response == null ||
                (isNullOrEmpty(response.getHtml()) &&
                        isNullOrEmpty(response.getCss()) &&
                        isNullOrEmpty(response.getJs()));
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String createFallbackHtml(String prompt) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Generated Website</title>
            </head>
            <body>
                <div class="container">
                    <h1>Welcome to Your Generated Website</h1>
                    <p class="description">Request: %s</p>
                    <div class="features">
                        <div class="feature">
                            <h3>Feature 1</h3>
                            <p>This is a sample feature of your website.</p>
                        </div>
                        <div class="feature">
                            <h3>Feature 2</h3>
                            <p>Another great feature for your users.</p>
                        </div>
                    </div>
                    <button onclick="showMessage()" class="btn">Click Me</button>
                    <div id="message" class="message"></div>
                </div>
            </body>
            </html>
            """.formatted(prompt != null ? prompt : "Website Generation");
    }

    private String createFallbackCss() {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                line-height: 1.6;
                color: #333;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 20px;
            }
            
            .container {
                background: rgba(255, 255, 255, 0.95);
                padding: 3rem;
                border-radius: 20px;
                box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
                backdrop-filter: blur(10px);
                max-width: 800px;
                width: 100%;
                text-align: center;
                animation: fadeInUp 0.8s ease-out;
            }
            
            @keyframes fadeInUp {
                from {
                    opacity: 0;
                    transform: translateY(30px);
                }
                to {
                    opacity: 1;
                    transform: translateY(0);
                }
            }
            
            h1 {
                color: #2c3e50;
                margin-bottom: 1.5rem;
                font-size: 2.5rem;
                font-weight: 700;
            }
            
            .description {
                color: #7f8c8d;
                margin-bottom: 2rem;
                font-size: 1.1rem;
                font-style: italic;
            }
            
            .features {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                gap: 2rem;
                margin: 2rem 0;
            }
            
            .feature {
                background: #f8f9fa;
                padding: 1.5rem;
                border-radius: 12px;
                border: 1px solid #e9ecef;
                transition: transform 0.3s ease, box-shadow 0.3s ease;
            }
            
            .feature:hover {
                transform: translateY(-5px);
                box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
            }
            
            .feature h3 {
                color: #495057;
                margin-bottom: 0.5rem;
            }
            
            .btn {
                background: linear-gradient(45deg, #4CAF50, #45a049);
                color: white;
                border: none;
                padding: 12px 30px;
                border-radius: 25px;
                cursor: pointer;
                font-size: 16px;
                font-weight: 600;
                margin: 20px 0;
                transition: all 0.3s ease;
                box-shadow: 0 4px 15px rgba(76, 175, 80, 0.3);
            }
            
            .btn:hover {
                background: linear-gradient(45deg, #45a049, #4CAF50);
                transform: translateY(-2px);
                box-shadow: 0 6px 20px rgba(76, 175, 80, 0.4);
            }
            
            .message {
                margin-top: 1rem;
                padding: 15px;
                border-radius: 8px;
                background: #e8f5e8;
                color: #2e7d32;
                font-weight: bold;
                opacity: 0;
                transition: opacity 0.3s ease;
            }
            
            .message.show {
                opacity: 1;
            }
            
            @media (max-width: 768px) {
                .container {
                    padding: 2rem;
                    margin: 10px;
                }
                
                h1 {
                    font-size: 2rem;
                }
                
                .features {
                    grid-template-columns: 1fr;
                    gap: 1rem;
                }
            }
            """;
    }

    private String createFallbackJs() {
        return """
            function showMessage() {
                const messageEl = document.getElementById('message');
                messageEl.innerHTML = '🎉 Hello! Your website is working perfectly!';
                messageEl.classList.add('show');
                
                // Add some interactive animation
                setTimeout(() => {
                    messageEl.style.transform = 'scale(1.05)';
                    setTimeout(() => {
                        messageEl.style.transform = 'scale(1)';
                    }, 200);
                }, 100);
            }
            
            // Add some interactivity when page loads
            document.addEventListener('DOMContentLoaded', function() {
                // Add hover effects to features
                const features = document.querySelectorAll('.feature');
                features.forEach(feature => {
                    feature.addEventListener('mouseenter', function() {
                        this.style.backgroundColor = '#f1f3f4';
                    });
                    
                    feature.addEventListener('mouseleave', function() {
                        this.style.backgroundColor = '#f8f9fa';
                    });
                });
                
                // Add click effect to button
                const btn = document.querySelector('.btn');
                if (btn) {
                    btn.addEventListener('click', function() {
                        this.style.transform = 'scale(0.95)';
                        setTimeout(() => {
                            this.style.transform = 'scale(1)';
                        }, 150);
                    });
                }
            });
            """;
    }

    private GenerateResponse createFallbackResponse(String prompt) {
        return new GenerateResponse(
                createFallbackHtml(prompt),
                createFallbackCss(),
                createFallbackJs()
        );
    }
}