package com.Jadhav.WebCraft.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeployResponse {

    @JsonProperty("html")
    private String html;

    @JsonProperty("css")
    private String css;

    @JsonProperty("js")
    private String js;

    @JsonProperty("deploymentUrl")
    private String deploymentUrl;

    @JsonProperty("projectName")
    private String projectName;

    @JsonProperty("deployed")
    private boolean deployed;

    // Default constructor
    public DeployResponse() {}

    // Constructor with all fields
    public DeployResponse(String html, String css, String js, String deploymentUrl, String projectName, boolean deployed) {
        this.html = html;
        this.css = css;
        this.js = js;
        this.deploymentUrl = deploymentUrl;
        this.projectName = projectName;
        this.deployed = deployed;
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

    public String getDeploymentUrl() {
        return deploymentUrl;
    }

    public void setDeploymentUrl(String deploymentUrl) {
        this.deploymentUrl = deploymentUrl;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public boolean isDeployed() {
        return deployed;
    }

    public void setDeployed(boolean deployed) {
        this.deployed = deployed;
    }

    @Override
    public String toString() {
        return "DeployResponse{" +
                "html='" + (html != null ? html.substring(0, Math.min(50, html.length())) + "..." : "null") + '\'' +
                ", css='" + (css != null ? css.substring(0, Math.min(30, css.length())) + "..." : "null") + '\'' +
                ", js='" + (js != null ? js.substring(0, Math.min(30, js.length())) + "..." : "null") + '\'' +
                ", deploymentUrl='" + deploymentUrl + '\'' +
                ", projectName='" + projectName + '\'' +
                ", deployed=" + deployed +
                '}';
    }
}