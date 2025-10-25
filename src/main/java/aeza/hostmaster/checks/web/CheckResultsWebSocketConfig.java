package aeza.hostmaster.checks.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class CheckResultsWebSocketConfig implements WebSocketConfigurer {

    private final CheckResultsWebSocketHandler handler;

    public CheckResultsWebSocketConfig(CheckResultsWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/checks/socket/**")
                .setAllowedOriginPatterns("*");
    }
}
