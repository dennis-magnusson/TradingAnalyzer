FROM openjdk:8-jre-slim

WORKDIR /app

COPY target/scala-2.12/dataproducer-assembly-1.0.jar /app/dataproducer-assembly-1.0.jar

VOLUME /data

CMD ["java", "-jar", "dataproducer-assembly-1.0.jar"]