package com.Jadhav.WebCraft.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GenerateRequest {

    @NotNull(message="Prompt is required")
    @NotBlank(message="Prompt Can't be empty")
    @JsonProperty("prompt")

    private String prompt;

    public GenerateRequest()
    {}
    public GenerateRequest(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
