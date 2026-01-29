package encube.assignment.files.api;

import encube.assignment.IntegrationTest;
import encube.assignment.modules.files.api.protocol.CreateFileRequest;
import encube.assignment.modules.files.domain.FileDescriptor;
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
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class FileApiTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void file_can_be_uploaded_with_file_descriptor() {
        var fileDescriptor = CreateFileRequest.builder()
                .fileDescriptor(FileDescriptor.Payload.builder()
                        .contentType("text/plain")
                        .fileName("test.txt")
                        .build())
                .build();

        // JSON part: ensure application/json
        var descriptorHeaders = new HttpHeaders();
        descriptorHeaders.setContentType(MediaType.APPLICATION_JSON);
        var descriptorPart = new HttpEntity<>(fileDescriptor, descriptorHeaders);

        // File part: Resource with filename + content-type
        byte[] bytes = "Hello, World!".getBytes(StandardCharsets.UTF_8);
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

        webTestClient.post()
                .uri("/files")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:changeme".getBytes(StandardCharsets.UTF_8)))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartData))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FileDescriptor.class)
                .value(actualFileDescriptor -> {
                    assertThat(actualFileDescriptor.id()).isNotNull();
                    assertThat(actualFileDescriptor.state()).isEqualTo(FileDescriptor.State.UPLOADED);
                    assertThat(actualFileDescriptor.payload().fileName()).isEqualTo("test.txt");
                    assertThat(actualFileDescriptor.payload().contentType()).isEqualTo("text/plain");
                });
    }
}
