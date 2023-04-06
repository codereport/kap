package com.dhsdevelopments.kap.benchmark

/*
Debug executable:
Running tests: linux
primes: avg=9104.8, median=10024.0, min=8634, max=10438, stddev=1.0020157531761062

Running tests: linux
primes: avg=3142.6, median=3299.0, min=2249, max=3422, stddev=1.0062743352550496
var lookup scope: avg=18161.9, median=16816.0, min=11784, max=24409, stddev=1.0269002758703367
contrib bench: avg=2773.9, median=3604.0, min=1423, max=3605, stddev=1.0645374633681086
simple sum: avg=1072.1, median=1068.5, min=1059, max=1104, stddev=1.0000740751386223
multiple call: avg=985.7, median=997.5, min=966, max=999, stddev=1.0000765765823796
*/

fun main() {
    runAllTests("linux", "../array/standard-lib")
}
