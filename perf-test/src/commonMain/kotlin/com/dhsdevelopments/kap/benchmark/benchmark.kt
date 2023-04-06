package com.dhsdevelopments.kap.benchmark

import array.Engine
import array.StringSourceLocation
import array.currentTime
import kotlin.math.pow
import kotlin.math.sqrt

class BenchmarkTestCase(val name: String, val src: String)

inline fun measureTimeMillis(fn: () -> Unit): Long {
    val startTime = currentTime()
    fn()
    val endTime = currentTime()
    return endTime - startTime
}

private fun benchmarkPrimes(): BenchmarkTestCase {
    val srcString = """
            |+/ (⍳N) /⍨ {~0∊⍵|⍨1↓1+⍳√⍵}¨ ⍳N←200000
        """.trimMargin()
    // N←1000
    // Default: 0.548
    // Specialised find result value: 0.072
    // N←4000
    // With previous opt: 4.191
    return BenchmarkTestCase("primes", srcString)
}

private fun benchmarkVarLookupScope(): BenchmarkTestCase {
    // Pre-rewrite: 1.3536
    // Orig: 1.2875
    // removed redundant: 1.0207
    // New stack: 0.9316
    // Storage list in array: 0.9357000000000001
    // Standalone stack allocation: 0.9074
    return BenchmarkTestCase("var lookup scope", "{ a←⍵ ◊ {a+⍺+⍵}/⍳10000000 } 4")
}

private fun contribBench(): BenchmarkTestCase {
    // Basic bignum: 0.3519
    return BenchmarkTestCase("contrib bench", "+/{+/⍵(⍵+1)}¨⍳1000000")
}

private fun simpleSum(): BenchmarkTestCase {
    // Basic bignum: 0.36469999999999997
    return BenchmarkTestCase("simple sum", "+/⍳100000000")
}

private fun benchmarkMultipleCall(): BenchmarkTestCase {
    val srcString = """
            |f ⇐ {⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵}
            |({f 5}⍣200000) 0
        """.trimMargin()
    // Pre-rewrite: 3.4658
    // Orig: 5.375100000000001
    // removed redundant lookup: 3.7815
    // New stack: 3.3473
    // Storage list in array: 3.2721
    return BenchmarkTestCase("multiple call", srcString)
}

class BenchmarkResults(val results: List<Long>) {
    fun avg() = results.sum() / results.size.toDouble()
    fun max() = results.max()
    fun min() = results.min()

    fun median(): Double {
        return (results[(results.size - 1) / 2] + results[results.size / 2]) / 2.0
    }

    fun stddev(): Double {
        val avg = avg()
        return sqrt(results.sumOf { v -> (v / avg).pow(2) } / results.size)
    }

    fun summary(): String {
        return "avg=${avg()}, median=${median()}, min=${min()}, max=${max()}, stddev=${stddev()}"
    }
}

fun benchmarkSrc(srcString: String, libPath: String): BenchmarkResults {
    val engine = Engine()
    engine.addLibrarySearchPath(libPath)
    engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"))
    val iterations = 10
    repeat(iterations) {
        val result = engine.parseAndEval(StringSourceLocation(srcString))
        result.collapse()
    }
    val results = ArrayList<Long>()
    repeat(iterations) {
        val elapsed = measureTimeMillis {
            val result = engine.parseAndEval(StringSourceLocation(srcString))
            result.collapse()
        }
        results.add(elapsed)
    }
    return BenchmarkResults(results)
}

fun runAllTests(name: String, libPath: String) {
    val tests = listOf(benchmarkPrimes(), benchmarkVarLookupScope(), contribBench(), simpleSum(), benchmarkMultipleCall())
    println("Running tests: ${name}")
    tests.forEach { testcase ->
        val results = benchmarkSrc(testcase.src, libPath)
        println("${testcase.name}: ${results.summary()}")
    }
}
