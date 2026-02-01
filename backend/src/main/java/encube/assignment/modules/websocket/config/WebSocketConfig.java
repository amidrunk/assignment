package encube.assignment.modules.websocket.config;

import encube.assignment.modules.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.LinkedHashMap;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig  {

    private final WebSocketService webSocketService;
    @Value("${spring.webflux.base-path:}")
    private String basePath;

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        var urlMap = new LinkedHashMap<String, WebSocketHandler>();

        urlMap.put("/ws", webSocketHandler());

        if (StringUtils.hasText(basePath)) {
            var normalizedBasePath = basePath.startsWith("/") ? basePath : "/" + basePath;
            urlMap.put(normalizedBasePath + "/ws", webSocketHandler());
        }

        var handlerMapping = new SimpleUrlHandlerMapping(urlMap);
        handlerMapping.setOrder(-1);

        return handlerMapping;
    }

    @Bean
    public WebSocketHandler webSocketHandler() {
        return webSocketService::connectWebSocket;
    }
}
