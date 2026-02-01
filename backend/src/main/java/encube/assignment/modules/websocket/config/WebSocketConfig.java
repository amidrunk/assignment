package encube.assignment.modules.websocket.config;

import encube.assignment.modules.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig  {

    private final WebSocketService webSocketService;

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        return new SimpleUrlHandlerMapping(Map.of(
                "/ws", webSocketHandler()
        ));
    }

    @Bean
    public WebSocketHandler webSocketHandler() {
        return webSocketService::connectWebSocket;
    }
}
