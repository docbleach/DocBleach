#!/bin/bash
set -e

coverity_scan() {
  if [ -n "${TRAVIS_TAG}" ] || [ "${TRAVIS_PULL_REQUEST}" = "true" ]
  then
    echo "Skipping coverity scan."
    return
  fi
  export COVERITY_SCAN_PROJECT_NAME="${TRAVIS_REPO_SLUG}"
  export COVERITY_SCAN_NOTIFICATION_EMAIL="punkeel@me.com"
  export COVERITY_SCAN_BUILD_COMMAND="mvn package"
  export COVERITY_SCAN_BUILD_COMMAND_PREPEND="mvn clean"
  export COVERITY_SCAN_BRANCH_PATTERN="master"
  curl -s "https://scan.coverity.com/scripts/travisci_build_coverity_scan.sh" | bash || :
}

mvn_deploy() {
    mvn deploy --settings scripts/deploy_settings.xml -DperformRelease=true -DskipTests=true
}

mvn_deploy
coverity_scan