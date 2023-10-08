@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package array.builtins

import array.*
import com.dhsdevelopments.mpbignum.*

abstract class BitwiseCombineAPLFunction(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
    override val optimisationFlags get() = OptimisationFlags(OptimisationFlags.OPTIMISATION_FLAG_1ARG_LONG or OptimisationFlags.OPTIMISATION_FLAG_2ARG_LONG_LONG)

    private fun throwTypeError(): Nothing {
        throwAPLException(APLIncompatibleDomainsException("Bitwise calls can only be performed on integers", pos))
    }

    override fun combine1Arg(a: APLSingleValue): APLValue = when (a) {
        is APLLong -> bitwiseCombine1ArgLong(a.value).makeAPLNumber()
        is APLBigInt -> bitwiseCombine1ArgBigint(a.value).makeAPLNumber()
        is APLRational -> a.value.let { v ->
            if (v.denominator == BigIntConstants.ONE) {
                bitwiseCombine1ArgBigint(v.numerator).makeAPLNumber()
            } else {
                throwTypeError()
            }
        }
        else -> throwTypeError()
    }

    private fun tryConvertToBigInt(v: APLValue): BigInt = when (v) {
        is APLLong -> v.value.toBigInt()
        is APLBigInt -> v.value
        is APLRational -> v.value.let { rat ->
            if (rat.denominator == BigIntConstants.ONE) {
                rat.numerator
            } else {
                throwTypeError()
            }
        }
        else -> throwTypeError()
    }

    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue = when {
        a is APLLong && b is APLLong -> bitwiseCombine2ArgLong(a.value, b.value).makeAPLNumber()
        else -> bitwiseCombine2ArgBigint(tryConvertToBigInt(a), tryConvertToBigInt(b)).makeAPLNumberWithReduction()
    }

    override fun combine1ArgLongToLong(a: Long) = bitwiseCombine1ArgLong(a)
    override fun combine2ArgLongToLong(a: Long, b: Long) = bitwiseCombine2ArgLong(a, b)

    open fun bitwiseCombine1ArgLong(a: Long): Long = throwAPLException(Unimplemented1ArgException(pos))
    open fun bitwiseCombine1ArgBigint(a: BigInt): BigInt = throwAPLException(Unimplemented1ArgException(pos))
    open fun bitwiseCombine2ArgLong(a: Long, b: Long): Long = throwAPLException(Unimplemented2ArgException(pos))
    open fun bitwiseCombine2ArgBigint(a: BigInt, b: BigInt): BigInt = throwAPLException(Unimplemented2ArgException(pos))
}

class BitwiseOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: FunctionInstantiation) = fn.deriveBitwise() ?: throw BitwiseNotSupported(fn.pos)
}

class BitwiseAndFunction : APLFunctionDescriptor {
    class BitwiseAndFunctionImpl(pos: FunctionInstantiation) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2ArgLong(a: Long, b: Long) = a and b
        override fun bitwiseCombine2ArgBigint(a: BigInt, b: BigInt) = a and b
        override val name2Arg get() = "bitwise and"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseAndFunctionImpl(instantiation)
}

class BitwiseOrFunction : APLFunctionDescriptor {
    class BitwiseOrFunctionImpl(pos: FunctionInstantiation) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2ArgLong(a: Long, b: Long) = a or b
        override fun bitwiseCombine2ArgBigint(a: BigInt, b: BigInt) = a or b
        override val name2Arg get() = "bitwise or"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseOrFunctionImpl(instantiation)
}

class BitwiseXorFunction : APLFunctionDescriptor {
    class BitwiseXorFunctionImpl(pos: FunctionInstantiation) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2ArgLong(a: Long, b: Long) = a xor b
        override fun bitwiseCombine2ArgBigint(a: BigInt, b: BigInt) = a xor b
        override val name2Arg get() = "bitwise xor"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseXorFunctionImpl(instantiation)
}

class BitwiseNotFunction : APLFunctionDescriptor {
    class BitwiseNotFunctionImpl(pos: FunctionInstantiation) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine1ArgLong(a: Long) = a.inv()
        override fun bitwiseCombine1ArgBigint(a: BigInt) = a.inv()
        override val name2Arg get() = "bitwise not"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseNotFunctionImpl(instantiation)
}

class BitwiseNandFunction : APLFunctionDescriptor {
    class BitwiseNandFunctionImpl(pos: FunctionInstantiation) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2ArgLong(a: Long, b: Long) = (a and b).inv()
        override fun bitwiseCombine2ArgBigint(a: BigInt, b: BigInt) = (a and b).inv()
        override val name2Arg get() = "bitwise nand"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseNandFunctionImpl(instantiation)
}

class BitwiseNorFunction : APLFunctionDescriptor {
    class BitwiseNorFunctionImpl(pos: FunctionInstantiation) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2ArgLong(a: Long, b: Long) = (a or b).inv()
        override fun bitwiseCombine2ArgBigint(a: BigInt, b: BigInt) = (a or b).inv()
        override val name2Arg get() = "bitwise nor"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseNorFunctionImpl(instantiation)
}

// TODO: Need to assign this to the appropriate parent function
class BitwiseCountBitsFunction : APLFunctionDescriptor {
    class BitwiseCountBitsFunctionImpl(pos: FunctionInstantiation) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine1ArgLong(a: Long): Long {
            var total = 0L
            repeat(64) { i ->
                if (a and (1L shl i) > 0) {
                    total++
                }
            }
            return total
        }

        override val name1Arg get() = "bitwise count bits"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseCountBitsFunctionImpl(instantiation)
}

class BitwiseShiftFunction : APLFunctionDescriptor {
    class BitwiseShiftFunctionImpl(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
        override fun combine2ArgLongToLong(a: Long, b: Long): Long {
            return opLong(a, b)
        }

        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> opLong(x, y).makeAPLNumber() },
                { x, y -> opBigInt(BigInt.of(x), BigInt.of(y)).makeAPLNumber() },
                { x, y -> throwAPLException(APLIncompatibleDomainsException("Complex numbers not supported", pos)) },
                fnBigint = { x, y -> opBigInt(x, y).makeAPLNumberWithReduction() })
        }

        private fun opLong(a: Long, b: Long): Long {
            if (a < Int.MIN_VALUE || a > Int.MAX_VALUE) {
                throwAPLException(APLIncompatibleDomainsException("Shift count out of range: ${a}"))
            }
            val result = BigInt.of(b).shl(a.toInt())
            if (result.rangeInLong()) {
                return result.toLong()
            } else {
                throw LongExpressionOverflow(result)
            }
        }

        private fun opBigInt(a: BigInt, b: BigInt): BigInt {
            if (!a.rangeInInt()) {
                throwAPLException(APLIncompatibleDomainsException("Shift count out of range: ${a}"))
            }
            return b.shl(a.toLong())
        }

        override val name2Arg get() = "bitwise shift"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseShiftFunctionImpl(instantiation)
}
