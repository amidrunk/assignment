package encube.assignment.modules.canvas.service;

import encube.assignment.modules.canvas.domain.Canvas;
import encube.assignment.modules.canvas.repository.CanvasRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class CanvasService {

    private final CanvasRepository canvasRepository;
    private final CanvasEventPublisher canvasEventPublisher;

    @Transactional
    public Mono<Canvas> createCanvas(Canvas.Payload canvasPayload) {
        Validate.notNull(canvasPayload, "canvasPayload must not be null");

        return canvasRepository.persist(canvasPayload)
                .flatMap(canvasRepository::findById)
                .flatMap(newCanvas -> canvasEventPublisher.publishCanvasCreated(newCanvas).thenReturn(newCanvas))
                .doOnSuccess(canvas -> log.info("Created new canvas with id {}", canvas.id()));
    }

    public Flux<Canvas> getAllCanvases() {
        return canvasRepository.findAll();
    }
}
