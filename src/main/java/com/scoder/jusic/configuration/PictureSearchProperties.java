package com.scoder.jusic.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration for the documented picture-search providers. */
@Component
@ConfigurationProperties(prefix = "jusic.picture-search")
public class PictureSearchProperties {

    private String apiUrl = "https://cn.apihz.cn/api/img/apihzbqb.php";
    private String apiId = "88888888";
    private String apiKey = "88888888";
    private String fallbackUrl = "https://api.xcvts.cn/api/img/qqbqbss";

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getApiId() { return apiId; }
    public void setApiId(String apiId) { this.apiId = apiId; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getFallbackUrl() { return fallbackUrl; }
    public void setFallbackUrl(String fallbackUrl) { this.fallbackUrl = fallbackUrl; }
}
