#!/bin/sh
# Runs a make target inside the Nano Java build container.
#   ./build.sh            build everything
#   ./build.sh test       run the tests
#   ./build.sh shell      open a shell in the container
set -e
cd "$(dirname "$0")"
IMAGE=nanojava-build

if ! docker image inspect "$IMAGE" >/dev/null 2>&1; then
    docker build -t "$IMAGE" docker
fi

if [ "$1" = "shell" ]; then
    exec docker run --rm -it -v "$PWD":/src -w /src "$IMAGE" bash
fi
exec docker run --rm -v "$PWD":/src -w /src "$IMAGE" make "$@"
