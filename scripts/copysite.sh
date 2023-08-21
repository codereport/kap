#!/bin/sh

set -e

asciidoctor -a toc ./docs/kap-comparison.asciidoc
asciidoctor -a toc ./docs/tutorial.asciidoc
asciidoctor -a toc ./docs/quick-tutorial.asciidoc
asciidoctor -a toc ./docs/reference.asciidoc

scp -r docs/kap-comparison.html \
       docs/tutorial.html \
       docs/quick-tutorial.html \
       docs/reference.html \
       docs/diagrams \
        root@matrix.dhsdevelopments.com:/var/www/kapdemo

#./gradlew clientweb:browserDistribution
#scp -r clientweb/build/distributions/* \
#       array/standard-lib \
#       root@matrix.dhsdevelopments.com:/var/www/kapdemo/clientweb

scripts/buildsite.sh

scp -r clientweb2/build/dist/jsclient/productionExecutable/* \
       root@matrix.dhsdevelopments.com:/var/www/kapdemo/clientweb2
scp -r clientweb2/build/dist/webworker/productionExecutable/* \
       root@matrix.dhsdevelopments.com:/var/www/kapdemo/clientweb2
scp -r array/standard-lib \
       root@matrix.dhsdevelopments.com:/var/www/kapdemo/clientweb2
