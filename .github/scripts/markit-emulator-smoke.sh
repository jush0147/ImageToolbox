#!/usr/bin/env bash
set -euo pipefail

PKG="com.e04stuff.markit.candidate"
ACTIVITY="com.t8rin.imagetoolbox.app.presentation.AppActivity"
COMPONENT="$PKG/$ACTIVITY"
AUTHORITY="com.e04stuff.markit.fileprovider.candidate"
OUT_DIR="build/markit-smoke"
mkdir -p "$OUT_DIR"

fail_with_logs() {
  adb logcat -d > "$OUT_DIR/logcat.txt" || true
  adb exec-out screencap -p > "$OUT_DIR/failure.png" || true
  adb shell dumpsys activity activities > "$OUT_DIR/activities.txt" || true
  echo "===== Markit smoke-test log tail ====="
  tail -n 200 "$OUT_DIR/logcat.txt" || true
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

  adb exec-out screencap -p > "$OUT_DIR/$stage.png" || true
}

dump_ui() {
  local target="$1"
  adb shell uiautomator dump /data/local/tmp/markit-window.xml >/dev/null
  adb pull /data/local/tmp/markit-window.xml "$target" >/dev/null
}

tap_text() {
  local wanted="$1"
  dump_ui "$OUT_DIR/window.xml"

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

tap_en_zh() {
  local english="$1"
  local chinese="$2"
  dump_ui "$OUT_DIR/localized.xml"
  if grep -Fq "text=\"$english\"" "$OUT_DIR/localized.xml" || grep -Fq "content-desc=\"$english\"" "$OUT_DIR/localized.xml"; then
    tap_text "$english"
  elif grep -Fq "text=\"$chinese\"" "$OUT_DIR/localized.xml" || grep -Fq "content-desc=\"$chinese\"" "$OUT_DIR/localized.xml"; then
    tap_text "$chinese"
  else
    echo "Could not find localized UI node: $english / $chinese"
    fail_with_logs
  fi
}

APK="$(find app/build/outputs/apk/foss/debug -name '*x86_64*.apk' | head -n 1)"
test -n "$APK"

adb install -r "$APK"
adb logcat -c
adb shell am force-stop "$PKG"

# 1. Cold launch catches Compose/Hilt/runtime startup failures.
adb shell am start -W -n "$COMPONENT"
sleep 3
assert_alive "launch"

# 2. Create a deterministic PNG, place it behind Markit's FileProvider, then
# enter through ACTION_SEND using a real content:// URI.
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

cat "$OUT_DIR/smoke.png" | adb shell run-as "$PKG" tee files/markit-smoke.png >/dev/null
IMAGE_URI="content://$AUTHORITY/files/markit-smoke.png"

adb logcat -c
adb shell am start -W \
  -n "$COMPONENT" \
  -a android.intent.action.SEND \
  -t image/png \
  --eu android.intent.extra.STREAM "$IMAGE_URI" \
  --grant-read-uri-permission
sleep 4
assert_alive "share-in"

dump_ui "$OUT_DIR/editor.xml"
grep -Eq 'text="Save"|text="儲存"' "$OUT_DIR/editor.xml" || {
  echo "Save action did not render"
  fail_with_logs
}
grep -Eq 'text="Share"|text="分享"' "$OUT_DIR/editor.xml" || {
  echo "Share action did not render"
  fail_with_logs
}

# 3. Exercise Smart Redact runtime. The synthetic image has no text, so the
# expected functional result is "nothing found", but ML Kit must initialize
# and complete without linkage/model/runtime failures.
adb logcat -c
tap_en_zh "Smart redact" "智慧遮蔽"
sleep 6
assert_alive "smart-redact"
adb logcat -d > "$OUT_DIR/logcat-smart-redact.txt"
if grep -E -q "MlKitException|TextRecognizer.*(Exception|ERROR)|com\.google\.mlkit.*(Exception|ERROR)" "$OUT_DIR/logcat-smart-redact.txt"; then
  echo "Smart Redact runtime reported an ML Kit failure"
  fail_with_logs
fi

# 4. Exercise crop overlay and apply a no-op crop. This covers cache URI,
# FileProvider and cropper runtime without depending on fragile drag geometry.
adb logcat -c
tap_en_zh "More options" "更多選項"
sleep 1
tap_en_zh "Crop" "裁切"
sleep 3
assert_alive "crop-open"
tap_en_zh "Apply crop" "套用裁切"
sleep 5
assert_alive "crop-apply"

# 5. Exercise drawing runtime.
adb logcat -c
adb shell input swipe 300 650 760 980 450
sleep 2
assert_alive "draw"

# 6. Save must create a real, readable MediaStore image in Pictures/Markit.
adb logcat -c
tap_en_zh "Save" "儲存"

saved_line=""
for _ in $(seq 1 20); do
  adb shell content query \
    --uri content://media/external/images/media \
    --projection _id:_display_name:relative_path:_size \
    > "$OUT_DIR/media-after-save.txt" 2>/dev/null || true

  saved_line="$(grep 'relative_path=Pictures/Markit/' "$OUT_DIR/media-after-save.txt" | grep '_display_name=Markit' | tail -n 1 || true)"
  if [ -n "$saved_line" ]; then
    break
  fi
  sleep 1
done

if [ -z "$saved_line" ]; then
  echo "Save did not create a Markit image in Pictures/Markit"
  cat "$OUT_DIR/media-after-save.txt" || true
  fail_with_logs
fi

saved_id="$(printf '%s\n' "$saved_line" | sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p')"
if [ -z "$saved_id" ]; then
  echo "Could not parse saved MediaStore row id"
  fail_with_logs
fi

saved_size="$(printf '%s\n' "$saved_line" | sed -n 's/.*_size=\([0-9][0-9]*\).*/\1/p')"
if [ -z "$saved_size" ] || [ "$saved_size" -le 100 ]; then
  echo "Saved MediaStore row is empty or implausibly small: ${saved_size:-unknown} bytes"
  fail_with_logs
fi
echo "Verified MediaStore save: id=$saved_id size=$saved_size bytes"
adb logcat -d > "$OUT_DIR/logcat-save.txt"
if grep -E -q "FATAL EXCEPTION|Unable to start activity|Process: $PKG.*PID" "$OUT_DIR/logcat-save.txt"; then
  echo "Fatal Android runtime error detected after save"
  fail_with_logs
fi

# 7. Share in a fresh editor. Require both a newly cached image and an actual
# Android chooser/resolver Activity, not merely a surviving Markit process.
adb shell am start -W \
  -n "$COMPONENT" \
  -a android.intent.action.SEND \
  -t image/png \
  --eu android.intent.extra.STREAM "$IMAGE_URI" \
  --grant-read-uri-permission
sleep 4
assert_alive "share-fresh"

adb shell run-as "$PKG" find cache -type f 2>/dev/null | tr -d '\r' | sort > "$OUT_DIR/cache-before-share.txt" || true
adb logcat -c
tap_en_zh "Share" "分享"
sleep 4

adb shell run-as "$PKG" find cache -type f 2>/dev/null | tr -d '\r' | sort > "$OUT_DIR/cache-after-share.txt" || true
comm -13 "$OUT_DIR/cache-before-share.txt" "$OUT_DIR/cache-after-share.txt" > "$OUT_DIR/cache-new-share.txt" || true
if [ ! -s "$OUT_DIR/cache-new-share.txt" ]; then
  echo "Share did not create a new cached output image"
  fail_with_logs
fi

adb shell dumpsys activity activities > "$OUT_DIR/share-activities.txt"
if ! grep -E -q "ChooserActivity|ResolverActivity|IntentResolver" "$OUT_DIR/share-activities.txt"; then
  echo "Android share chooser/resolver did not become active"
  fail_with_logs
fi

adb logcat -d > "$OUT_DIR/logcat-share.txt"
if grep -E -q "FATAL EXCEPTION|Unable to start activity|Process: $PKG.*PID|FileUriExposedException|IllegalArgumentException.*FileProvider" "$OUT_DIR/logcat-share.txt"; then
  echo "Share runtime reported a fatal provider/intent error"
  fail_with_logs
fi

echo "Markit latest-source runtime smoke passed."
