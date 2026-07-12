FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY DevStats-Backend/pom.xml .
RUN mvn dependency:go-offline -B

COPY DevStats-Backend/src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/devstats-bot-1.0.0.jar app.jar

ENV JAVA_OPTS=""

EXPOSE 8080

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
