package encube.assignment.config;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Configuration
@Slf4j
public class GrpcConfig {

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server grpcServer(@Value("${grpc.server.port}") int port, List<BindableService> services, List<ServerInterceptor> serverInterceptors) {
        var builder = ServerBuilder.forPort(port);

        serverInterceptors.forEach(builder::intercept);
        services.forEach(builder::addService);

        log.info(
                "gRPC server configured on {} {} {}",
                kv("port", port),
                kv("servicesCount", services.size()),
                kv("interceptorsCount", serverInterceptors.size())
        );

        return builder.build();
    }
}
