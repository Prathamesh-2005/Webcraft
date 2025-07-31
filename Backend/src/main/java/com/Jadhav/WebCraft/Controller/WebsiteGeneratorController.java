package com.Jadhav.WebCraft.Controller;

import com.Jadhav.WebCraft.Service.WebsiteGeneratorService;
import com.Jadhav.WebCraft.Service.VercelDeploymentService;
import com.Jadhav.WebCraft.dto.ErrorResponse;
import com.Jadhav.WebCraft.dto.GenerateRequest;
import com.Jadhav.WebCraft.dto.GenerateResponse;
import com.Jadhav.WebCraft.dto.DeployRequest;
import com.Jadhav.WebCraft.dto.DeployResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("/")
@CrossOrigin(origins = "http://localhost:5173")
public class WebsiteGeneratorController {
    private static final Logger logger = LoggerFactory.getLogger(WebsiteGeneratorController.class);

    @Autowired
    private WebsiteGeneratorService websiteGeneratorService;

    @Autowired
    private VercelDeploymentService vercelDeploymentService;

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

            logger.info("Generating website for prompt: {}", request.getPrompt().substring(0, Math.min(50, request.getPrompt().length())));

            GenerateResponse response = websiteGeneratorService.generateWebsite(request.getPrompt());

            logger.info("Website generated successfully");
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
                            "An unexpected error occurred. Please try again later."));
        }
    }

    @PostMapping("/deploy")
    public ResponseEntity<?> deployWebsite(
            @Valid @RequestBody DeployRequest request,
            BindingResult bindingResult) {

        try {
            // Enhanced validation
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

            if (request.getProjectName() == null || request.getProjectName().trim().isEmpty()) {
                logger.error("Project name is missing");
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Validation Error", "Project name cannot be empty"));
            }

            // Sanitize project name
            String sanitizedProjectName = sanitizeProjectName(request.getProjectName());
            if (sanitizedProjectName.isEmpty()) {
                sanitizedProjectName = "webcraft-project-" + System.currentTimeMillis();
            }

            logger.info("Deploying website with project name: {}", sanitizedProjectName);

            String deploymentUrl = vercelDeploymentService.deployToVercel(
                    request.getHtml(),
                    request.getCss() != null ? request.getCss() : "",
                    request.getJs() != null ? request.getJs() : "",
                    sanitizedProjectName
            );

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

    @RequestMapping(value = "/generate", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleGenerateOptions() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .build();
    }

    @RequestMapping(value = "/deploy", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleDeployOptions() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "POST, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is running");
    }

    private String sanitizeProjectName(String projectName) {
        if (projectName == null) {
            return "";
        }

        // Remove special characters and convert to lowercase
        String sanitized = projectName.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        // Ensure it's not too long
        if (sanitized.length() > 30) {
            sanitized = sanitized.substring(0, 30);
        }

        // Add timestamp to make it unique if it's too short
        if (sanitized.length() < 3) {
            sanitized = "webcraft-" + System.currentTimeMillis();
        }

        return sanitized;
    }
}