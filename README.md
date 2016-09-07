# USACE Program Analysis Geoprocessing

[![Build Status](https://travis-ci.org/azavea/usace-program-analysis-geoprocessing.png?branch=master)](https://travis-ci.org/azavea/usace-program-analysis-geoprocessing)
[![Docker Repository on Quay](https://quay.io/repository/usace/program-analysis-geoprocessing/status "Docker Repository on Quay")](https://quay.io/repository/usace/program-analysis-geoprocessing)

This repository contains the backing geoprocessing service for the [USACE Program Analysis web app](https://github.com/azavea/usace-program-analysis). It is a [Spray](https://github.com/spray/spray) based web service that performs geoprocessing operations using [GeoTrellis](https://github.com/geotrellis/geotrellis) and [Apache Spark](http://spark.apache.org/).

## Build

To build the service JAR, execute the following:

    $ ./sbt "project geop" assembly

which should result in `geop/target/scala-2.10/usace-programanalysis-geop-assembly-$VERSION.jar`.

## Run

The assembled JAR can be run via Spark.

    $ spark-submit geop/target/scala-2.10/usace-programanalysis-geop-assembly-$VERSION.jar

As Spark loads the JAR it will output a number of messages. Once the following are shown, the server is ready to accept requests:

    [INFO] [06/01/2016 14:22:52.711] [usace-programanalysis-geop-akka.actor.default-dispatcher-2] [akka://usace-programanalysis-geop/user/IO-HTTP/listener-0] Bound to /0.0.0.0:8090
    [INFO] [06/01/2016 14:22:52.713] [usace-programanalysis-geop-akka.actor.default-dispatcher-3] [akka://usace-programanalysis-geop/deadLetters] Message [akka.io.Tcp$Bound] from Actor[akka://usace-programanalysis-geop/user/IO-HTTP/listener-0#-233686889] to Actor[akka://usace-programanalysis-geop/deadLetters] was not delivered. [1] dead letters encountered. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.

The server can be stopped with <kbd>Ctrl</kbd> + <kbd>C</kbd>.

### Test

You can test the running service by hitting the `/ping` endpoint, which should result in a simple `OK` reply. Using [httpie](https://github.com/jkbrzt/httpie):

    $ http :8090/ping
    HTTP/1.1 200 OK
    Content-Length: 2
    Content-Type: text/plain; charset=UTF-8
    Date: Wed, 01 Jun 2016 18:22:56 GMT
    Server: spray-can/1.3.3

    OK

Alternatively, using `curl`:

    $ curl http://localhost:8090/ping
    OK

## Test with Web App

To test with the [main web app](https://github.com/azavea/usace-program-analysis), build the JAR and copy it to the [`/src/geoprocessing`](https://github.com/azavea/usace-program-analysis/tree/develop/src/geoprocessing) directory, and uncomment the volume mapping in [`docker-compose.yml`](https://github.com/azavea/usace-program-analysis/blob/develop/docker-compose.yml#L69-L71). Then run `docker-compose up`.

## Deploy

Deployments to [GitHub Releases](https://github.com/azavea/usace-program-analysis-geoprocessing/releases) and [Quay](https://quay.io/repository/usace/program-analysis-geoprocessing) are handled via [Travis-CI](https://travis-ci.org/azavea/usace-program-analysis-geoprocessing). The following `git-flow` commands signal to Travis that we want to create a release. The `version` variable should be updated in `project/Version.scala` and `Dockerfile`.

``` bash
$ git flow release start 0.0.1
$ vim CHANGELOG.md
$ vim project/Version.scala
$ vim Dockerfile
$ git commit -m "0.0.1"
$ git flow release publish 0.0.1
$ git flow release finish 0.0.1
```

You should now check the `develop` and `master` branches on Github to make sure that they look correct.  In particular, they should both contain the changes that you made to `CHANGELOG.md`.  If they do not, then the following two steps may also be required:

```bash
$ git push origin develop:develop
$ git push origin master:master
```

To actually kick off the deployment, ensure that the newly created Git tags are pushed remotely with `git push --tags`.
