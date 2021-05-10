#!/usr/bin/env bash

set -e
set -o pipefail

echo "VERSION_TAG=$(mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout)" >> $GITHUB_ENV