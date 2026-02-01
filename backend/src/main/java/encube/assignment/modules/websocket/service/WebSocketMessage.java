package encube.assignment.modules.websocket.service;

import tools.jackson.databind.JsonNode;

public sealed interface WebSocketMessage permits WebSocketMessage.Text {

    record Text(String payload) implements WebSocketMessage {
    }
}
