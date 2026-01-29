package encube.assignment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        if (List.of(args).contains("--provision")) {
            new SpringApplicationBuilder(Application.class)
                    .profiles("provision")
                    .web(WebApplicationType.NONE)
                    .run(args);
        } else {
            SpringApplication.run(Application.class, args);
        }

    }
}
