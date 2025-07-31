package com.Jadhav.WebCraft.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Base64;
import java.util.Map;
import java.util.List;
import java.nio.charset.StandardCharsets;

@Service
public class VercelDeploymentService {
    private static final Logger logger = LoggerFactory.getLogger(VercelDeploymentService.class);

    @Value("${vercel.token}")
    private String vercelToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String deployToVercel(String htmlContent, String cssContent, String jsContent, String projectName) {
        try {
            logger.info("Starting deployment to Vercel for project: {}", projectName);

            // Validate inputs
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                throw new IllegalArgumentException("HTML content cannot be empty");
            }

            if (projectName == null || projectName.trim().isEmpty()) {
                throw new IllegalArgumentException("Project name cannot be empty");
            }

            if (vercelToken == null || vercelToken.trim().isEmpty()) {
                throw new IllegalArgumentException("Vercel token is not configured");
            }

            // Use v6 API which is more reliable for static deployments
            ObjectNode deploymentPayload = createStaticDeploymentPayload(htmlContent, cssContent, jsContent, projectName);
            String deploymentUrl = callVercelAPI(deploymentPayload, "https://api.vercel.com/v6/deployments");
            logger.info("Successfully deployed: {}", deploymentUrl);
            return deploymentUrl;

        } catch (IllegalArgumentException e) {
            logger.error("Invalid deployment parameters: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to deploy to Vercel: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to deploy to Vercel: " + e.getMessage(), e);
        }
    }

    private ObjectNode createStaticDeploymentPayload(String htmlContent, String cssContent, String jsContent, String projectName) {
        ObjectNode payload = objectMapper.createObjectNode();

        // Set basic deployment info
        String sanitizedName = sanitizeProjectName(projectName);
        payload.put("name", sanitizedName);
        payload.put("target", "production");
        payload.put("public", true); // Make it publicly accessible

        logger.debug("Using sanitized project name: {}", sanitizedName);

        // Ensure HTML content is properly formatted
        String processedHtmlContent = processHtmlContent(htmlContent, cssContent, jsContent);

        // Create files array
        ArrayNode files = objectMapper.createArrayNode();

        // Add HTML file
        ObjectNode htmlFile = objectMapper.createObjectNode();
        htmlFile.put("file", "index.html");
        htmlFile.put("data", Base64.getEncoder().encodeToString(processedHtmlContent.getBytes(StandardCharsets.UTF_8)));
        files.add(htmlFile);
        logger.debug("Added HTML file (size: {} chars)", processedHtmlContent.length());

        // Add CSS file if exists
        if (cssContent != null && !cssContent.trim().isEmpty()) {
            ObjectNode cssFile = objectMapper.createObjectNode();
            cssFile.put("file", "styles.css");
            cssFile.put("data", Base64.getEncoder().encodeToString(cssContent.getBytes(StandardCharsets.UTF_8)));
            files.add(cssFile);
            logger.debug("Added CSS file (size: {} chars)", cssContent.length());
        }

        // Add JS file if exists
        if (jsContent != null && !jsContent.trim().isEmpty()) {
            ObjectNode jsFile = objectMapper.createObjectNode();
            jsFile.put("file", "script.js");
            jsFile.put("data", Base64.getEncoder().encodeToString(jsContent.getBytes(StandardCharsets.UTF_8)));
            files.add(jsFile);
            logger.debug("Added JS file (size: {} chars)", jsContent.length());
        }

        payload.set("files", files);

        // Add meta for static deployment (no framework needed for v6)
        ObjectNode meta = objectMapper.createObjectNode();
        meta.put("githubDeployment", "0");
        payload.set("meta", meta);

        logger.debug("Created static deployment payload with {} files", files.size());
        return payload;
    }

    private String processHtmlContent(String htmlContent, String cssContent, String jsContent) {
        StringBuilder processedHtml = new StringBuilder();

        // Ensure content starts with proper DOCTYPE
        String content = htmlContent.trim();
        if (!content.toLowerCase().startsWith("<!doctype")) {
            processedHtml.append("<!DOCTYPE html>\n");
        }
        processedHtml.append(content);

        // Add CSS link if CSS content exists and not already referenced
        if (cssContent != null && !cssContent.trim().isEmpty() &&
                !htmlContent.toLowerCase().contains("styles.css") &&
                !htmlContent.toLowerCase().contains("<style")) {

            String cssLink = "\n    <link rel=\"stylesheet\" href=\"styles.css\">";
            int headEndIndex = processedHtml.toString().toLowerCase().indexOf("</head>");
            if (headEndIndex != -1) {
                processedHtml.insert(headEndIndex, cssLink);
                logger.debug("Added CSS link to HTML");
            } else {
                // Add basic head structure if missing
                int htmlIndex = processedHtml.toString().toLowerCase().indexOf("<html");
                if (htmlIndex != -1) {
                    int htmlTagEnd = processedHtml.toString().indexOf(">", htmlIndex);
                    if (htmlTagEnd != -1) {
                        String headSection = "\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" + cssLink + "\n</head>";
                        processedHtml.insert(htmlTagEnd + 1, headSection);
                        logger.debug("Added complete head section with CSS");
                    }
                }
            }
        }

        // Add JS script if JS content exists and not already referenced
        if (jsContent != null && !jsContent.trim().isEmpty() &&
                !htmlContent.toLowerCase().contains("script.js") &&
                !htmlContent.toLowerCase().contains("<script")) {

            String jsScript = "\n    <script src=\"script.js\"></script>";
            int bodyEndIndex = processedHtml.toString().toLowerCase().lastIndexOf("</body>");
            if (bodyEndIndex != -1) {
                processedHtml.insert(bodyEndIndex, jsScript);
                logger.debug("Added JS script to HTML");
            } else {
                // Add before closing html tag if no body tag
                int htmlEndIndex = processedHtml.toString().toLowerCase().lastIndexOf("</html>");
                if (htmlEndIndex != -1) {
                    processedHtml.insert(htmlEndIndex, jsScript + "\n");
                    logger.debug("Added JS script before </html>");
                }
            }
        }

        return processedHtml.toString();
    }

    private String callVercelAPI(ObjectNode payload, String apiUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(vercelToken);
        headers.set("User-Agent", "WebCraft-Static-Deployment/1.0");

        String payloadString = payload.toString();
        logger.debug("Sending payload to Vercel (size: {} chars)", payloadString.length());

        // Log payload structure for debugging (without sensitive data)
        logger.debug("Payload structure - files count: {}, has public flag: {}",
                payload.get("files").size(), payload.has("public"));

        HttpEntity<String> entity = new HttpEntity<>(payloadString, headers);

        try {
            logger.debug("Calling Vercel API: {}", apiUrl);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                Map<String, Object> responseBody = response.getBody();

                if (responseBody == null) {
                    throw new RuntimeException("Empty response from Vercel API");
                }

                logger.debug("Vercel response keys: {}", responseBody.keySet());

                String deploymentId = (String) responseBody.get("id");
                if (deploymentId == null) {
                    throw new RuntimeException("No deployment ID received from Vercel");
                }

                logger.info("Deployment created with ID: {}", deploymentId);

                // Check for immediate URL
                String immediateUrl = (String) responseBody.get("url");
                if (immediateUrl != null && !immediateUrl.isEmpty()) {
                    String fullUrl = immediateUrl.startsWith("https://") ? immediateUrl : "https://" + immediateUrl;
                    logger.info("Immediate URL available: {}", fullUrl);

                    // Wait a moment for deployment to be ready
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    return fullUrl;
                }

                // Check for alias
                @SuppressWarnings("unchecked")
                List<String> aliases = (List<String>) responseBody.get("alias");
                if (aliases != null && !aliases.isEmpty()) {
                    String aliasUrl = "https://" + aliases.get(0);
                    logger.info("Alias URL available: {}", aliasUrl);
                    return aliasUrl;
                }

                // Wait for deployment to complete
                return waitForDeploymentAndGetUrl(deploymentId);

            } else {
                logger.error("Vercel API returned status: {} with body: {}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Vercel API returned status: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            logger.error("Vercel API client error: {} - Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Vercel API client error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException e) {
            logger.error("Vercel API server error: {} - Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Vercel API server error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.error("Error calling Vercel API: {}", e.getMessage(), e);
            throw new RuntimeException("Error calling Vercel API: " + e.getMessage(), e);
        }
    }

    private String waitForDeploymentAndGetUrl(String deploymentId) {
        String url = "https://api.vercel.com/v6/deployments/" + deploymentId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(vercelToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        int maxRetries = 12; // 12 * 5 seconds = 1 minute max
        int retryCount = 0;

        logger.info("Waiting for deployment {} to complete...", deploymentId);

        while (retryCount < maxRetries) {
            try {
                Thread.sleep(5000); // Wait 5 seconds between checks

                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    Map<String, Object> responseBody = response.getBody();
                    if (responseBody == null) {
                        retryCount++;
                        continue;
                    }

                    String state = (String) responseBody.get("state");
                    logger.debug("Deployment {} state: {} (attempt {}/{})", deploymentId, state, retryCount + 1, maxRetries);

                    if ("READY".equals(state)) {
                        String deploymentUrl = (String) responseBody.get("url");
                        if (deploymentUrl != null) {
                            String fullUrl = deploymentUrl.startsWith("https://") ? deploymentUrl : "https://" + deploymentUrl;
                            logger.info("Deployment completed successfully: {}", fullUrl);
                            return fullUrl;
                        } else {
                            // Use deployment ID to construct URL
                            String constructedUrl = "https://" + deploymentId + ".vercel.app";
                            logger.info("Using constructed URL: {}", constructedUrl);
                            return constructedUrl;
                        }
                    } else if ("ERROR".equals(state) || "CANCELED".equals(state)) {
                        logger.error("Deployment failed with state: {}", state);
                        // Log more details if available
                        Object errorInfo = responseBody.get("error");
                        if (errorInfo != null) {
                            logger.error("Deployment error details: {}", errorInfo);
                        }
                        throw new RuntimeException("Deployment failed with state: " + state);
                    }
                    // Continue waiting for other states (BUILDING, INITIALIZING, QUEUED, etc.)
                }

                retryCount++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Deployment polling interrupted");
                throw new RuntimeException("Deployment polling interrupted", e);
            } catch (Exception e) {
                retryCount++;
                logger.warn("Error checking deployment status (attempt {}/{}): {}", retryCount, maxRetries, e.getMessage());

                // If we keep getting errors, don't wait too long
                if (retryCount >= 3) {
                    logger.warn("Multiple consecutive errors, reducing wait time");
                }
            }
        }

        // Return constructed URL as fallback
        String fallbackUrl = "https://" + deploymentId + ".vercel.app";
        logger.warn("Deployment status check timeout. Using fallback URL: {}", fallbackUrl);
        return fallbackUrl;
    }

    private String sanitizeProjectName(String projectName) {
        if (projectName == null) {
            return "webcraft-project-" + System.currentTimeMillis();
        }

        String sanitized = projectName.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "")
                .trim();

        if (sanitized.length() < 3) {
            sanitized = "webcraft-" + sanitized + "-" + System.currentTimeMillis();
        }

        if (sanitized.length() > 63) {
            sanitized = sanitized.substring(0, 63);
            if (sanitized.endsWith("-")) {
                sanitized = sanitized.substring(0, 62);
            }
        }

        return sanitized;
    }
}