#!/bin/sh
# Compiles the Java tests against the class library and runs each one on the
# host build, comparing its output with tests/java/<Name>.expected.
set -u
cd "$(dirname "$0")/.."

OUT=build/tests
rm -rf "$OUT" && mkdir -p "$OUT/classes"
javac -nowarn -Xlint:-options -encoding UTF-8 -source 1.3 -target 1.3 \
    -bootclasspath build/classlib.jar -extdirs "" \
    -d "$OUT/classes" tests/java/*.java || exit 1

pass=0
fail=0
for exp in tests/java/*.expected; do
    name=$(basename "$exp" .expected)
    timeout 60 build/host/nanojava --classlib build/classlib.jar \
        -cp "$OUT/classes" -main "$name" > "$OUT/$name.out" 2>&1
    code=$?
    if [ $code -eq 0 ] && cmp -s "$exp" "$OUT/$name.out"; then
        pass=$((pass + 1))
        echo "PASS $name"
    else
        fail=$((fail + 1))
        echo "FAIL $name (exit $code)"
        diff "$exp" "$OUT/$name.out" | head -20
    fi
done

# Test MIDlets with an expected.txt: run headless, compare their output.
sh tests/midlets/build.sh >/dev/null || exit 1
for exp in tests/midlets/*/expected.txt; do
    [ -f "$exp" ] || continue
    name=$(basename "$(dirname "$exp")")
    rm -rf "$OUT/rms-$name"
    timeout 60 build/host/nanojava --classlib build/classlib.jar --data "$OUT/rms-$name" \
        --run-ms 4000 "build/midlets/$name.jar" > "$OUT/midlet-$name.out" 2>&1
    code=$?
    if [ $code -eq 0 ] && cmp -s "$exp" "$OUT/midlet-$name.out"; then
        pass=$((pass + 1))
        echo "PASS midlet $name"
    else
        fail=$((fail + 1))
        echo "FAIL midlet $name (exit $code)"
        diff "$exp" "$OUT/midlet-$name.out" | head -20
    fi
done

echo "$pass passed, $fail failed"
[ $fail -eq 0 ]
