package encube.assignment.modules.files.repository;

import encube.assignment.IntegrationTest;
import encube.assignment.modules.files.domain.FileDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@IntegrationTest
class FileDescriptorRepositoryTest {

    @Autowired
    private FileDescriptorRepository fileDescriptorRepository;

    @Autowired
    private TransactionalOperator tx;

    @Test
    void persisted_file_can_be_retrieved_by_id() {
        var id = fileDescriptorRepository.persist(FileDescriptor.State.UPLOADED, List.of(
                FileDescriptor.Payload.builder()
                        .fileName("document.pdf")
                        .contentType("application/pdf")
                        .attributes(Map.of(
                                "author", "John Doe",
                                "pages", "10"
                        ))
                        .build()
        )).as(tx::transactional).single().block().getT1();

        var retrieved = fileDescriptorRepository.findById(id)
                .as(tx::transactional)
                .single()
                .block();

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo(id);
        assertThat(retrieved.state()).isEqualTo(FileDescriptor.State.UPLOADED);
        assertThat(retrieved.payload().fileName()).isEqualTo("document.pdf");
        assertThat(retrieved.payload().contentType()).isEqualTo("application/pdf");
        assertThat(retrieved.payload().attributes()).containsEntry("author", "John Doe")
                .containsEntry("pages", "10");
    }

}