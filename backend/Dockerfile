FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S gymsaas && adduser -S gymsaas -G gymsaas \
    && apk add --no-cache wget
WORKDIR /app
# Contract PDFs (Phase 5); mounted as a named volume in docker-compose.
RUN mkdir -p /app/data/contracts && chown -R gymsaas:gymsaas /app/data
COPY --from=build /workspace/target/*.jar app.jar
USER gymsaas
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
