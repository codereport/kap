package array

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class APLGenericException(message: String, val pos: Position? = null, cause: Throwable? = null) : Exception(message, cause) {
    var extendedDescription: String? = null

    fun formattedError(): String {
        val buf = StringBuilder()
        if (pos != null) {
            buf.append("Error at: ${pos.line + 1}:${pos.col}: ")
            if (pos.callerName != null) {
                buf.append("${pos.callerName}: ")
            }
        } else {
            buf.append("Error: ")
        }
        if (message != null) {
            buf.append(message)
        } else {
            buf.append("no message")
        }
        return buf.toString()
    }

    override fun toString() = formattedError()
}

fun <T : APLGenericException> T.details(description: String): T {
    if (extendedDescription != null) {
        throw IllegalStateException("Extended description already set")
    }
    extendedDescription = description
    return this
}

class APLEvaluationInterrupted(pos: Position? = null) : APLGenericException("Interrupted", pos)

open class APLEvalException(message: String, pos: Position? = null, cause: Throwable? = null) : APLGenericException(message, pos, cause) {
    var callStack: List<StorageStack.StackFrameDescription>? = null
}

open class IncompatibleTypeException(message: String, pos: Position? = null) : APLEvalException(message, pos)
class InvalidDimensionsException(message: String, pos: Position? = null) : APLEvalException(message, pos) {
    constructor(aDimensions: Dimensions, bDimensions: Dimensions, pos: Position)
            : this("Mismatched dimensions. a: ${aDimensions}, b: ${bDimensions}", pos)
}

class APLIndexOutOfBoundsException(message: String, pos: Position? = null) : APLEvalException("Index out of bounds: ${message}", pos)
class VariableNotAssigned(name: Symbol, pos: Position? = null) : APLEvalException("Variable not assigned: ${name.nameWithNamespace}", pos)
class IllegalAxisException(message: String, pos: Position?) : APLEvalException(message, pos) {
    constructor(axis: Int, dimensions: Dimensions, pos: Position? = null)
            : this("Axis $axis is not valid. Expected: ${dimensions.size}", pos)
}

class AxisNotSupported(pos: Position) : APLEvalException("Function does not support axis specifier", pos)

class APLIllegalArgumentException(message: String, pos: Position? = null) : APLEvalException(message, pos)
class APLIncompatibleDomainsException(message: String, pos: Position? = null, cause: Throwable? = null) :
        APLEvalException(message, pos, cause)

class Unimplemented1ArgException(pos: Position? = null) : APLEvalException("Function cannot be called with one argument", pos)
class Unimplemented2ArgException(pos: Position? = null) : APLEvalException("Function cannot be called with two arguments", pos)
class IllegalArgumentNumException(expectedCount: Int, receivedCount: Int, pos: Position? = null) :
        APLEvalException("Expected a list of ${expectedCount} values. Actual elements: ${receivedCount}", pos)

class IntMagnitudeException(value: Long, pos: Position? = null) : APLEvalException("Value does not fit in an int: ${value}", pos)
class InverseNotAvailable(pos: Position? = null) : APLEvalException("Function does not have an inverse", pos)

class LeftAssigned2ArgException(pos: Position? = null) :
        APLEvalException("Left assigned functions cannot be called with two arguments", pos)

class ArraySizeException(d: IntArray, pos: Position? = null) :
        APLEvalException("Array too large. Requested size: [${d.joinToString(", ")}]", pos)

class StructuralUnderNotSupported(pos: Position? = null) : APLEvalException("under not supported for function", pos)

open class ParseException(message: String, pos: Position? = null) : APLGenericException(message, pos)
class UnexpectedSymbol(ch: Int, pos: Position? = null) :
        ParseException("Unexpected symbol: '${charToString(ch)}' (${ch.toString(16)})", pos)

class UnexpectedToken(token: Token, pos: Position? = null) : ParseException("Unexpected token: ${token.formatted()}", pos)
class IncompatibleTypeParseException(message: String, pos: Position? = null) : ParseException(message, pos)
class IllegalNumberFormat(message: String, pos: Position? = null) : ParseException(message, pos)
class IllegalContextForFunction(pos: Position? = null) : ParseException("Illegal function call", pos)
class OperatorAxisNotSupported(pos: Position) : ParseException("Operator does not support axis argument", pos)
class SyntaxRuleMismatch(expectedSymbol: Symbol, foundSymbol: Symbol, pos: Position? = null) :
        ParseException("In custom syntax rule: Expected: ${expectedSymbol.symbolName}. Found: ${foundSymbol.symbolName}", pos)

