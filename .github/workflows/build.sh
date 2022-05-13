#!/bin/bash

set -ex

cd "$(dirname "$(dirname "$0")")"/..

# Add stretch for Java 8
cat <<END > /etc/apt/sources.list.d/stretch.list
deb http://deb.debian.org/debian stretch main
deb http://security.debian.org/debian-security stretch/updates main
END


apt-get update -y
apt-get install -y --no-install-recommends openjdk-8-jdk make gcc g++ libc6-dev
./gradlew build --no-daemon

