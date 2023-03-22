#!/bin/sh

set -e

asciidoctor -a toc ./website/src/orchid/resources/pages/kap-comparison.asciidoc
asciidoctor -a toc ./website/src/orchid/resources/pages/tutorial.asciidoc

scp -r website/src/orchid/resources/pages/kap-comparison.html \
       website/src/orchid/resources/pages/tutorial.html \
       website/src/orchid/resources/pages/diagrams \
        root@matrix.dhsdevelopments.com:/var/www/kapdemo

./gradlew clientweb:browserDistribution
scp -r clientweb/build/distributions/* \
       array/standard-lib \
       root@matrix.dhsdevelopments.com:/var/www/kapdemo/clientweb

scripts/buildsite.sh

scp -r clientweb2/build/distributions/* \
       root@matrix.dhsdevelopments.com:/var/www/kapdemo/clientweb2
