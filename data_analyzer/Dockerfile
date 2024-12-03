FROM openjdk:8 AS buildingstage
ARG SBT_VERSION=1.10.2

# install sbt 
RUN \
  mkdir /working/ && \
  cd /working/ && \
  curl -L -o sbt-$SBT_VERSION.deb https://repo.scala-sbt.org/scalasbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  cd && \
  rm -r /working/ && \
  sbt sbtVersion

# add aplication data
WORKDIR /app
COPY . /app/

# build jar file for execution stage
RUN sbt assembly

FROM bde2020/spark-master:3.3.0-hadoop3.3
WORKDIR /app
COPY --from=buildingstage /app/target/scala-2.12/dataanalyzer-assembly-1.0.jar /app/dataanalyzer-assembly-1.0.jar

VOLUME /data

CMD ["/spark/bin/spark-submit", "--packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.3.0,com.influxdb:influxdb-client-java:4.0.0", "--master", "spark://spark-master:7077", "./dataanalyzer-assembly-1.0.jar"]