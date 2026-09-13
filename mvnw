#!/bin/sh

MAVEN_PARENT="${HOME}/.m2/wrapper/dists/apache-maven-3.9.16"
MAVEN_HOME="$(find "$MAVEN_PARENT" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | head -n 1)"

if [ -x "$MAVEN_HOME/bin/mvn" ]; then
  exec "$MAVEN_HOME/bin/mvn" "$@"
fi

echo "Maven Wrapper distribution was not found." >&2
exit 1