package com.tiankong44.tool.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import javax.servlet.ServletContext;

@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

//    @Override
//    public void onStartup(ServletContext servletContext) {
//        servletContext.setInitParameter("org.apache.tomcat.websocket.textBufferSize", "524288000"); // 50MB
//        servletContext.setInitParameter("org.apache.tomcat.websocket.binaryBufferSize", "524288000"); // 50MB
//    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(100 * 1024 * 1024); // 16MB
        registration.setSendBufferSizeLimit(100 * 1024 * 1024); // 16MB
    }
}