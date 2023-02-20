#!/bin/sh

set -e

./gradlew clientweb2:webworkerBrowserDistribution
./gradlew clientweb2:jsBrowserDistribution
cp -r array/standard-lib clientweb2/build/distributions
