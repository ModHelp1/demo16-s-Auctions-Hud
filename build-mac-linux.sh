#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
echo "=== demo16's Auction's Hud - one-command build ==="

if ! command -v java >/dev/null 2>&1; then
  echo "Java not found. Install JDK 21 from https://adoptium.net and run again."; exit 1
fi
V=$(java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
if [ "$V" -lt 21 ]; then
  echo "Java $V is too old. You need JDK 21+: https://adoptium.net"; exit 1
fi

if [ ! -x .gradle-dist/gradle-8.8/bin/gradle ]; then
  echo "Downloading Gradle 8.8 (one time only)..."
  mkdir -p .gradle-dist
  curl -L -o gradle.zip https://services.gradle.org/distributions/gradle-8.8-bin.zip
  unzip -q -o gradle.zip -d .gradle-dist
  rm gradle.zip
fi

echo "Building... this takes a few minutes the first time."
.gradle-dist/gradle-8.8/bin/gradle build --no-daemon

mkdir -p MOD-JAR
cp build/libs/demo16s-auctions-hud-1.0.0.jar MOD-JAR/
echo
echo "DONE! Your mod: MOD-JAR/demo16s-auctions-hud-1.0.0.jar"
