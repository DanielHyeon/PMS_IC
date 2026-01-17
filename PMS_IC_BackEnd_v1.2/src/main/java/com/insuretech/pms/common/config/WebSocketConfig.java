package com.insuretech.pms.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.allowed-origins:*}")
    private String allowedOrigins;

    /**
     * Register STOMP endpoints for WebSocket connections
     * Frontend connects to /ws/wip for WIP updates
     */
    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/wip")
                .setAllowedOrigins(allowedOrigins.split(","))
                .withSockJS();

        registry.addEndpoint("/ws/wip")
                .setAllowedOrigins(allowedOrigins.split(","));
    }

    /**
     * Configure message broker for real-time communication
     * - /app/* routes messages to @MessageMapping methods
     * - /topic/* broadcasts messages to subscribed clients
     * - /queue/* sends messages to specific users
     */
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
}
