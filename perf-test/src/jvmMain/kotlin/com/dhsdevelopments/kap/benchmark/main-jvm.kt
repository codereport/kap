package com.dhsdevelopments.kap.benchmark

/*
Running tests: JVM
primes: avg=255.1, median=251.5, min=249, max=280, stddev=1.0005767743846692
var lookup scope: avg=1545.0, median=1545.0, min=1522, max=1574, stddev=1.000038582848229
contrib bench: avg=235.9, median=239.5, min=232, max=242, stddev=1.0000619041696195
simple sum: avg=193.5, median=191.0, min=191, max=206, stddev=1.0002543594139597
multiple call: avg=140.3, median=139.0, min=138, max=153, stddev=1.0004675272623051
*/

fun main() {
    runAllTests("JVM", "array/standard-lib")
}
