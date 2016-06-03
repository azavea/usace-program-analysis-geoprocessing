#!/usr/bin/env bash

set -e
set -x

DIR="$(dirname "$0")"

if [ -z "${TRAVIS_TAG}" ]; then
    QUAY_TAG="${TRAVIS_COMMIT:0:7}"
else
    QUAY_TAG="${TRAVIS_TAG}"
fi

QUAY_TAG="${QUAY_TAG}" docker-compose -f "${DIR}/../docker-compose.ci.yml" build release

docker push "quay.io/usace/program-analysis-geoprocessing:${QUAY_TAG}"
docker tag -f "quay.io/usace/program-analysis-geoprocessing:${QUAY_TAG}" "quay.io/usace/program-analysis-geoprocessing:latest"
docker push "quay.io/usace/program-analysis-geoprocessing:latest"
