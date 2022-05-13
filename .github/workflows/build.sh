#!/bin/bash

set -ex

cd "$(dirname "$(dirname "$0")")"/..

apt-get update -y
apt-get install -y --no-install-recommends openjdk-8-jdk make gcc g++ libc6-dev

release() {
  apt-get install -y --no-install-recommends gnupg
  export GPG_TTY=$(tty)
  ls -la /work
  gpg --batch --import /work/release.gpg
  gpg --list-secret-keys --keyid-format LONG
  ./gradlew clean build publishToSonatype -Pversion=$VERSION -Psigning.gnupg.passphrase=$PASSPHRASE
}

build() {
  ./gradlew build --no-daemon
}

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
esac
