# syntax=docker/dockerfile:1.7

# Возьмём образ с уже установленным Gradle
FROM gradle:8.14.3-jdk21 AS builder
WORKDIR /workspace

# 1) Сначала кладём только файлы сборки, чтобы кэш зависимостей сохранялся
#    (если используешь Kotlin DSL — заменишь на build.gradle.kts / settings.gradle.kts)
COPY build.gradle settings.gradle ./
# Если есть gradle.properties — тоже:
# COPY gradle.properties ./

# 2) Пропрокачаем кэш зависимостей Gradle без wrapper’а
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle --no-daemon build -x test || true

# 3) Теперь докидываем исходники
COPY src ./src

# 4) Собираем jar системным Gradle (НЕ wrapper!)
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle --no-daemon bootJar -x test

# ---- runtime-слой ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Кладём собранный jar
COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
