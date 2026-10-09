#!/bin/sh
# Boots a DS ROM in melonDS (the nanojava-emu image, docker/Dockerfile.emu)
# under a virtual X server, presses keys and saves screenshots of both
# screens.
#
#   sh tools/ds-smoke.sh ROM STEP...
#
# Each STEP is SECONDS[:KEY]: wait that long, take a screenshot, then press
# KEY (if given) for 0.3 s, or touch the bottom screen with tap,X,Y. Keys:
# x = A, z = B, s = X, a = Y, q = L, w = R, Return = Start,
# BackSpace = Select, Up/Down/Left/Right = D-pad.
# Screenshots: build/ds-0.png, build/ds-1.png, ...
#
# Example (from the repo root, ROM built with make nds-embed):
#   docker run --rm -v "$PWD":/src -w /src nanojava-emu \
#       sh tools/ds-smoke.sh build/nanojava-embed.nds 20:x 5:x 5
set -e
ROM=$1
shift

mkdir -p /tmp/home/.config /tmp/rt
export HOME=/tmp/home XDG_CONFIG_HOME=/tmp/home/.config XDG_RUNTIME_DIR=/tmp/rt
export DISPLAY=:99 QT_QPA_PLATFORM=xcb
Xvfb :99 -screen 0 1024x768x24 >/dev/null 2>&1 &
XVFB=$!
sleep 1

# Let melonDS write a default config, then map the keys (Qt key codes; a
# fresh config maps none) and expose build/sdcard as a DLDI SD card.
CFG=/tmp/home/.config/melonDS/melonDS.toml
(cd /tmp/home && exec melonDS >/dev/null 2>&1) &
FIRST=$!
i=0
while ! grep -q -F '[Instance0.Keyboard]' "$CFG" 2>/dev/null && [ $i -lt 60 ]; do
    sleep 0.5
    i=$((i + 1))
done
kill $FIRST 2>/dev/null || true
sleep 1
mkdir -p /src/build/sdcard
awk '
    BEGIN {
        keys = "A=88 B=90 X=83 Y=65 L=81 R=87 Start=16777220 Select=16777219"
        keys = keys " Left=16777234 Up=16777235 Right=16777236 Down=16777237"
        n = split(keys, m, " ")
        for (i = 1; i <= n; i++) { split(m[i], kv, "="); key[kv[1]] = kv[2] }
    }
    /^\[/ { section = $0 }
    section == "[Instance0.Keyboard]" && ($1 in key) { print $1 " = " key[$1]; next }
    section == "[DLDI]" { next }
    { print }
    END {
        print "[DLDI]"
        print "Enable = true"
        print "FolderSync = true"
        print "FolderPath = \"/src/build/sdcard\""
        print "ImagePath = \"/tmp/dldi.img\""
        print "ImageSize = 0"
    }
' "$CFG" > /tmp/cfg && mv /tmp/cfg "$CFG"

grep -A 6 -F '[DLDI]' "$CFG" >&2
# With AUDIO_OUT=file.wav, record melonDS's sound through a PulseAudio
# null sink.
if [ -n "$AUDIO_OUT" ]; then
    pulseaudio -D --exit-idle-time=-1 --system=false >/dev/null 2>&1 || true
    sleep 1
    pactl load-module module-null-sink sink_name=rec >/dev/null
    pactl set-default-sink rec
    parecord --device=rec.monitor --file-format=wav "/src/$AUDIO_OUT" &
    REC=$!
fi
(cd /tmp/home && melonDS "/src/$ROM" >/src/build/ds-emu.log 2>&1) &
sleep 2
WIN=$(xdotool search --name "melonDS 1" | head -1)

n=0
for step in "$@"; do
    secs=${step%%:*}
    key=${step#*:}
    [ "$key" = "$step" ] && key=
    sleep "$secs"
    # Crop away the menu bar: the two screens are the bottom 256x384.
    import -window "$WIN" -gravity south -crop 256x384+0+0 +repage "/src/build/ds-$n.png"
    n=$((n + 1))
    case "$key" in
    "") ;;
    tap,*)
        # tap,X,Y: touch the bottom screen at (X, Y).
        tx=$(echo "$key" | cut -d, -f2)
        ty=$(echo "$key" | cut -d, -f3)
        wh=$(xdotool getwindowgeometry "$WIN" | sed -n 's/.*Geometry: [0-9]*x\([0-9]*\).*/\1/p')
        xdotool mousemove --window "$WIN" "$tx" $((wh - 192 + ty))
        xdotool mousedown 1
        sleep 0.2
        xdotool mouseup 1
        ;;
    *)
        xdotool windowfocus --sync "$WIN" >/dev/null 2>&1 || true
        xdotool keydown "$key"
        sleep 0.3
        xdotool keyup "$key"
        ;;
    esac
done
[ -n "$REC" ] && kill -INT $REC 2>/dev/null && sleep 1
kill $XVFB 2>/dev/null || true
echo "saved $n screenshots: build/ds-0.png .. build/ds-$((n - 1)).png"
