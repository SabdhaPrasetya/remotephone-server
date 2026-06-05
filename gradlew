#!/bin/sh
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
GRADLE_HOME="${GRADLE_USER_HOME}/wrapper/dists"
APP_HOME="$(cd "$(dirname "$0")" && pwd)"

# Use gradle wrapper jar if exists, else download
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Downloading Gradle wrapper..."
  curl -L -o "$WRAPPER_JAR" \
    "https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar"
fi

exec java -jar "$WRAPPER_JAR" "$@"
