#!/bin/bash

mvn deploy --settings scripts/deploy_settings.xml -DperformRelease=true -DskipTests=true
exit $?