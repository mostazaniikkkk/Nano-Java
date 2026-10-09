#!/bin/sh
# Builds each test MIDlet in tests/midlets/<name>/ into build/midlets/<name>.jar
# (sources in src/, resources in res/, manifest in MANIFEST.MF).
set -e
cd "$(dirname "$0")/../.."
mkdir -p build/midlets
for dir in tests/midlets/*/; do
    name=$(basename "$dir")
    out=build/midlets/$name
    rm -rf "$out" && mkdir -p "$out"
    javac -nowarn -Xlint:-options -encoding UTF-8 -source 1.3 -target 1.3 \
        -bootclasspath build/classlib.jar -extdirs "" \
        -d "$out" $(find "$dir/src" -name '*.java')
    if [ -d "$dir/res" ]; then
        cp -r "$dir/res/." "$out/"
    fi
    (cd "$out" && jar cfm "../$name.jar" "../../../$dir/MANIFEST.MF" .)
    echo "built build/midlets/$name.jar"
done
