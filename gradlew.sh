#!/bin/bash
# Gradle Wrapper para macOS/Linux

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
GRADLE_VERSION=7.3.3

if [ ! -f "$DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
  echo "Descargando Gradle Wrapper..."
  mkdir -p "$DIR/gradle/wrapper"
  curl -L "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$DIR/gradle/wrapper/gradle-${GRADLE_VERSION}-bin.zip"
  unzip -q "$DIR/gradle/wrapper/gradle-${GRADLE_VERSION}-bin.zip" -d "$DIR/gradle/wrapper/"
fi

exec "$DIR/gradle/wrapper/gradle-${GRADLE_VERSION}/bin/gradle" "$@"
