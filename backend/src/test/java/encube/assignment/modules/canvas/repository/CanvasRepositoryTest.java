package encube.assignment.modules.canvas.repository;

import encube.assignment.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.reactive.TransactionalOperator;

import static org.assertj.core.api.Assertions.*;

@IntegrationTest
class CanvasRepositoryTest {

    @Autowired
    private CanvasRepository canvasRepository;

    @Autowired
    private TransactionalOperator tx;

    @Test
    void persisted_canvas_can_be_retrieved_by_id() {
        var id = canvasRepository.persist(
                        encube.assignment.modules.canvas.domain.Canvas.Payload.builder()
                                .name("Test Canvas")
                                .build())
                .as(tx::transactional)
                .block();

        var retrieved = canvasRepository.findById(id)
                .as(tx::transactional)
                .block();

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo(id);
        assertThat(retrieved.payload().name()).isEqualTo("Test Canvas");
    }

    @Test
    void all_canvases_can_be_retrieved() {
        canvasRepository.persist(
                        encube.assignment.modules.canvas.domain.Canvas.Payload.builder()
                                .name("Canvas One")
                                .build())
                .as(tx::transactional)
                .block();

        canvasRepository.persist(
                        encube.assignment.modules.canvas.domain.Canvas.Payload.builder()
                                .name("Canvas Two")
                                .build())
                .as(tx::transactional)
                .block();

        var canvases = canvasRepository.findAll()
                .as(tx::transactional)
                .collectList()
                .block();

        assertThat(canvases).isNotNull();
        assertThat(canvases).hasSizeGreaterThanOrEqualTo(2);
        assertThat(canvases).extracting(c -> c.payload().name())
                .contains("Canvas One", "Canvas Two");
    }
}