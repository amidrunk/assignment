package encube.assignment.modules.canvas.api;

import encube.assignment.DomainEventReader;
import encube.assignment.IntegrationTest;
import encube.assignment.TestHelper;
import encube.assignment.events.CanvasChangedEvent;
import encube.assignment.events.ChangeType;
import encube.assignment.modules.canvas.domain.Canvas;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class CanvasApiTest {

    @Autowired
    private TestHelper testHelper;
    @Autowired
    private DomainEventReader domainEventReader;

    @Test
    void canvas_can_be_created_and_retrieved() {
        var createdCanvas = testHelper.authenticatedClient()
                .post()
                .uri("/canvases")
                .bodyValue(Canvas.Payload.builder()
                        .name("Test Canvas")
                        .build())
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Canvas.class)
                .value(newCanvas -> {
                    assertThat(newCanvas.id()).isNotNull();
                    assertThat(newCanvas.payload().name()).isEqualTo("Test Canvas");
                })
                .returnResult()
                .getResponseBody();

        testHelper.authenticatedClient().get()
                .uri("/canvases")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Canvas.class)
                .value(canvases -> {
                    assertThat(canvases).isNotEmpty();
                    assertThat(canvases).anyMatch(canvas -> canvas.id().equals(createdCanvas.id()));
                });

        var domainEvent = domainEventReader.all()
                .filter(CanvasChangedEvent.class::isInstance)
                .cast(CanvasChangedEvent.class)
                .single()
                .block();

        assertThat(domainEvent.getChangeType()).isEqualTo(ChangeType.CHANGE_TYPE_CREATED);
        assertThat(domainEvent.getNewValue().getName()).isEqualTo("Test Canvas");
    }
}
