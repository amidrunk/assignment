package encube.assignment.modules.websocket.grpc;

import encube.assignment.client.WebSocketClientGrpc;
import encube.assignment.client.WebSocketMessageRequest;
import encube.assignment.client.WebSocketMessageResponse;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WebSocketGrpcService extends WebSocketClientGrpc.WebSocketClientImplBase {

    @Override
    public void sendMessage(WebSocketMessageRequest request, StreamObserver<WebSocketMessageResponse> responseObserver) {
        super.sendMessage(request, responseObserver);
    }
}