class BitwiseNotSupported(pos: Position? = null) : ParseException("Function does not support bitwise operations", pos)
class ParallelNotSupported(pos: Position? = null) : ParseException("Function does not support parallel", pos)
class SyntaxSubRuleNotFound(name: Symbol, pos: Position? = null) : ParseException("Syntax sub rule does not exist. Name: ${name}", pos)
class IllegalDeclaration(message: String, pos: Position? = null) : ParseException("Illegal declaration: ${message}", pos)

class InvalidFunctionRedefinition(name: Symbol, pos: Position? = null) :
        ParseException("Function cannot be redefined: ${name.nameWithNamespace}", pos)

class ReturnValue(val value: APLValue, val returnEnvironment: Environment, pos: Position? = null) : APLEvalException("Target stack frame is not available", pos)

@OptIn(ExperimentalContracts::class)
inline fun unless(cond: Boolean, fn: () -> Unit) {
    contract { callsInPlace(fn, InvocationKind.AT_MOST_ONCE) }
    if (!cond) {
        fn()
    }
}

fun Long.plusMod(divisor: Long): Long {
    val v = this % divisor
    return if (v < 0) divisor + v else v
}

class Arrays {
    companion object {
        fun <T> equals(a: Array<T>, b: Array<T>): Boolean {
            if (a === b) {
                return true
            }
            val aLength = a.size
            val bLength = b.size
            unless(aLength == bLength) {
                return false
            }

            for (i in 0 until aLength) {
                if (a[i] != b[i]) {
                    return false
                }
            }
            return true
        }

        fun equals(a: IntArray, b: IntArray): Boolean {
            if (a === b) {
                return true
            }

            if (a.size != b.size) {
                return false
            }

            for (i in a.indices) {
                if (a[i] != b[i]) {
                    return false
                }
            }

            return true
        }

        fun compare(a: IntArray, b: IntArray): Int {
            var i = 0
            while (i < a.size && i < b.size) {
                val aVal = a[i]
                val bVal = b[i]
                when {
                    aVal < bVal -> return -1
                    aVal > bVal -> return 1
                }
                i++
            }
            return when {
                i < a.size -> 1
                i < b.size -> -1
                else -> 0
            }
        }

        fun toString(values: Array<*>): String {
            val buf = StringBuilder()
            buf.append("[")
            buf.append(values.asSequence().joinToString(", "))
            buf.append("]")
            return buf.toString()
        }
    }
}

fun <T> List<T>.rest(): List<T> {
    if (this.isEmpty()) {
        throw IllegalStateException("Cannot take the rest of an empty list")
    }
    return this.subList(1, this.size)
}

@OptIn(ExperimentalContracts::class)
inline fun assertx(condition: Boolean, message: () -> String = { "Assertion error" }) {
    contract { returns() implies condition }
    if (!condition) {
        throw AssertionError(message())
    }
}

fun ensureValidAxis(axis: Int, dimensions: Dimensions, pos: Position? = null) {
    if (axis < 0 || axis >= dimensions.size) {
        throwAPLException(IllegalAxisException(axis, dimensions, pos))
    }
}

inline fun <T, R : Comparable<R>> List<T>.maxValueBy(fn: (T) -> R): R {
    if (this.isEmpty()) {
        throw RuntimeException("call to maxValueBy on empty list")
    }
    var currMax: R? = null
    this.forEach { e ->
        val res = fn(e)
        if (currMax == null || res > currMax!!) {
            currMax = res
        }
    }
    return currMax!!
}

inline fun <T, R> List<T>.reduceWithInitial(initial: R, fn: (R, T) -> R): R {
    var curr = initial
    for (element in this) {
        curr = fn(curr, element)
    }
    return curr
}

inline fun IntArray.reduceWithInitial(initial: Int, fn: (Int, Int) -> Int): Int {
    var curr = initial
    for (element in this) {
        curr = fn(curr, element)
    }
    return curr
}

fun checkAxisPositionIsInRange(posAlongAxis: Int, dimensions: Dimensions, axis: Int, pos: Position?) {
    if (posAlongAxis < 0 || posAlongAxis >= dimensions[axis]) {
        throwAPLException(
            APLIndexOutOfBoundsException(
                "Position ${posAlongAxis} does not fit in dimensions ${Arrays.toString(dimensions.dimensions.toTypedArray())} axis ${axis}",
                pos))
    }
}

sealed class Either<out A, out B> {
    class Left<A>(val value: A) : Either<A, Nothing>()
    class Right<B>(val value: B) : Either<Nothing, B>()
}
