package com.Jadhav.WebCraft.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GenerateResponse {

    @JsonProperty("html")
    private String html;

    @JsonProperty("css")
    private String css;

    @JsonProperty("js")
    private String js;

    // Default constructor
    public GenerateResponse() {}

    // Constructor with all fields
    public GenerateResponse(String html, String css, String js) {
        this.html = html;
        this.css = css;
        this.js = js;
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

    @Override
    public String toString() {
        return "GenerateResponse{" +
                "html='" + (html != null ? html.substring(0, Math.min(50, html.length())) + "..." : "null") + '\'' +
                ", css='" + (css != null ? css.substring(0, Math.min(30, css.length())) + "..." : "null") + '\'' +
                ", js='" + (js != null ? js.substring(0, Math.min(30, js.length())) + "..." : "null") + '\'' +
                '}';
    }
}