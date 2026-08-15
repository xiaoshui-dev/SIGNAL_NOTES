package com.signalnotes.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String uploadDir;
    public WebConfig(@Value("${app.upload-dir}") String uploadDir) { this.uploadDir = uploadDir; }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations(Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString());
    }
}
