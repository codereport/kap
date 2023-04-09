package array

import kotlin.system.measureTimeMillis

private fun benchmarkPrimes(): String {
    val srcString = """
            |+/ (⍳N) /⍨ {~0∊⍵|⍨1↓1+⍳√⍵}¨ ⍳N←200000
        """.trimMargin()
    // N←1000
    // Default: 0.548
    // Specialised find result value: 0.072
    // N←4000
    // With previous opt: 4.191
    return srcString
}

private fun benchmarkVarLookupScope(): String {
    // Pre-rewrite: 1.3536
    // Orig: 1.2875
    // removed redundant: 1.0207
    // New stack: 0.9316
    // Storage list in array: 0.9357000000000001
    // Standalone stack allocation: 0.9074
    return "{ a←⍵ ◊ {a+⍺+⍵}/⍳10000000 } 4"
}

private fun contribBench(): String {
    // Basic bignum: 0.3519
    return "+/{+/⍵(⍵+1)}¨⍳1000000"
}

private fun simpleSum(): String {
    // Basic bignum: 0.36469999999999997
    return "+/⍳100000000"
}

private fun benchmarkMultipleCall(): String {
    val srcString = """
            |f ⇐ {⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵}
            |({f 5}⍣10000000) 0
        """.trimMargin()
    // Pre-rewrite: 3.4658
    // Orig: 5.375100000000001
    // removed redundant lookup: 3.7815
    // New stack: 3.3473
    // Storage list in array: 3.2721
    return srcString
}

fun main() {
    val engine = Engine()
    engine.addLibrarySearchPath("array/standard-lib")
    engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"))
    val srcString = simpleSum()
    println("Starting")
    val iterations = 10
    repeat(iterations) {
        val result = engine.parseAndEval(StringSourceLocation(srcString))
        result.collapse()
    }
    val elapsed = measureTimeMillis {
        repeat(iterations) {
            val result = engine.parseAndEval(StringSourceLocation(srcString))
            result.collapse()
        }
    }
    println("Elapsed: ${elapsed / iterations.toDouble() / 1000.0}")
}
