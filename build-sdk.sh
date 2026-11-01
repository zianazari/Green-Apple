#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Set ANDROID_HOME to an SDK with platform android-35 and build-tools 35.0.0}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
BT="$ANDROID_HOME/build-tools/35.0.0"
SRC="$PWD/app/src/main"
BUILD="$PWD/build-local"
mkdir -p "$BUILD/classes" "$BUILD/dex"
"$BT/aapt2" compile --dir "$SRC/res" -o "$BUILD/res.zip"
"$BT/aapt2" link -o "$BUILD/base.apk" -I "$ANDROID_HOME/platforms/android-35/android.jar" --manifest "$SRC/AndroidManifest.xml" --java "$BUILD/generated" -A "$SRC/assets" "$BUILD/res.zip"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -source 17 -target 17 -classpath "$ANDROID_HOME/platforms/android-35/android.jar" -d "$BUILD/classes" "$SRC/java/com/sabz/companion/"*.java "$BUILD/generated/com/sabz/companion/R.java"
find "$BUILD/classes" -name '*.class' > "$BUILD/classes-list.txt"
mapfile_available=0
# Paths are read into an array to preserve spaces on macOS bash 3.2 as well.
class_files=()
while IFS= read -r class_file; do class_files+=("$class_file"); done < "$BUILD/classes-list.txt"
"$BT/d8" --lib "$ANDROID_HOME/platforms/android-35/android.jar" --min-api 30 --output "$BUILD/dex" "${class_files[@]}"
cp "$BUILD/base.apk" "$BUILD/unsigned.apk"
(cd "$BUILD/dex" && zip -q "$BUILD/unsigned.apk" classes.dex)
"$BT/zipalign" -f -p 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
if [ ! -f "$BUILD/development.jks" ]; then
 "$JAVA_HOME/bin/keytool" -genkeypair -keystore "$BUILD/development.jks" -storepass android -keypass android -alias development -dname 'CN=Sabz Development' -keyalg RSA -keysize 2048 -validity 10000
fi
"$BT/apksigner" sign --ks "$BUILD/development.jks" --ks-pass pass:android --out "$BUILD/Green-Apple-1.4.0.apk" "$BUILD/aligned.apk"
"$BT/apksigner" verify --verbose "$BUILD/Green-Apple-1.4.0.apk"
