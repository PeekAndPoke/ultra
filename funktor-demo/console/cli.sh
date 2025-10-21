#!/bin/bash

set -e

echo "Running Funktor CLI ..."

../gradlew :funktor-demo:server:run --configuration-cache \
    --console=plain \
    --quiet \
    -Djansi.passthrough=true \
    --args="--cli $*"
