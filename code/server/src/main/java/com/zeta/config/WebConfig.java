package com.zeta.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Value("${zeta.cors.allowed-origins}")
  private String allowedOrigins;

  private final UploadProperties uploadProperties;

  public WebConfig(UploadProperties uploadProperties) {
    this.uploadProperties = uploadProperties;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String[] origins = allowedOrigins.split(",");
    registry
        .addMapping("/api/**")
        .allowedOriginPatterns(origins)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
    registry
        .addMapping("/uploads/**")
        .allowedOriginPatterns(origins)
        .allowedMethods("GET")
        .allowedHeaders("*")
        .allowCredentials(false);
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Path storageRoot = Paths.get(uploadProperties.getStorageDir()).toAbsolutePath().normalize();
    String location = storageRoot.toUri().toString();
    registry
        .addResourceHandler(uploadProperties.getPublicPathPrefix() + "/**")
        .addResourceLocations(location.endsWith("/") ? location : location + "/");
  }
}
