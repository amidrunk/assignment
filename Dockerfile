FROM eclipse-temurin:25-jdk-jammy as build
WORKDIR /build

# Install maven
RUN apt-get update && \
    apt-get install -y maven

# Cache maven
COPY ./backend/pom.xml /build/pom.xml
RUN mvn dependency:go-offline

COPY ./backend /build
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY --from=build /build/target/encube-assignment-backend.jar /app/backend.jar

EXPOSE 8080
COPY ./entrypoint.sh /app/entrypoint.sh
ENTRYPOINT ["sh", "/app/entrypoint.sh"]