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
*/

var jsFilesystem: dynamic = js("require('fs')")

fun main() {
    loadFs()
    runAllTests("js", "standard-lib")
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
