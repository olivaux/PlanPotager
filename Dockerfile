# --- Étape 1 : build du frontend (Vue / Vite) ---
FROM node:22-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

# --- Étape 2 : build du backend (Spring Boot / Gradle) ---
FROM eclipse-temurin:25-jdk AS backend-build
WORKDIR /app
COPY . .
COPY --from=frontend-build /frontend/dist src/main/resources/static
RUN chmod +x gradlew && ./gradlew bootJar -x test

# --- Étape 3 : image d'exécution ---
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=backend-build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
