#!/bin/bash

set -ex

cd "$(dirname "$(dirname "$0")")"/..

echo "ULIMIT1233: "
ulimit -l
apt-get update -y
apt-get install -y --no-install-recommends openjdk-8-jdk make gcc g++ libc6-dev
./gradlew build --no-daemon --debug
