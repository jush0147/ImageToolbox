#!/usr/bin/env bash
set -euo pipefail

PKG="com.e04stuff.markit.debug"
ACTIVITY="com.t8rin.imagetoolbox.app.presentation.AppActivity"
COMPONENT="$PKG/$ACTIVITY"
OUT_DIR="build/markit-smoke"
mkdir -p "$OUT_DIR"

fail_with_logs() {
  adb logcat -d > "$OUT_DIR/logcat.txt" || true
  adb exec-out screencap -p > "$OUT_DIR/failure.png" || true
  adb shell dumpsys activity activities > "$OUT_DIR/activities.txt" || true
  echo "===== Markit smoke-test log tail ====="
  tail -n 160 "$OUT_DIR/logcat.txt" || true
  exit 1
}

assert_alive() {
  local stage="$1"
  local pid
  pid="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
  if [ -z "$pid" ]; then
    echo "Markit process is not alive after: $stage"
    fail_with_logs
  fi

  adb logcat -d > "$OUT_DIR/logcat-$stage.txt"
  if grep -E -q "FATAL EXCEPTION|Unable to start activity|Process: $PKG.*PID" "$OUT_DIR/logcat-$stage.txt"; then
    echo "Fatal Android runtime error detected after: $stage"
    fail_with_logs
  fi

  adb exec-out screencap -p > "$OUT_DIR/$stage.png"
}

tap_text() {
  local wanted="$1"
  adb shell uiautomator dump /sdcard/markit-window.xml >/dev/null
  adb pull /sdcard/markit-window.xml "$OUT_DIR/window.xml" >/dev/null

  local point
  point="$(python3 - "$OUT_DIR/window.xml" "$wanted" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path, wanted = sys.argv[1], sys.argv[2]
root = ET.parse(path).getroot()
for node in root.iter("node"):
    text = node.attrib.get("text", "")
    desc = node.attrib.get("content-desc", "")
    if text == wanted or desc == wanted:
        m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            raise SystemExit(0)
raise SystemExit(2)
PY
)" || {
    echo "Could not find UI node: $wanted"
    fail_with_logs
  }

  read -r x y <<< "$point"
  adb shell input tap "$x" "$y"
}

APK="$(find app/build/outputs/apk/foss/debug -name '*x86_64*.apk' | head -n 1)"
test -n "$APK"

adb install -r "$APK"
adb logcat -c
adb shell am force-stop "$PKG"

# 1. Cold launch: catches Compose/runtime failures that compilation cannot.
adb shell am start -W -n "$COMPONENT"
sleep 4
assert_alive "launch"

# Create a real image without external dependencies.
python3 - "$OUT_DIR/smoke.png" <<'PY'
import struct
import sys
import zlib

path = sys.argv[1]
w, h = 1080, 1600
rows = []
for y in range(h):
    row = bytearray([0])
    for x in range(w):
        if 180 < x < 900 and 500 < y < 700:
            row.extend((245, 201, 69, 255))
        else:
            row.extend((245, 245, 245, 255))
    rows.append(bytes(row))

def chunk(kind, data):
    return (
        struct.pack(">I", len(data))
        + kind
        + data
        + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)
    )

png = (
    b"\x89PNG\r\n\x1a\n"
    + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    + chunk(b"IDAT", zlib.compress(b"".join(rows), 6))
    + chunk(b"IEND", b"")
)
open(path, "wb").write(png)
PY

adb push "$OUT_DIR/smoke.png" /sdcard/Pictures/markit-smoke.png >/dev/null
adb shell am broadcast \
  -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
  -d file:///sdcard/Pictures/markit-smoke.png >/dev/null
sleep 2

MEDIA_ID="$(adb shell content query \
  --uri content://media/external/images/media \
  --projection _id:_display_name \
  --where "_display_name='markit-smoke.png'" \
  | tr -d '\r' \
  | sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' \
  | head -n 1)"

if [ -z "$MEDIA_ID" ]; then
  echo "Could not resolve smoke image in MediaStore"
  fail_with_logs
fi

IMAGE_URI="content://media/external/images/media/$MEDIA_ID"

# 2. Real share-in path.
adb logcat -c
adb shell am start -W \
  -n "$COMPONENT" \
  -a android.intent.action.SEND \
  -t image/png \
  --eu android.intent.extra.STREAM "$IMAGE_URI" \
  --grant-read-uri-permission
sleep 5
assert_alive "share-in"

# Confirm the redesigned editor actually rendered the two equal primary outcomes.
adb shell uiautomator dump /sdcard/markit-editor.xml >/dev/null
adb pull /sdcard/markit-editor.xml "$OUT_DIR/editor.xml" >/dev/null
grep -Eq 'text="Save"|text="儲存"' "$OUT_DIR/editor.xml" || {
  echo "Save action did not render"
  fail_with_logs
}
grep -Eq 'text="Share"|text="分享"' "$OUT_DIR/editor.xml" || {
  echo "Share action did not render"
  fail_with_logs
}

# 3. Exercise drawing runtime. Arrow is the default tool; draw inside the canvas.
adb logcat -c
adb shell input swipe 300 650 760 980 450
sleep 3
assert_alive "draw"

# 4. Exercise save runtime.
adb logcat -c
if grep -q 'text="Save"' "$OUT_DIR/editor.xml"; then
  tap_text "Save"
else
  tap_text "儲存"
fi
sleep 5
assert_alive "save"

# 5. Exercise share runtime. A system chooser may take focus; Markit's process must survive.
adb logcat -c
adb shell input keyevent KEYCODE_BACK || true
sleep 1
adb shell uiautomator dump /sdcard/markit-before-share.xml >/dev/null
adb pull /sdcard/markit-before-share.xml "$OUT_DIR/before-share.xml" >/dev/null
if grep -q 'text="Share"' "$OUT_DIR/before-share.xml"; then
  tap_text "Share"
else
  tap_text "分享"
fi
sleep 5
assert_alive "share"

adb shell dumpsys activity activities > "$OUT_DIR/activities-final.txt"
adb logcat -d > "$OUT_DIR/logcat-final.txt"

echo "Markit emulator smoke test passed."
