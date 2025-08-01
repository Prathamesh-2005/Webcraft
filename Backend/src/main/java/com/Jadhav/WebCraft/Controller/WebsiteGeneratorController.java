package com.Jadhav.WebCraft.Controller;

import com.Jadhav.WebCraft.Service.WebsiteGeneratorService;
import com.Jadhav.WebCraft.Service.NetlifyDeploymentService;
import com.Jadhav.WebCraft.dto.ErrorResponse;
import com.Jadhav.WebCraft.dto.GenerateRequest;
import com.Jadhav.WebCraft.dto.GenerateResponse;
import com.Jadhav.WebCraft.dto.DeployRequest;
import com.Jadhav.WebCraft.dto.DeployResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/")  // Keep original path for compatibility
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class WebsiteGeneratorController {
    private static final Logger logger = LoggerFactory.getLogger(WebsiteGeneratorController.class);
    private static final Pattern VALID_PROJECT_NAME = Pattern.compile("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$");
    private static final int MAX_PROJECT_NAME_LENGTH = 63;
    private static final int MIN_PROJECT_NAME_LENGTH = 3;

    @Autowired
    private WebsiteGeneratorService websiteGeneratorService;

    @Autowired
    private NetlifyDeploymentService netlifyDeploymentService;

    @Value("${netlify.token:}")
    private String netlifyToken;

    @PostMapping("/generate")
    public ResponseEntity<?> generateWebsite(
            @Valid @RequestBody GenerateRequest request,
            BindingResult bindingResult) {

        try {
            if (bindingResult.hasErrors()) {
                String errorMsg = bindingResult.getFieldError() != null ?
                        bindingResult.getFieldError().getDefaultMessage() : "Validation failed";
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Validation Error", errorMsg));
            }

            if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Validation Error", "Prompt cannot be empty"));
            }

            // Validate prompt length
            if (request.getPrompt().length() > 5000) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Validation Error", "Prompt is too long (max 5000 characters)"));
            }

            logger.info("Generating website for prompt: {}",
                    request.getPrompt().substring(0, Math.min(100, request.getPrompt().length())));

            GenerateResponse response = websiteGeneratorService.generateWebsite(request.getPrompt());

            if (response == null || response.getHtml() == null || response.getHtml().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse("Generation Error", "Failed to generate valid HTML content"));
            }

            logger.info("Website generated successfully with HTML length: {}", response.getHtml().length());
            return ResponseEntity.ok(response);

        } catch (JsonProcessingException e) {
            logger.error("JSON parsing error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("JSON Parsing Error",
                            "The AI response contains invalid JSON format. Please try again."));

        } catch (IllegalArgumentException e) {
            logger.error("Invalid AI response format: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("AI Response Error",
                            "The AI response is missing required fields. Please try again."));

        } catch (Exception e) {
            logger.error("Unexpected error during generation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Generation Error",
                            "An unexpected error occurred during website generation. Please try again later."));
        }
    }

    @PostMapping("/deploy")
    public ResponseEntity<?> deployWebsite(
            @Valid @RequestBody DeployRequest request,
            BindingResult bindingResult) {

        try {
            logger.info("Deploy endpoint called");

            // Check if Netlify is configured
            if (netlifyToken == null || netlifyToken.trim().isEmpty()) {
                logger.error("Netlify token not configured");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new ErrorResponse("Configuration Error",
                                "Deployment service is not properly configured. Please check server configuration."));
            }

            if (bindingResult.hasErrors()) {
                String errorMsg = bindingResult.getFieldError() != null ?
                        bindingResult.getFieldError().getDefaultMessage() : "Validation failed";
                logger.error("Validation error: {}", errorMsg);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Validation Error", errorMsg));
            }

            // Validate required fields
            if (request.getHtml() == null || request.getHtml().trim().isEmpty()) {
                logger.error("HTML content is missing");
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Validation Error", "HTML content cannot be empty"));
            }

            // Validate HTML content size (Netlify has limits)
            if (request.getHtml().length() > 25 * 1024 * 1024) { // 25MB limit
                logger.error("HTML content too large: {} bytes", request.getHtml().length());
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Validation Error", "HTML content is too large (max 25MB)"));
            }

            if (request.getProjectName() == null || request.getProjectName().trim().isEmpty()) {
                logger.error("Project name is missing");
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Validation Error", "Project name cannot be empty"));
            }

            // Sanitize and validate project name
            String sanitizedProjectName = sanitizeProjectName(request.getProjectName());
            if (!isValidProjectName(sanitizedProjectName)) {
                sanitizedProjectName = generateFallbackProjectName();
            }

            logger.info("Deploying website with project name: {}", sanitizedProjectName);
            logger.info("Content sizes - HTML: {} bytes, CSS: {} bytes, JS: {} bytes",
                    request.getHtml().length(),
                    request.getCss() != null ? request.getCss().length() : 0,
                    request.getJs() != null ? request.getJs().length() : 0);

            String deploymentUrl = netlifyDeploymentService.deployToNetlify(
                    request.getHtml(),
                    request.getCss() != null ? request.getCss() : "",
                    request.getJs() != null ? request.getJs() : "",
                    sanitizedProjectName
            );

            if (deploymentUrl == null || deploymentUrl.trim().isEmpty()) {
                throw new RuntimeException("Deployment failed - no URL returned from Netlify");
            }

            DeployResponse response = new DeployResponse();
            response.setHtml(request.getHtml());
            response.setCss(request.getCss());
            response.setJs(request.getJs());
            response.setDeploymentUrl(deploymentUrl);
            response.setProjectName(sanitizedProjectName);
            response.setDeployed(true);

            logger.info("Website deployed successfully to: {}", deploymentUrl);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid deployment request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid Request", e.getMessage()));

        } catch (Exception e) {
            logger.error("Deployment error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Deployment Error",
                            "Failed to deploy website: " + e.getMessage()));
        }
    }

    @GetMapping("/diagnostics")
    public ResponseEntity<?> runDiagnostics() {
        try {
            logger.info("Running Netlify diagnostics...");
            Map<String, Object> diagnostics = netlifyDeploymentService.runDiagnostics();

            return ResponseEntity.ok(diagnostics);
        } catch (Exception e) {
            logger.error("Diagnostics error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Diagnostics Error",
                            "Failed to run diagnostics: " + e.getMessage()));
        }
    }

    @GetMapping("/deployment-status")
    public ResponseEntity<?> getDeploymentStatus() {
        try {
            boolean isConfigured = netlifyToken != null && !netlifyToken.trim().isEmpty();
            Map<String, Object> status = Map.of(
                    "configured", isConfigured,
                    "service", "Netlify",
                    "message", isConfigured ? "Deployment service is ready" : "Deployment service not configured"
            );

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Status check error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Status Error", "Failed to check deployment status"));
        }
    }

    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With")
                .header("Access-Control-Max-Age", "3600")
                .build();
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        try {
            boolean netlifyConfigured = netlifyToken != null && !netlifyToken.trim().isEmpty();

            Map<String, Object> healthStatus = Map.of(
                    "status", "UP",
                    "timestamp", System.currentTimeMillis(),
                    "services", Map.of(
                            "websiteGenerator", "UP",
                            "netlifyDeployment", netlifyConfigured ? "UP" : "NOT_CONFIGURED"
                    )
            );

            return ResponseEntity.ok(healthStatus);
        } catch (Exception e) {
            logger.error("Health check error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "DOWN", "error", e.getMessage()));
        }
    }

    private String sanitizeProjectName(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            return generateFallbackProjectName();
        }

        // Convert to lowercase and remove invalid characters
        String sanitized = projectName.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")  // Keep only alphanumeric, spaces, and hyphens
                .replaceAll("\\s+", "-")           // Replace spaces with hyphens
                .replaceAll("-+", "-")             // Replace multiple hyphens with single
                .replaceAll("^-+|-+$", "");        // Remove leading/trailing hyphens

        // Ensure length constraints
        if (sanitized.length() > MAX_PROJECT_NAME_LENGTH) {
            sanitized = sanitized.substring(0, MAX_PROJECT_NAME_LENGTH);
            // Remove trailing hyphen if created by truncation
            sanitized = sanitized.replaceAll("-+$", "");
        }

        // If too short or empty after sanitization, generate fallback
        if (sanitized.length() < MIN_PROJECT_NAME_LENGTH) {
            return generateFallbackProjectName();
        }

        return sanitized;
    }

    private boolean isValidProjectName(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            return false;
        }

        return projectName.length() >= MIN_PROJECT_NAME_LENGTH &&
                projectName.length() <= MAX_PROJECT_NAME_LENGTH &&
                VALID_PROJECT_NAME.matcher(projectName).matches();
    }

    private String generateFallbackProjectName() {
        return "webcraft-site-" + System.currentTimeMillis();
    }
}