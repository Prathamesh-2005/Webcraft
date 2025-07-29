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
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

//@CrossOrigin(origins="http://localhost:5173")
@RestController
@RequestMapping("/")
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
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Prompt is required"));
            }

            GenerateResponse response = websiteGeneratorService.generateWebsite(request.getPrompt());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid response format from AI: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Invalid response format from AI", e.getMessage()));

        } catch (Exception e) {
            logger.error("Generation error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to generate website", e.getMessage()));
        }
    }

    @RequestMapping(value = "/generate", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok().build();
    }
}
