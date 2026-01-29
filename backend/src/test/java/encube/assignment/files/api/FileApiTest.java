package encube.assignment.files.api;

import encube.assignment.IntegrationTest;
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

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class FileApiTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void files_should_be_empty_if_no_files_have_been_created() {
        authenticatedClient().get()
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

        authenticatedClient().get()
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

    private WebTestClient authenticatedClient() {
        var sessionCookie = login();

        return webTestClient.mutate()
                .defaultCookie(sessionCookie.getName(), sessionCookie.getValue())
                .build();
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

        WebTestClient client = authenticatedClient();

        WebTestClient.ResponseSpec responseSpec = client.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartData))
                .exchange();
        return responseSpec;
    }

    private org.springframework.http.ResponseCookie login() {
        var result = webTestClient.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "admin")
                        .with("password", "changeme"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .returnResult(Void.class);

        var session = result.getResponseCookies().getFirst("SESSION");
        assertThat(session).as("session cookie from login").isNotNull();
        return session;
    }
}
