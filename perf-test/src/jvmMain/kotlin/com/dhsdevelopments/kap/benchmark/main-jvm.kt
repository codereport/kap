package com.dhsdevelopments.kap.benchmark

/*
Basline:
Running tests: JVM
primes: avg=255.1, median=251.5, min=249, max=280, stddev=1.0005767743846692
var lookup scope: avg=1545.0, median=1545.0, min=1522, max=1574, stddev=1.000038582848229
contrib bench: avg=235.9, median=239.5, min=232, max=242, stddev=1.0000619041696195
simple sum: avg=193.5, median=191.0, min=191, max=206, stddev=1.0002543594139597
multiple call: avg=140.3, median=139.0, min=138, max=153, stddev=1.0004675272623051

With overflow bignum:
Running tests: JVM
primes: avg=243.7, median=244.0, min=243, max=245, stddev=1.0000034517716154
var lookup scope: avg=1432.9, median=1426.0, min=1424, max=1442, stddev=1.0000089834869959
contrib bench: avg=224.7, median=224.5, min=223, max=227, stddev=1.00001990469809
simple sum: avg=205.1, median=204.5, min=203, max=215, stddev=1.0001341848511105
multiple call: avg=132.0, median=131.0, min=130, max=134, stddev=1.000040173665031
*/

fun main() {
    runAllTests("JVM", "array/standard-lib")
}
