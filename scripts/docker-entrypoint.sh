#!/usr/bin/env bash

set -e

exec /opt/spark/bin/spark-submit "$@" usace-programanalysis-geop.jar 2>&1
