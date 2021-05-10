#!/usr/bin/env bash

set -e
set -o pipefail

HEAD_REF=$1

if [[ -z "$HEAD_REF" ]]; then
  echo "Set the HEAD_REF parameter"
  exit 1
fi

echo "VERSION_TAG=${HEAD_REF#*org.liquibase-liquibase-core-}" >> $GITHUB_ENV