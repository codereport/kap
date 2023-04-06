package com.dhsdevelopments.kap.benchmark

/*
Running tests: JVM
primes: avg=572.1, median=573.5, min=550, max=590, stddev=1.0002836305713394
var lookup scope: avg=2667.2, median=2661.0, min=2637, max=2710, stddev=1.0000373596689864
contrib bench: avg=394.1, median=397.5, min=376, max=408, stddev=1.0003685060966
simple sum: avg=347.5, median=347.5, min=336, max=393, stddev=1.0010141330420723
multiple call: avg=13163.6, median=13179.5, min=12900, max=13378, stddev=1.000044230923112
*/

fun main() {
    runAllTests("JVM")
}
