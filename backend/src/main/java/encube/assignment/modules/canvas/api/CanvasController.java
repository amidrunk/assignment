package encube.assignment.modules.canvas.api;

import encube.assignment.modules.canvas.domain.Canvas;
import encube.assignment.modules.canvas.service.CanvasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CanvasController {

    private final CanvasService canvasService;

    @PostMapping("/canvases")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Canvas> createCanvas(@RequestBody Canvas.Payload canvasPayload) {
        return canvasService.createCanvas(canvasPayload);
    }

    @GetMapping("/canvases")
    public Flux<Canvas> getAllCanvases() {
        return canvasService.getAllCanvases();
    }
}
