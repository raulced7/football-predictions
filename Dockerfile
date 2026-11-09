# --- Compilación ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Primero solo el pom: mientras no cambien las dependencias, Docker reutiliza esta capa y no las vuelve a descargar
COPY .mvn .mvn
COPY mvnw pom.xml ./
# En Windows git no guarda el permiso de ejecución de mvnw
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Los tests no se ejecutan aquí (necesitan Docker para Testcontainers); ya los pasa la CI
COPY src src
RUN ./mvnw -B -q package -DskipTests && cp target/*.jar app.jar

# --- Ejecución: solo el JRE, sin Maven ni el código fuente ---
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 1001 spring
WORKDIR /app
COPY --from=build /build/app.jar app.jar
USER spring
EXPOSE 8080
# Memoria por defecto; en Railway se sobrescribe con la variable JAVA_TOOL_OPTIONS
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]