package n7.facade.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Active le broker STOMP in-memory.
 *
 * Endpoints :
 *   ws://<host>/ws  (raw WebSocket)
 *
 * Préfixe applicatif : /app  → routé vers les @MessageMapping
 * Préfixe topic      : /topic → broker in-memory (publish/subscribe)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Active le broker simple pour /topic
        config.enableSimpleBroker("/topic");
        // Préfixe pour les messages entrants destinés aux @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Point de connexion WebSocket — avec fallback SockJS pour les vieux navigateurs
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
