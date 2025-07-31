package com.Jadhav.WebCraft.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DeployRequest {

    @NotNull(message = "HTML content is required")
    @NotBlank(message = "HTML content cannot be empty")
    @JsonProperty("html")
    private String html;

    @JsonProperty("css")
    private String css;

    @JsonProperty("js")
    private String js;

    @NotNull(message = "Project name is required")
    @NotBlank(message = "Project name cannot be empty")
    @JsonProperty("projectName")
    private String projectName;

    // Default constructor
    public DeployRequest() {}

    // Constructor with all fields
    public DeployRequest(String html, String css, String js, String projectName) {
        this.html = html;
        this.css = css;
        this.js = js;
        this.projectName = projectName;
    }

    // Getters and Setters
    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getCss() {
        return css;
    }

    public void setCss(String css) {
        this.css = css;
    }

    public String getJs() {
        return js;
    }

    public void setJs(String js) {
        this.js = js;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public String toString() {
        return "DeployRequest{" +
                "html='" + (html != null ? html.substring(0, Math.min(50, html.length())) + "..." : "null") + '\'' +
                ", css='" + (css != null ? css.substring(0, Math.min(30, css.length())) + "..." : "null") + '\'' +
                ", js='" + (js != null ? js.substring(0, Math.min(30, js.length())) + "..." : "null") + '\'' +
                ", projectName='" + projectName + '\'' +
                '}';
    }
}