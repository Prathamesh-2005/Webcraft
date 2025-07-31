package com.Jadhav.WebCraft.Controller;

import com.Jadhav.WebCraft.Service.WebsiteGeneratorService;
import com.Jadhav.WebCraft.dto.ErrorResponse;
import com.Jadhav.WebCraft.dto.GenerateRequest;
import com.Jadhav.WebCraft.dto.GenerateResponse;
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
@CrossOrigin(origins ="http://localhost:5173")
public class WebsiteGeneratorController {
    private static final Logger logger = LoggerFactory.getLogger(WebsiteGeneratorController.class);

    @Autowired
    private WebsiteGeneratorService websiteGeneratorService;

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

    @RequestMapping(value = "/generate", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is running");
    }
}