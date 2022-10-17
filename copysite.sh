#!/bin/sh

set -e

asciidoctor -a toc ./website/src/orchid/resources/pages/kap-comparison.asciidoc
asciidoctor -a toc ./website/src/orchid/resources/pages/tutorial.asciidoc

scp -r /home/elias/prog/array_kotlin/website/src/orchid/resources/pages/kap-comparison.html \
       /home/elias/prog/array_kotlin/website/src/orchid/resources/pages/tutorial.html \
       /home/elias/prog/array_kotlin/website/src/orchid/resources/pages/diagrams \
        root@matrix.dhsdevelopments.com:/var/www/kapdemo

./gradlew clientweb:browserDistribution
scp -r /home/elias/prog/array_kotlin/clientweb/build/distributions/* \
       /home/elias/prog/array_kotlin/array/standard-lib \
       root@matrix.dhsdevelopments.com:/var/www/kapdemo/clientweb

./buildsite.sh

scp -r /home/elias/prog/array_kotlin/clientweb2/build/distributions/* \
       root@matrix.dhsdevelopments.com:/var/www/kapdemo/clientweb2
