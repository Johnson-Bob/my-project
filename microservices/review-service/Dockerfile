FROM amazoncorretto:23-alpine as builder
WORKDIR /builder
ADD ./build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM amazoncorretto:23-alpine
WORKDIR /application
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]