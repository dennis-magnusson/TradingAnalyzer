FROM openjdk:8 as buildingstage
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

FROM openjdk:8-jre-slim
WORKDIR /app
COPY --from=buildingstage /app/target/scala-2.12/dataproducer-assembly-1.0.jar /app/dataproducer-assembly-1.0.jar

VOLUME /data

CMD ["java", "-jar", "dataproducer-assembly-1.0.jar"]