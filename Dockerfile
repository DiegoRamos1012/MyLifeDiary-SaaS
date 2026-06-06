# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -DskipTests package -Dspring-boot.repackage.excludeDevtools=true

# ---- runtime ----
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Extrai o JAR em camadas para melhor cache no Docker
COPY --from=build /app/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre AS final
WORKDIR /app

COPY --from=runtime /app/extracted/dependencies/ ./
COPY --from=runtime /app/extracted/spring-boot-loader/ ./
COPY --from=runtime /app/extracted/snapshot-dependencies/ ./
COPY --from=runtime /app/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+OptimizeStringConcat", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]