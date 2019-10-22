#!/bin/bash

set -e
set -u
set -o pipefail

IMAGE_NAME=${IMAGE_NAME:-dl4j-nlp-cuda}
IMAGE_VERSION=${IMAGE_VERSION:-$(cat ../version.txt)}
DOCKER_FULL_TAG_NAME="${DOCKER_USER_NAME}/${IMAGE_NAME}"

docker run -it                                            \
           --volume $(pwd):/$(pwd)                        \
           --env JAVA_HOME=/graalvm                       \
           ${DOCKER_FULL_TAG_NAME}:${IMAGE_VERSION}       \
           /bin/bash