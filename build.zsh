#!/usr/bin/env zsh
set -euo pipefail

# Build OMX-27.bwextension with javac. Run from zsh so jenv / login hooks set JAVA_HOME.
ROOT="${0:A:h}"
SRC_JAVA="$ROOT/src/main/java"
RES="$ROOT/src/main/resources"
CLASSES="$ROOT/build/classes"
OUT_EXT="$ROOT/build"
BW_JAR="/Applications/Bitwig Studio.app/Contents/Java/bitwig.jar"

if [[ ! -r $BW_JAR ]]; then
  print -u2 "Missing Bitwig API jar: $BW_JAR"
  exit 1
fi

rm -rf "$CLASSES"
mkdir -p "$CLASSES" "$OUT_EXT"
find "$SRC_JAVA" -name '*.java' -print0 | xargs -0 javac \
  --release 8 \
  -encoding UTF-8 \
  -Xlint:-options \
  -d "$CLASSES" \
  -cp "$BW_JAR"

cp -R "$RES/META-INF" "$CLASSES/"

BWEXT="$OUT_EXT/OMX-27.bwextension"
rm -f "$BWEXT"
( cd "$CLASSES" && jar cf "$BWEXT" . )

print "Built $BWEXT"
