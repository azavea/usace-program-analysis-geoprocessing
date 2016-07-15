FROM quay.io/azavea/spark:1.6.1

ENV VERSION 0.1.1

COPY geop/target/scala-2.10/usace-programanalysis-geop-assembly-${VERSION}.jar /opt/geoprocessing/usace-programanalysis-geop.jar
COPY scripts/docker-entrypoint.sh /opt/geoprocessing/

WORKDIR /opt/geoprocessing

EXPOSE 8090

VOLUME /tmp

ENTRYPOINT ["./docker-entrypoint.sh"]
