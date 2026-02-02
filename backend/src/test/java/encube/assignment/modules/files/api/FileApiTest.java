package encube.assignment.modules.files.api;

import encube.assignment.DomainEventReader;
import encube.assignment.IntegrationTest;
import encube.assignment.TestHelper;
import encube.assignment.events.ChangeType;
import encube.assignment.events.FileDescriptorChangedEvent;
import encube.assignment.modules.files.api.protocol.CreateFileRequest;
import encube.assignment.modules.files.domain.FileDescriptor;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class FileApiTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private TestHelper testHelper;
    @Autowired
    private DomainEventReader domainEventReader;

    @Test
    void files_should_be_empty_if_no_files_have_been_created() {
        testHelper.authenticatedClient().get()
                .uri("/files")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FileDescriptor.class)
                .hasSize(0);
    }

    @Test
    void file_can_be_uploaded_with_file_descriptor() {
        var fileDescriptor = FileDescriptor.Payload.builder()
                .contentType("text/plain")
                .fileName("test.txt")
                .build();

        uploadFileThen(fileDescriptor, "Hello, World!").expectStatus().isCreated()
                .expectBody(FileDescriptor.class)
                .value(actualFileDescriptor -> {
                    assertThat(actualFileDescriptor.id()).isNotNull();
                    assertThat(actualFileDescriptor.state()).isEqualTo(FileDescriptor.State.UPLOADED);
                    assertThat(actualFileDescriptor.payload().fileName()).isEqualTo("test.txt");
                    assertThat(actualFileDescriptor.payload().contentType()).isEqualTo("text/plain");
                });

        testHelper.authenticatedClient().get()
                .uri("/files")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FileDescriptor.class)
                .hasSize(1)
                .value(fileDescriptors -> {
                    FileDescriptor fd = fileDescriptors.get(0);
                    assertThat(fd.payload().fileName()).isEqualTo("test.txt");
                    assertThat(fd.payload().contentType()).isEqualTo("text/plain");
                });
    }

    @Test
    void file_can_be_created_with_tags() {
        var fileDescriptor = FileDescriptor.Payload.builder()
                .fileName("tagged-file.txt")
                .contentType("text/plain")
                .attributes(Map.of("category", "documents", "owner", "user123"))
                .build();

        uploadFileThen(fileDescriptor, "File with attributes").expectStatus().isCreated()
                .expectBody(FileDescriptor.class)
                .value(actualFileDescriptor -> {
                    assertThat(actualFileDescriptor.id()).isNotNull();
                    assertThat(actualFileDescriptor.state()).isEqualTo(FileDescriptor.State.UPLOADED);
                    assertThat(actualFileDescriptor.payload().fileName()).isEqualTo("tagged-file.txt");
                    assertThat(actualFileDescriptor.payload().contentType()).isEqualTo("text/plain");
                    assertThat(actualFileDescriptor.payload().attributes()).containsEntry("category", "documents")
                            .containsEntry("owner", "user123");
                });

        var domainEvent = domainEventReader.all()
                .filter(FileDescriptorChangedEvent.class::isInstance)
                .cast(FileDescriptorChangedEvent.class)
                .blockFirst();

        assertThat(domainEvent.getChangeType()).isEqualTo(ChangeType.CHANGE_TYPE_CREATED);
        assertThat(domainEvent.getNewValue().getId()).isNotNull();
        assertThat(domainEvent.getNewValue().getName()).isEqualTo("tagged-file.txt");
        assertThat(domainEvent.getNewValue().getContentType()).isEqualTo("text/plain");
        assertThat(domainEvent.getNewValue().getAttributesMap()).containsEntry("category", "documents")
                .containsEntry("owner", "user123");
    }

    @Test
    void files_can_be_filtered_by_canvas_id_query_param() {
        var fileWithCanvas = FileDescriptor.Payload.builder()
                .fileName("canvas-file.txt")
                .contentType("text/plain")
                .attributes(Map.of("canvasId", "1234", "other", "keep"))
                .build();

        var otherFile = FileDescriptor.Payload.builder()
                .fileName("other-file.txt")
                .contentType("text/plain")
                .attributes(Map.of("canvasId", "5678"))
                .build();

        uploadFileThen(fileWithCanvas, "canvas content").expectStatus().isCreated();
        uploadFileThen(otherFile, "other content").expectStatus().isCreated();

        testHelper.authenticatedClient().get()
                .uri(builder -> builder.path("/files").queryParam("canvasId", "1234").build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FileDescriptor.class)
                .hasSize(1)
                .value(fileDescriptors -> {
                    var fd = fileDescriptors.get(0);
                    assertThat(fd.payload().fileName()).isEqualTo("canvas-file.txt");
                    assertThat(fd.payload().attributes()).containsEntry("canvasId", "1234");
                });
    }

    @Test
    void file_data_can_be_retrieved_for_uploaded_file() {
        var fileDescriptor = FileDescriptor.Payload.builder()
                .fileName("data-file.txt")
                .contentType("text/plain")
                .build();

        var uploadResponse = uploadFileThen(fileDescriptor, "File data content")
                .expectStatus().isCreated()
                .expectBody(FileDescriptor.class)
                .returnResult()
                .getResponseBody();

        assert uploadResponse != null;
        Long fileId = uploadResponse.id();

        webTestClient.get()
                .uri("/files/{fileId}/data", fileId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/plain")
                .expectBody(String.class)
                .value(content -> assertThat(content).isEqualTo("File data content"));
    }

    private WebTestClient.@NonNull ResponseSpec uploadFileThen(FileDescriptor.Payload fileDescriptor, String content) {
        var createFileRequest = CreateFileRequest.builder()
                .fileDescriptor(fileDescriptor)
                .build();

        // JSON part: ensure application/json
        var descriptorHeaders = new HttpHeaders();
        descriptorHeaders.setContentType(MediaType.APPLICATION_JSON);
        var descriptorPart = new HttpEntity<>(createFileRequest, descriptorHeaders);

        // File part: Resource with filename + content-type
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        var fileResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        };

        var fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.TEXT_PLAIN);
        var filePart = new HttpEntity<>(fileResource, fileHeaders);

        MultiValueMap<String, Object> multipartData = new LinkedMultiValueMap<>();
        multipartData.add("descriptor", descriptorPart);
        multipartData.add("file", filePart);

        WebTestClient client = testHelper.authenticatedClient();

        return client.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartData))
                .exchange();
    }
}
