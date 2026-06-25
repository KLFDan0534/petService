package com.pet.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * SPA forward controller that serves static resources from classpath:/static/
 * and falls back to index.html for client-side routing.
 * Only enabled when pet.frontend.static-enabled is set to true.
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Configuration
@ConditionalOnProperty(name = "pet.frontend.static-enabled", havingValue = "true")
public class SpaForwardController implements WebMvcConfigurer {

    /**
     * Configures resource handlers to serve static files from classpath:/static/
     * and fall back to index.html for SPA client-side routing.
     * @param registry the ResourceHandlerRegistry to configure
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    Resource resource = location.createRelative(resourcePath);
                    if (resource.exists() && resource.isReadable()) {
                        return resource;
                    }
                    Resource fallback = new ClassPathResource("static/index.html");
                    if (fallback.exists()) {
                        return fallback;
                    }
                    return null;
                }
            });
    }
}
