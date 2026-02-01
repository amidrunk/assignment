package encube.assignment.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ApplicationConfig {

    @Bean
    public DataBufferFactory dataBufferFactory() {
        return new DefaultDataBufferFactory(true);
    }

    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }

    @Bean
    public DSLContext dslContext() {
        return DSL.using(SQLDialect.POSTGRES);
    }
}
