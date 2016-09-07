#!/usr/bin/env bash

set -e
set -x

if [ -z "${TRAVIS_TAG}" ]; then
    QUAY_TAG="${TRAVIS_COMMIT:0:7}"
else
    QUAY_TAG="${TRAVIS_TAG}"
fi

docker build -t "quay.io/usace/program-analysis-geoprocessing:${QUAY_TAG}" .

docker push "quay.io/usace/program-analysis-geoprocessing:${QUAY_TAG}"
docker tag "quay.io/usace/program-analysis-geoprocessing:${QUAY_TAG}" "quay.io/usace/program-analysis-geoprocessing:latest"
docker push "quay.io/usace/program-analysis-geoprocessing:latest"
