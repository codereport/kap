package com.dhsdevelopments.kap.benchmark

import array.*
import array.csv.CsvWriter
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
    return BenchmarkTestCase("var lookup scope", "{ a←⍵ ◊ {a+⍺+⍵}/⍳1000000 } 4")
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

private fun benchmarkFormatter(): BenchmarkTestCase {
    val srcString = """
        |use("output3.kap")
        |o3:format 200 20 ⍴ 10 100 "foo" (2 2 ⍴ ⍳10)
    """.trimMargin()
    return BenchmarkTestCase("formatter", srcString)
}

class TestCaseResults(val name: String, val results: List<Long>) {
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

class BenchmarkResult(val name: String, testcases: List<BenchmarkTestCase>)

fun benchmarkSrc(name: String, srcString: String, libPath: String): TestCaseResults {
    val engine = Engine()
    engine.addLibrarySearchPath(libPath)
    engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"))
    val warmupIterations = 5
    val iterations = 10
    val results = ArrayList<Long>()
    repeat(warmupIterations + iterations) { i ->
        val elapsed = measureTimeMillis {
            val result = engine.parseAndEval(StringSourceLocation(srcString))
            result.collapse()
        }
        if (i >= warmupIterations) {
            results.add(elapsed)
        }
//        println("Result${if (i < warmupIterations) " (warmup)" else ""}: ${elapsed}")
    }
    return TestCaseResults(name, results)
}

fun runAllTests(name: String, libPath: String, reportPath: String, reportName: String): BenchmarkResult {
    val type = fileType(reportPath)
    if (type == null) {
        createDirectory(reportPath)
    } else if (type != FileNameType.DIRECTORY) {
        throw IllegalStateException("Report directory is a file: ${reportPath}")
    }

    val tests = listOf(benchmarkPrimes(), benchmarkVarLookupScope(), contribBench(), simpleSum(), benchmarkMultipleCall(), benchmarkFormatter())
    println("Running tests: ${name}")
    val results = ArrayList<TestCaseResults>()
    tests.forEach { testcase ->
        val result = benchmarkSrc(testcase.name, testcase.src, libPath)
        println("${testcase.name}: ${result.summary()}")
        results.add(result)
    }

    openOutputCharFile("${reportPath}/benchmark-${reportName}-${name}.csv").use { output ->
        val writer = CsvWriter(output)
        results.forEach { testcase ->
            val row = arrayListOf(testcase.name)
            row.addAll(testcase.results.map(Long::toString))
            writer.writeRow(row)
        }
    }

    return BenchmarkResult(name, tests)
}
