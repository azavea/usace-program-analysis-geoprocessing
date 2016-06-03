FROM quay.io/azavea/spark:1.6.1

ENV VERSION 0.1.0

COPY geop/target/scala-2.10/usace-programanalysis-geop-assembly-${VERSION}.jar /opt/geoprocessing/

WORKDIR /opt/geoprocessing

EXPOSE 8090

ENTRYPOINT ["spark-submit"]
CMD ["/opt/geoprocessing/usace-programanalysis-geop-assembly-${VERSION}.jar"]
