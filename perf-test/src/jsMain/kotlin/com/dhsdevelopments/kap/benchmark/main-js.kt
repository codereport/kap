package com.dhsdevelopments.kap.benchmark

import array.RegisteredEntry
import array.registeredFilesRoot

/*
Running jsNodeRun:
Running tests: js
primes: avg=7610.3, median=7595, min=7535, max=7700, stddev=1.0000203429011993
var lookup scope: avg=5933.8, median=5948, min=5882, max=5992, stddev=1.0000169689073763
contrib bench: avg=1441.5, median=1454.5, min=1427, max=1467, stddev=1.0000327846161194
simple sum: avg=1161.6, median=1169, min=1152, max=1179, stddev=1.0000278063243908
multiple call: avg=555.6, median=557, min=550, max=560, stddev=1.0000126986874875

With overflow bignum:
Running tests: js
primes: avg=9697.8, median=9868.5, min=9582, max=10031, stddev=1.0000777406036818
var lookup scope: avg=9947.8, median=9916.5, min=9913, max=9995, stddev=1.000002908076898
contrib bench: avg=1954.6, median=1950.5, min=1948, max=1962, stddev=1.000001392501081
simple sum: avg=14390.3, median=14443, min=14347, max=14486, stddev=1.0000032538222676
multiple call: avg=1298.9, median=1300, min=1295, max=1301, stddev=1.0000008564780307

Remove extra try/catch in addExact:
Running tests: js
primes: avg=9093.8, median=9212, min=8941, max=9213, stddev=1.0000605477393347
var lookup scope: avg=9686.8, median=9747.5, min=9581, max=9815, stddev=1.0000338024938362
contrib bench: avg=1920.5, median=1919.5, min=1905, max=1935, stddev=1.0000128309618421
simple sum: avg=14367.5, median=14368, min=14270, max=14511, stddev=1.0000096568479195
multiple call: avg=1325.4, median=1318, min=1317, max=1337, stddev=1.0000112256252656
*/

var jsFilesystem: dynamic = js("require('fs')")

fun main() {
    loadFs()
    runAllTests("js", "standard-lib", "benchmark-reports", "unnamed")
}

fun loadFs() {
    fun readFileRecurse(fsDir: String, dir: RegisteredEntry.Directory) {
        val files = jsFilesystem.readdirSync(fsDir) as Array<String>
        files.forEach { name ->
            val newName = "${fsDir}/${name}"
            val result = jsFilesystem.statSync(newName)
            when {
                result.isDirectory() -> readFileRecurse(newName, dir.createDirectory(name, false))
                result.isFile() -> {
                    val content = jsFilesystem.readFileSync(newName)
                    dir.registerFile(name, content)
                }
            }
        }
    }

    fun initDirectory(fsDir: String, base: String) {
        readFileRecurse("${fsDir}/${base}", registeredFilesRoot.createDirectory(base, errorIfExists = false))
    }

    initDirectory("../../../../array", "standard-lib")
    initDirectory("../../../../array", "test-data")
}
