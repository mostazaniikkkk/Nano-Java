#!/usr/bin/env bash
# build.sh — shortcuts for building Nano Java NDS inside the container
# Toolchain: BlocksDS v1.20.0 + Wonderful Toolchain + OpenJDK 8
#
# Usage (from the container shell or via docker compose):
#   ./build.sh nds          - build nanojava.nds  (default)
#   ./build.sh nds debug=1  - build with debug symbols + INCLUDEDEBUGCODE
#   ./build.sh clean        - remove build artefacts
#   ./build.sh api          - compile Java API sources → api/classes/
#   ./build.sh jcc          - compile the JCC tool
#   ./build.sh romize       - run JCC to regenerate ROMjavaGBA.c
#   ./build.sh preverifier  - compile the host preverifier binary
#   ./build.sh hello        - compile + preverify HelloMIDlet → FAT/
#   ./build.sh full         - api + jcc + romize + preverifier + hello + nds
#
# All paths are relative to the project root (/project inside the container).

export BLOCKSDS="${BLOCKSDS:-/opt/wonderful/thirdparty/blocksds/core}"
export BLOCKSDSEXT="${BLOCKSDSEXT:-/opt/wonderful/thirdparty/blocksds/external}"
export WONDERFUL_TOOLCHAIN="${WONDERFUL_TOOLCHAIN:-/opt/wonderful}"

set -euo pipefail
cd "$(dirname "$0")"

TARGET="${1:-nds}"
EXTRA="${2:-}"

NDS_BUILD_DIR="kvm/VmSkel/build"
PREVERIFIER_DIR="tools/preverifier/build/linux"
JCC_DIR="tools/jcc"
API_SRC_DIR="api/src"
API_CLASSES_DIR="api/classes"
DEMOS_DIR="demos"

case "$TARGET" in

  nds)
    echo "==> Building nanojava.nds [$EXTRA]"
    make -C "$NDS_BUILD_DIR" $EXTRA
    echo "==> Done: $NDS_BUILD_DIR/nanojava.nds"
    ;;

  clean)
    echo "==> Cleaning NDS build"
    make -C "$NDS_BUILD_DIR" clean
    ;;

  api)
    echo "==> Compiling Java API (source 1.4 → api/classes/)"
    mkdir -p "$API_CLASSES_DIR"
    find "$API_SRC_DIR" -name "*.java" > /tmp/api_sources.txt
    javac -source 1.4 -target 1.4 \
          -d "$API_CLASSES_DIR" \
          @/tmp/api_sources.txt
    echo "==> Done: $API_CLASSES_DIR"
    ;;

  jcc)
    echo "==> Compiling JCC"
    mkdir -p "$JCC_DIR/classes"
    find "$JCC_DIR/src" -name "*.java" > /tmp/jcc_sources.txt
    javac -source 1.4 -target 1.4 \
          -d "$JCC_DIR/classes" \
          @/tmp/jcc_sources.txt
    # JCCMessage.properties must be on the classpath at runtime
    cp "$JCC_DIR/src/JCCMessage.properties" "$JCC_DIR/classes/"
    echo "==> Done: $JCC_DIR/classes"
    ;;

  romize)
    echo "==> Running JCC to regenerate ROMjavaGBA.c"
    JCC_JAR="$JCC_DIR/classes"
    OUT_C="kvm/VmSkel/src/ROMjavaGBA.c"
    TMP_JAR="/tmp/api_classes.jar"
    TMP_DEMOS="/tmp/demos_for_rom"

    # Compile demo MIDlets for ROM inclusion (no preverify needed for JCC)
    mkdir -p "$TMP_DEMOS"
    javac -source 1.4 -target 1.4 \
          -classpath "$API_CLASSES_DIR" \
          -d "$TMP_DEMOS" \
          "$DEMOS_DIR/hello/HelloMIDlet.java"

    # Pack API + demos into one JAR for JCC
    jar cf "$TMP_JAR" -C "$API_CLASSES_DIR" . -C "$TMP_DEMOS" .

    java -cp "$JCC_JAR" JavaCodeCompact \
        -nq \
        -arch KVM \
        -o "$OUT_C" \
        "$TMP_JAR"

    echo "==> Generated: $OUT_C"
    ;;

  preverifier)
    echo "==> Building preverifier (host Linux binary)"
    make -C "$PREVERIFIER_DIR"
    echo "==> Done: $PREVERIFIER_DIR/preverify"
    ;;

  hello)
    echo "==> Compiling + preverifying HelloMIDlet"
    FAT_DIR="$NDS_BUILD_DIR/FAT"
    PREVERIFY="$PREVERIFIER_DIR/preverify"
    TMP_COMPILE="/tmp/hello_compiled"
    TMP_PREVERIFY="/tmp/hello_preverified"

    mkdir -p "$FAT_DIR" "$TMP_COMPILE" "$TMP_PREVERIFY"

    # Compile to temp dir (never touch FAT directly)
    javac -source 1.4 -target 1.4 \
          -classpath "$API_CLASSES_DIR" \
          -d "$TMP_COMPILE" \
          "$DEMOS_DIR/hello/HelloMIDlet.java"

    # Preverify from temp
    "$PREVERIFY" -classpath "$API_CLASSES_DIR" \
                 -d "$TMP_PREVERIFY" \
                 "$TMP_COMPILE"

    # Only copy the final .class to FAT
    cp "$TMP_PREVERIFY/HelloMIDlet.class" "$FAT_DIR/HelloMIDlet.class"
    echo "==> Done: $FAT_DIR/HelloMIDlet.class"
    ;;

  full)
    echo "==> Full build: api → jcc → preverifier → romize → hello → nds"
    "$0" api
    "$0" jcc
    "$0" preverifier
    "$0" romize
    touch kvm/VmSkel/src/ROMjavaGBA.c
    "$0" hello
    "$0" nds
    echo "==> Full build complete"
    ;;

  *)
    echo "Unknown target: $TARGET"
    echo "Valid targets: nds, clean, api, jcc, romize, preverifier, hello, full"
    exit 1
    ;;

esac
