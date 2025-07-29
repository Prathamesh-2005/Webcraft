package com.Jadhav.WebCraft.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GenerateResponse {

    @JsonProperty("html")
    private String html;

    @JsonProperty("css")
    private String css;

    @JsonProperty("js")
    private String js;

    public GenerateResponse() {}

    public GenerateResponse(String html, String css, String js) {
        this.html = html;
        this.css = css;
        this.js = js;
    }

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
}
