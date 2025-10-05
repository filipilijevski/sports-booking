package com.ttclub.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** expose physical “uploads/” directory under /uploads/** */
    @Override public void addResourceHandlers(ResourceHandlerRegistry reg) {
        reg.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
