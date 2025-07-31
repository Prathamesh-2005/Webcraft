package com.Jadhav.WebCraft.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GenerateRequest {

    @NotNull(message = "Prompt is required")
    @NotBlank(message = "Prompt cannot be empty")
    @JsonProperty("prompt")
    private String prompt;

    // Default constructor
    public GenerateRequest() {}

    // Constructor
    public GenerateRequest(String prompt) {
        this.prompt = prompt;
    }

    // Getter and Setter
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @Override
    public String toString() {
        return "GenerateRequest{" +
                "prompt='" + (prompt != null ? prompt.substring(0, Math.min(100, prompt.length())) + "..." : "null") + '\'' +
                '}';
    }
}