package com.Jadhav.WebCraft.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.io.IOException;

@Service
public class NetlifyDeploymentService {
    private static final Logger logger = LoggerFactory.getLogger(NetlifyDeploymentService.class);

    @Value("${netlify.token:}")
    private String netlifyToken;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Main deployment method that deploys HTML, CSS, and JS to Netlify
     */
    public String deployToNetlify(String html, String css, String js, String projectName) {
        try {
            // Validate inputs
            if (html == null || html.trim().isEmpty()) {
                throw new IllegalArgumentException("HTML content cannot be empty");
            }

            if (netlifyToken == null || netlifyToken.trim().isEmpty()) {
                throw new IllegalStateException("Netlify token is not configured");
            }

            if (projectName == null || projectName.trim().isEmpty()) {
                projectName = "webcraft-site-" + System.currentTimeMillis();
            }

            logger.info("Starting deployment for project: {}", projectName);

            // Create ZIP file with all assets
            byte[] zipData = createDeploymentZip(html, css, js);

            // Deploy to Netlify
            String deploymentUrl = deployZipToNetlify(zipData, projectName);

            if (deploymentUrl != null) {
                logger.info("Deployment successful: {}", deploymentUrl);
                return deploymentUrl;
            } else {
                throw new RuntimeException("Deployment failed - no URL returned from Netlify");
            }

        } catch (Exception e) {
            logger.error("Deployment failed for project {}: {}", projectName, e.getMessage(), e);
            throw new RuntimeException("Failed to deploy to Netlify: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a ZIP file containing HTML, CSS, and JS files
     */
    private byte[] createDeploymentZip(String html, String css, String js) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Process HTML to include CSS and JS links
            String processedHtml = processHtmlWithAssets(html, css, js);

            // Add index.html
            ZipEntry htmlEntry = new ZipEntry("index.html");
            zos.putNextEntry(htmlEntry);
            zos.write(processedHtml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            logger.info("Added index.html to ZIP");

            // Add styles.css if CSS is provided
            if (css != null && !css.trim().isEmpty()) {
                ZipEntry cssEntry = new ZipEntry("styles.css");
                zos.putNextEntry(cssEntry);
                zos.write(css.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                logger.info("Added styles.css to ZIP ({} bytes)", css.length());
            }

            // Add script.js if JS is provided
            if (js != null && !js.trim().isEmpty()) {
                ZipEntry jsEntry = new ZipEntry("script.js");
                zos.putNextEntry(jsEntry);
                zos.write(js.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                logger.info("Added script.js to ZIP ({} bytes)", js.length());
            }

            // Add a simple _redirects file for SPA support (optional)
            ZipEntry redirectsEntry = new ZipEntry("_redirects");
            zos.putNextEntry(redirectsEntry);
            zos.write("/*    /index.html   200".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.flush();
        }

        byte[] zipData = baos.toByteArray();
        logger.info("Created deployment ZIP with size: {} bytes", zipData.length);
        return zipData;
    }

    /**
     * Processes HTML to ensure CSS and JS files are properly linked
     */
    private String processHtmlWithAssets(String html, String css, String js) {
        if (html == null || html.trim().isEmpty()) {
            return html;
        }

        String processedHtml = html;
        boolean hasCssLink = false;
        boolean hasJsLink = false;

        // Check if HTML already has CSS link
        if (css != null && !css.trim().isEmpty()) {
            hasCssLink = processedHtml.contains("styles.css") ||
                    processedHtml.contains("<link") && processedHtml.contains("stylesheet");

            if (!hasCssLink) {
                // Find </head> tag and insert CSS link before it
                if (processedHtml.contains("</head>")) {
                    String cssLink = "    <link rel=\"stylesheet\" href=\"styles.css\">\n";
                    processedHtml = processedHtml.replace("</head>", cssLink + "</head>");
                    logger.info("Added CSS link to HTML");
                } else {
                    // If no </head> tag, add it after <head> or at the beginning
                    if (processedHtml.contains("<head>")) {
                        String cssLink = "\n    <link rel=\"stylesheet\" href=\"styles.css\">";
                        processedHtml = processedHtml.replace("<head>", "<head>" + cssLink);
                    }
                }
            }
        }

        // Check if HTML already has JS script
        if (js != null && !js.trim().isEmpty()) {
            hasJsLink = processedHtml.contains("script.js") ||
                    processedHtml.contains("<script") && processedHtml.contains("src=");

            if (!hasJsLink) {
                // Find </body> tag and insert JS script before it
                if (processedHtml.contains("</body>")) {
                    String jsScript = "    <script src=\"script.js\"></script>\n";
                    processedHtml = processedHtml.replace("</body>", jsScript + "</body>");
                    logger.info("Added JS script to HTML");
                } else {
                    // If no </body> tag, add it before </html>
                    if (processedHtml.contains("</html>")) {
                        String jsScript = "\n    <script src=\"script.js\"></script>\n";
                        processedHtml = processedHtml.replace("</html>", jsScript + "</html>");
                    }
                }
            }
        }

        return processedHtml;
    }

    /**
     * Deploys the ZIP file to Netlify
     */
    private String deployZipToNetlify(byte[] zipData, String projectName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/zip"));
            headers.setBearerAuth(netlifyToken);
            headers.set("Cache-Control", "no-cache");
            headers.set("User-Agent", "WebCraft/1.0");

            // Optional: Set custom site name
            if (projectName != null && !projectName.trim().isEmpty()) {
                headers.set("Netlify-Site-Name", projectName);
            }

            HttpEntity<byte[]> entity = new HttpEntity<>(zipData, headers);

            logger.info("Sending deployment request to Netlify...");
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.netlify.com/api/v1/sites", entity, Map.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String url = (String) responseBody.get("url");
                String siteId = (String) responseBody.get("id");

                if (url != null) {
                    String httpsUrl = url.replace("http://", "https://");
                    logger.info("Site created successfully with ID: {}, URL: {}", siteId, httpsUrl);

                    // Wait a moment for deployment to complete
                    waitForDeployment(httpsUrl);

                    return httpsUrl;
                }
            }

            logger.error("Unexpected response from Netlify: {}", response.getStatusCode());
            return null;

        } catch (HttpClientErrorException e) {
            logger.error("HTTP error during deployment: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Netlify API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Deployment error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to deploy to Netlify: " + e.getMessage(), e);
        }
    }

    /**
     * Waits for the deployment to be accessible
     */
    private void waitForDeployment(String url) {
        int maxAttempts = 10;
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                Thread.sleep(2000); // Wait 2 seconds between attempts

                if (testSiteAccessibility(url)) {
                    logger.info("Deployment is accessible after {} attempts", attempt + 1);
                    return;
                }

                attempt++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Deployment wait interrupted");
                return;
            }
        }

        logger.warn("Deployment may not be fully accessible yet after {} attempts", maxAttempts);
    }

    // === DIAGNOSTIC METHODS (existing code) ===

    public Map<String, Object> runDiagnostics() {
        Map<String, Object> diagnostics = new HashMap<>();

        try {
            // Validate token exists
            if (netlifyToken == null || netlifyToken.trim().isEmpty()) {
                diagnostics.put("tokenValid", false);
                diagnostics.put("error", "Netlify token is not configured");
                return diagnostics;
            }

            // Test 1: Check token validity
            boolean tokenValid = testTokenValidity();
            diagnostics.put("tokenValid", tokenValid);

            if (tokenValid) {
                // Test 2: Check account info
                Map<String, Object> accountInfo = getAccountInfo();
                diagnostics.put("accountInfo", accountInfo);

                // Test 3: List existing sites
                Object sites = listSites();
                diagnostics.put("existingSites", sites);

                // Test 4: Test minimal deployment
                String testUrl = testMinimalDeployment();
                diagnostics.put("testDeploymentUrl", testUrl);

                if (testUrl != null) {
                    // Test 5: Check if deployed site is accessible
                    boolean siteAccessible = testSiteAccessibility(testUrl);
                    diagnostics.put("siteAccessible", siteAccessible);

                    if (!siteAccessible) {
                        // Test 6: Get detailed error info
                        Map<String, Object> errorDetails = getDetailedErrorInfo(testUrl);
                        diagnostics.put("errorDetails", errorDetails);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Diagnostic error: {}", e.getMessage(), e);
            diagnostics.put("diagnosticError", e.getMessage());
        }

        return diagnostics;
    }

    private boolean testTokenValidity() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(netlifyToken);
            headers.set("User-Agent", "WebCraft-Diagnostic/1.0");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.netlify.com/api/v1/user", HttpMethod.GET, entity, Map.class);

            boolean isValid = response.getStatusCode() == HttpStatus.OK;
            logger.info("Token validity test: {}", isValid ? "PASSED" : "FAILED");
            return isValid;

        } catch (HttpClientErrorException e) {
            logger.error("Token validity test failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            logger.error("Token validity test error: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, Object> getAccountInfo() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(netlifyToken);
            headers.set("User-Agent", "WebCraft-Diagnostic/1.0");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.netlify.com/api/v1/user", HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> accountInfo = response.getBody();
                logger.info("Account info retrieved successfully");
                return accountInfo;
            }

        } catch (Exception e) {
            logger.error("Failed to get account info: {}", e.getMessage());
        }

        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Object listSites() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(netlifyToken);
            headers.set("User-Agent", "WebCraft-Diagnostic/1.0");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(
                    "https://api.netlify.com/api/v1/sites", HttpMethod.GET, entity, List.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Sites list retrieved successfully");
                return response.getBody();
            }

        } catch (Exception e) {
            logger.error("Failed to list sites: {}", e.getMessage());
        }

        return "Failed to retrieve";
    }

    private String testMinimalDeployment() {
        try {
            // Create the most minimal valid HTML possible
            String minimalHtml = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>Test</title>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <h1>Test Deployment</h1>\n" +
                    "</body>\n" +
                    "</html>";

            // Create ZIP
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                ZipEntry entry = new ZipEntry("index.html");
                zos.putNextEntry(entry);
                zos.write(minimalHtml.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/zip"));
            headers.setBearerAuth(netlifyToken);
            headers.set("Cache-Control", "no-cache");
            headers.set("User-Agent", "WebCraft-Diagnostic/1.0");

            HttpEntity<byte[]> entity = new HttpEntity<>(baos.toByteArray(), headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.netlify.com/api/v1/sites", entity, Map.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String url = (String) responseBody.get("url");
                if (url != null) {
                    String httpsUrl = url.replace("http://", "https://");
                    logger.info("Test deployment successful: {}", httpsUrl);
                    return httpsUrl;
                }
            }

        } catch (HttpClientErrorException e) {
            logger.error("Test deployment failed with HTTP error: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Test deployment failed: {}", e.getMessage(), e);
        }

        return null;
    }

    private boolean testSiteAccessibility(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            headers.set("Accept-Language", "en-US,en;q=0.5");
            headers.set("Accept-Encoding", "gzip, deflate");
            headers.set("Connection", "keep-alive");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            boolean accessible = response.getStatusCode().is2xxSuccessful();
            logger.info("Site accessibility test for {}: {} (Status: {})",
                    url, accessible ? "PASSED" : "FAILED", response.getStatusCode());

            return accessible;

        } catch (HttpClientErrorException e) {
            logger.error("Site accessibility test failed for {}: {} - {}",
                    url, e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (ResourceAccessException e) {
            logger.error("Site accessibility test - network error for {}: {}", url, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Site accessibility test error for {}: {}", url, e.getMessage());
            return false;
        }
    }

    private Map<String, Object> getDetailedErrorInfo(String url) {
        Map<String, Object> errorInfo = new HashMap<>();

        try {
            // Try different request methods to understand the error
            String[] methods = {"GET", "HEAD", "OPTIONS"};

            for (String method : methods) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("User-Agent", "WebCraft-Diagnostic/1.0");

                    HttpEntity<String> entity = new HttpEntity<>(headers);

                    ResponseEntity<String> response = restTemplate.exchange(
                            url, HttpMethod.valueOf(method), entity, String.class);

                    errorInfo.put(method + "_status", response.getStatusCode().value());
                    errorInfo.put(method + "_headers", response.getHeaders().toSingleValueMap());

                } catch (HttpClientErrorException e) {
                    errorInfo.put(method + "_error_status", e.getStatusCode().value());
                    errorInfo.put(method + "_error_body", e.getResponseBodyAsString());
                    errorInfo.put(method + "_error_headers", e.getResponseHeaders().toSingleValueMap());
                } catch (Exception e) {
                    errorInfo.put(method + "_error", e.getMessage());
                }
            }

            // Try to get site info from Netlify API
            String siteId = extractSiteIdFromUrl(url);
            if (siteId != null) {
                Map<String, Object> siteInfo = getSiteInfo(siteId);
                errorInfo.put("netlify_site_info", siteInfo);
            }

        } catch (Exception e) {
            errorInfo.put("diagnostic_error", e.getMessage());
        }

        return errorInfo;
    }

    private String extractSiteIdFromUrl(String url) {
        try {
            // Extract site ID from Netlify URL format
            String domain = url.replace("https://", "").replace("http://", "");
            String[] parts = domain.split("\\.");
            if (parts.length > 0) {
                String firstPart = parts[0];
                if (firstPart.contains("--")) {
                    return firstPart.split("--")[0];
                }
                return firstPart; // Simple case where the whole first part is the site ID
            }
        } catch (Exception e) {
            logger.error("Failed to extract site ID from URL: {}", url, e);
        }
        return null;
    }

    private Map<String, Object> getSiteInfo(String siteId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(netlifyToken);
            headers.set("User-Agent", "WebCraft-Diagnostic/1.0");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.netlify.com/api/v1/sites/" + siteId, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }

        } catch (Exception e) {
            logger.error("Failed to get site info for {}: {}", siteId, e.getMessage());
        }

        return new HashMap<>();
    }

    public void printDiagnosticReport() {
        Map<String, Object> diagnostics = runDiagnostics();

        logger.info("=== NETLIFY DEPLOYMENT DIAGNOSTIC REPORT ===");
        diagnostics.forEach((key, value) -> {
            logger.info("{}: {}", key, value);
        });
        logger.info("=== END DIAGNOSTIC REPORT ===");
    }
}