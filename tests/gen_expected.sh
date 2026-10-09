#!/bin/sh
# Regenerates tests/java/*.expected by running the tests on the JDK, which
# serves as the reference implementation. Only use for tests whose output
# does not depend on the VM (no timing, hash codes or memory sizes).
set -e
cd "$(dirname "$0")/.."
OUT=build/tests-ref
rm -rf "$OUT" && mkdir -p "$OUT"
javac -nowarn -encoding UTF-8 -d "$OUT" tests/java/*.java
for src in tests/java/Test*.java; do
    name=$(basename "$src" .java)
    java -cp "$OUT" "$name" > "tests/java/$name.expected"
    echo "generated $name.expected"
done
