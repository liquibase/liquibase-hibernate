#!/usr/bin/env bash

set -ex
set -o pipefail

if [[ -z "$GITHUB_TOKEN" ]]; then
  echo "Set the GITHUB_TOKEN env variable."
  exit 1
fi

if [[ -z "$GITHUB_ISSUE_URL" ]]; then
  echo "Set the GITHUB_ISSUE_URL env variable."
  exit 1
fi

if [[ -z "$GITHUB_COMMENT" ]]; then
  echo "Set the GITHUB_COMMENT env variable."
  exit 1
fi

data='{"body":"'${GITHUB_COMMENT}'"}'

#Create Label on Pull Request
curl -X POST -H "Authorization: token $GITHUB_TOKEN" \
  --data "$data" \
  "$GITHUB_ISSUE_URL/comments"
