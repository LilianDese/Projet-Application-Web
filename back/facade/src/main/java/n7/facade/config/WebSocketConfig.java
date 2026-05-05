package n7.facade.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
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

    private final WebSocketPresenceTracker presenceTracker;

    public WebSocketConfig(WebSocketPresenceTracker presenceTracker) {
        this.presenceTracker = presenceTracker;
    }

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

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String rawJoueurId = accessor.getFirstNativeHeader("joueurId");
                    Long joueurId = parseLong(rawJoueurId);
                    presenceTracker.registerSession(accessor.getSessionId(), joueurId);
                }
                return message;
            }
        });
    }

    private static Long parseLong(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        try {
            return Long.valueOf(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
