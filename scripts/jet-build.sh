#!/bin/sh
if [ -z "${TRAVIS_TAG}" ] || [ "${TRAVIS_PULL_REQUEST}" = "true" ]
  then
    echo "Skipping the Excelsior Jet build."
    return
fi
if [ -z "${JET_URL}" ]
  then
    echo "Unknown JET_URL variable, skipping build."
    return
fi

wget -q --no-check-certificate -O jet-1130-pro-en-linux-x86.bin "${JET_URL}"
chmod +x ./jet-1130-pro-en-linux-x86.bin
./jet-1130-pro-en-linux-x86.bin -batch -no-aftrun
export JET_HOME="`pwd`/jet11.3-pro-x86/"
mvn jet:testrun jet:build
