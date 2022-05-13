#!/bin/bash

set -ex

cd "$(dirname "$(dirname "$0")")"/..

apt-get update -y
apt-get install -y --no-install-recommends openjdk-8-jdk make gcc g++ libc6-dev

case "$1" in
  "release")
    release
    ;;
  "build")
    build
    ;;
  *)
    echo "You have to specify release or build."
    exit 1
    ;

function release() {
  ./gradlew clean build publishToSonatype -Pversion=$VERSION -Psigning.gnupg.passphrase=$PASSPHRASE
}

function build() {
  ./gradlew build --no-daemon
}
