package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enables a simple memory-based message broker to carry messages back to the
        // client
        // on destinations prefixed with "/topic"
        config.enableSimpleBroker("/topic");

        // Prefix used for messages bound for methods annotated with @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register connection endpoint, enabling SockJS fallback options
        // We set setAllowedOrigins("*") or specifically "http://localhost:5173" to
        // prevent CORS blocks
        registry.addEndpoint("/ws-campus")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
