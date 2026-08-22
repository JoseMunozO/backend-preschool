package com.preschool.backendpreschool.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String uploadDir;
    private final String publicUrlPrefix;

    public WebMvcConfig(
            @Value("${app.storage.upload-dir}") String uploadDir,
            @Value("${app.storage.public-url-prefix}") String publicUrlPrefix
    ) {
        this.uploadDir = uploadDir;
        this.publicUrlPrefix = publicUrlPrefix;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + Paths.get(uploadDir).toAbsolutePath().normalize() + "/";
        registry.addResourceHandler(publicUrlPrefix + "/**")
                .addResourceLocations(location);
    }
}
