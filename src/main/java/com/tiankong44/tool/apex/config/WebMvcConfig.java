package com.tiankong44.tool.apex.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatterForFieldType(LocalDate.class, new LocalDateFormatter());
    }
    
    private static class LocalDateFormatter implements org.springframework.format.Formatter<LocalDate> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        @Override
        public LocalDate parse(String text, java.util.Locale locale) {
            return LocalDate.parse(text, FORMATTER);
        }
        
        @Override
        public String print(LocalDate object, java.util.Locale locale) {
            return object.format(FORMATTER);
        }
    }
}