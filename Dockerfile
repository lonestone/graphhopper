FROM maven:3.6.3-jdk-8 as build

RUN apt-get install -y wget

WORKDIR /graphhopper

COPY . .


RUN chmod a+rx ./graphhopper.sh

RUN ./graphhopper.sh build

FROM openjdk:11.0-jre

ENV JAVA_OPTS "-Xmx1g -Xms1g -Ddw.server.application_connectors[0].bind_host=0.0.0.0 -Ddw.server.application_connectors[0].port=8989"

RUN mkdir -p /data

WORKDIR /graphhopper

COPY --from=build /graphhopper/web/target/*.jar ./web/target/
# pom.xml is used to get the jar file version. see https://github.com/graphhopper/graphhopper/pull/1990#discussion_r409438806
COPY ./graphhopper.sh ./pom.xml ./config-example.yml ./pays-de-la-loire-latest.osm.pbf ./

VOLUME [ "/data" ]

EXPOSE 8989

ENTRYPOINT [ "./graphhopper.sh", "web" ]

CMD [ "/data/pays-de-la-loire-latest.osm.pbf" ]