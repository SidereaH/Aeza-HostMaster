package aeza.hostmaster.checks.web;

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
        config.enableSimpleBroker("/topic"); // публикации (клиент подписывается)
        config.setApplicationDestinationPrefixes("/app"); // сообщения от клиента (нам не нужно, но пусть будет)
    }


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Обычный WebSocket (для Postman, wscat и т.п.)
        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // SockJS для браузеров
        registry
                .addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }


}
