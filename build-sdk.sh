#!/usr/bin/env bash
# Native Kotlin/Compose build. Gradle resolves Compose/Kotlin dependencies.
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Set ANDROID_HOME to an Android SDK with platform 35 and Build Tools 35.0.0}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
exec ./gradlew :app:assembleDebug "$@"
