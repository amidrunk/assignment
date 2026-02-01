package encube.assignment.modules.websocket.grpc;

import encube.assignment.client.WebSocketClientGrpc;
import encube.assignment.client.WebSocketMessageRequest;
import encube.assignment.client.WebSocketMessageResponse;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.NoSuchElementException;

@Component
@Slf4j
public class WebSocketGrpcService extends WebSocketClientGrpc.WebSocketClientImplBase {

    private final encube.assignment.modules.websocket.service.WebSocketService webSocketService;

    private final Duration timeout;

    public WebSocketGrpcService(encube.assignment.modules.websocket.service.WebSocketService webSocketService,
                                @Value("${grpc.server.timeout-seconds:5}") int timeoutSeconds) {
        this.webSocketService = webSocketService;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public void sendMessage(WebSocketMessageRequest request, StreamObserver<WebSocketMessageResponse> responseObserver) {
        try {
            var message = toDomainMessage(request);

            Mono<Void> action;
            if (request.hasConnectionId()) {
                action = webSocketService.sendMessageToConnection(request.getConnectionId(), message);
            } else if (request.hasUserName()) {
                action = webSocketService.sendMessageToUser(request.getUserName(), message);
            } else {
                throw new IllegalArgumentException("receiver must be provided (connectionId or userName)");
            }

            action.timeout(timeout).block();

            responseObserver.onNext(WebSocketMessageResponse.newBuilder()
                    .setSuccess(true)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to send WebSocket message via gRPC", e);
            responseObserver.onNext(WebSocketMessageResponse.newBuilder()
                    .setError(toError(e))
                    .build());
            responseObserver.onCompleted();
        }
    }

    private static encube.assignment.client.Error toError(Exception e) {
        var builder = encube.assignment.client.Error.newBuilder()
                .setMessage(e.getMessage() == null ? e.toString() : e.getMessage());

        if (e instanceof NoSuchElementException) {
            builder.setCode(encube.assignment.client.ErrorCode.ERROR_CODE_NOT_FOUND);
        } else {
            builder.setCode(encube.assignment.client.ErrorCode.ERROR_CODE_UNDEFINED);
        }

        return builder.build();
    }

    private static encube.assignment.modules.websocket.service.WebSocketMessage toDomainMessage(WebSocketMessageRequest request) {
        if (!request.hasMessage()) {
            throw new IllegalArgumentException("message must be provided");
        }

        var payload = request.getMessage();

        if (payload.hasTextMessage()) {
            return new encube.assignment.modules.websocket.service.WebSocketMessage.Text(payload.getTextMessage());
        }

        throw new IllegalArgumentException("Unsupported message type");
    }
}
