#!/bin/bash

set -ex

cat <<END > /etc/apt/sources.list.d/stretch.list
deb http://deb.debian.org/debian stretch main
deb http://security.debian.org/debian-security stretch/updates main
END

cd "$(dirname "$(dirname "$0")")"

apt-get update -y
apt-get install -y --no-install-recommends openjdk-8-jdk-headless make gcc libc6-dev
cd ..
pwd
./gradlew build

