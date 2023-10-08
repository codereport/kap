package array.builtins

import array.*
import array.OptimisationFlags.Companion.OPTIMISATION_FLAG_2ARG_LONG_LONG
import array.complex.Complex
import com.dhsdevelopments.mpbignum.BigInt
import com.dhsdevelopments.mpbignum.LongExpressionOverflow
import com.dhsdevelopments.mpbignum.Rational
import com.dhsdevelopments.mpbignum.compareTo

class EqualsAPLFunction : APLFunctionDescriptor {
    class EqualsAPLFunctionImpl(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return if ((a is APLChar && b !is APLChar) || a !is APLChar && b is APLChar) {
                makeBoolean(false)
            } else {
                numericRelationOperation(
                    pos,
                    a,
                    b,
                    { x, y -> makeBoolean(x == y) },
                    { x, y -> makeBoolean(x == y) },
                    { x, y -> makeBoolean(x == y) },
                    { x, y -> makeBoolean(x == y) },
                    { x, y -> if (x.compareEquals(y)) APLLONG_1 else APLLONG_0 },
                    fnBigint = { x, y -> makeBoolean(x == y) },
                    fnRational = { x, y -> makeBoolean(x == y) })
            }
        }

        override fun identityValue() = APLLONG_1
        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG)
        override fun combine2ArgLongToLong(a: Long, b: Long) = if (a == b) 1L else 0L

        override val name2Arg get() = "equals"
    }

    override fun make(instantiation: FunctionInstantiation) = EqualsAPLFunctionImpl(instantiation)
}

class NotEqualsAPLFunction : APLFunctionDescriptor {
    class NotEqualsAPLFunctionImpl(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return if ((a is APLChar && b !is APLChar) || a !is APLChar && b is APLChar) {
                makeBoolean(true)
            } else {
                numericRelationOperation(
                    pos,
                    a,
                    b,
                    { x, y -> makeBoolean(x != y) },
                    { x, y -> makeBoolean(x != y) },
                    { x, y -> makeBoolean(x != y) },
                    { x, y -> makeBoolean(x != y) },
                    { x, y -> if (x.compareEquals(y)) APLLONG_0 else APLLONG_1 },
                    fnBigint = { x, y -> makeBoolean(x != y) },
                    fnRational = { x, y -> makeBoolean(x != y) })
            }
        }

        override fun identityValue() = APLLONG_0
        override fun deriveBitwise() = BitwiseXorFunction()

        override val name2Arg get() = "not equals"
    }

    override fun make(instantiation: FunctionInstantiation) = NotEqualsAPLFunctionImpl(instantiation)
}

class LessThanAPLFunction : APLFunctionDescriptor {
    class LessThanAPLFunctionImpl(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> makeBoolean(x < y) },
                { x, y -> makeBoolean(x < y) },
                { x, y -> makeBoolean(if (x.real == y.real) x.imaginary < y.imaginary else x.real < y.real) },
                { x, y -> makeBoolean(x < y) },
                fnBigint = { x, y -> makeBoolean(x < y) },
                fnRational = { x, y -> makeBoolean(x < y) })
        }

        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            if (axis != null) {
                throwAPLException(AxisNotSupported(pos))
            }
            val aDimensions = a.dimensions
            val dimensionsArray = IntArray(aDimensions.size + 1) { i ->
                if (i == 0) {
                    1
                } else {
                    aDimensions[i - 1]
                }
            }
            return ResizedArrayImpls.makeResizedArray(Dimensions(dimensionsArray), a)
        }

        override fun identityValue() = APLLONG_0

        override val name1Arg get() = "promote"
        override val name2Arg get() = "less than"
    }

    override fun make(instantiation: FunctionInstantiation) = LessThanAPLFunctionImpl(instantiation)
}

class GreaterThanAPLFunction : APLFunctionDescriptor {
    class GreaterThanAPLFunctionImpl(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> makeBoolean(x > y) },
                { x, y -> makeBoolean(x > y) },
                { x, y -> makeBoolean(if (x.real == y.real) x.imaginary > y.imaginary else x.real > y.real) },
                { x, y -> makeBoolean(x > y) },
                fnBigint = { x, y -> makeBoolean(x > y) },
                fnRational = { x, y -> makeBoolean(x > y) })
        }

        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            if (axis != null) {
                throwAPLException(AxisNotSupported(pos))
            }
            val aDimensions = a.dimensions
            return if (aDimensions.size <= 1) {
                a
            } else {
                val dimensionsArray = IntArray(aDimensions.size - 1) { i ->
                    if (i == 0) {
                        aDimensions[0] * aDimensions[1]
                    } else {
                        aDimensions[i + 1]
                    }
                }
                ResizedArrayImpls.makeResizedArray(Dimensions(dimensionsArray), a)
            }
        }

        override fun identityValue() = APLLONG_0

        override val name1Arg get() = "demote"
        override val name2Arg get() = "greater than"
    }

    override fun make(instantiation: FunctionInstantiation) = GreaterThanAPLFunctionImpl(instantiation)
}

class LessThanEqualAPLFunction : APLFunctionDescriptor {
    class LessThanEqualAPLFunctionImpl(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> makeBoolean(x <= y) },
                { x, y -> makeBoolean(x <= y) },
                { x, y -> makeBoolean(if (x.real == y.real) x.imaginary <= y.imaginary else x.real < y.real) },
                { x, y -> makeBoolean(x <= y) },
                fnBigint = { x, y -> makeBoolean(x <= y) },
                fnRational = { x, y -> makeBoolean(x <= y) })
        }

        override fun identityValue() = APLLONG_1

        override val name2Arg get() = "less than or equals"
    }

    override fun make(instantiation: FunctionInstantiation) = LessThanEqualAPLFunctionImpl(instantiation)
}

class GreaterThanEqualAPLFunction : APLFunctionDescriptor {
    class GreaterThanEqualAPLFunctionImpl(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> makeBoolean(x >= y) },
                { x, y -> makeBoolean(x >= y) },
                { x, y -> makeBoolean(if (x.real == y.real) x.imaginary >= y.imaginary else x.real > y.real) },
                { x, y -> makeBoolean(x >= y) },
                fnBigint = { x, y -> makeBoolean(x >= y) },
                fnRational = { x, y -> makeBoolean(x >= y) })
        }

        override fun identityValue() = APLLONG_1

        override val name2Arg get() = "greater than or equals"
    }

    override fun make(instantiation: FunctionInstantiation) = GreaterThanEqualAPLFunctionImpl(instantiation)
}

fun makeBoolean(value: Boolean): APLValue {
    return if (value) APLLONG_1 else APLLONG_0
}

fun throwIncompatibleArg(pos: Position?): Nothing {
    throwAPLException(APLIncompatibleDomainsException("Incompatible argument types", pos))
}

inline fun numericRelationOperation(
    pos: Position,
    a: APLSingleValue,
    b: APLSingleValue,
    fnLong: (Long, Long) -> APLValue,
    fnDouble: (Double, Double) -> APLValue,
    fnComplex: (Complex, Complex) -> APLValue,
    fnChar: ((Int, Int) -> APLValue) = { _, _ -> throwIncompatibleArg(pos) },
    fnOther: ((aOther: APLValue, bOther: APLValue) -> APLValue) = { _, _ -> throwIncompatibleArg(pos) },
    fnBigint: ((aBigint: BigInt, bBigint: BigInt) -> APLValue) = { _, _ -> throwIncompatibleArg(pos) },
    fnRational: ((aRational: Rational, bRational: Rational) -> APLValue) = { _, _ -> throwIncompatibleArg(pos) }
): APLValue {
    numericRelationOperation2(
        pos,
        a,
        b,
        { x, y ->
            return try {
                fnLong(x, y)
            } catch (e: LongExpressionOverflow) {
                e.result.makeAPLNumber()
            }
        },
        { x, y -> return fnDouble(x, y) },
        { x, y -> return fnComplex(x, y) },
        { x, y -> return fnChar(x, y) },
        { x, y -> return fnOther(x, y) },
        { x, y -> return fnBigint(x, y) },
        { x, y -> return fnRational(x, y) })
}

inline fun numericRelationOperation2(
    pos: Position,
    a: APLSingleValue,
    b: APLSingleValue,
    fnLong: (Long, Long) -> Nothing,
    fnDouble: (Double, Double) -> Nothing,
    fnComplex: (Complex, Complex) -> Nothing,
    fnChar: ((Int, Int) -> Nothing) = { _, _ -> throwIncompatibleArg(pos) },
    fnOther: ((aOther: APLValue, bOther: APLValue) -> Nothing) = { _, _ -> throwIncompatibleArg(pos) },
    fnBigint: ((aBigint: BigInt, bBigint: BigInt) -> Nothing) = { _, _ -> throwIncompatibleArg(pos) },
    fnRational: ((aRational: Rational, bRational: Rational) -> Nothing) = { _, _ -> throwIncompatibleArg(pos) }
): Nothing {
    when {
        a is APLNumber && b is APLNumber -> {
            when {
                a is APLComplex || b is APLComplex -> fnComplex(a.asComplex(), b.asComplex())
                a is APLDouble || b is APLDouble -> fnDouble(a.asDouble(), b.asDouble())
                a is APLRational || b is APLRational -> fnRational(a.asRational(), b.asRational())
                a is APLBigInt || b is APLBigInt -> fnBigint(a.asBigInt(), b.asBigInt())
                a is APLLong && b is APLLong -> fnLong(a.asLong(pos), b.asLong(pos))
                else -> error("Unexpected types. a: ${a::class.simpleName}, b: ${b::class.simpleName}")
            }
        }
        a is APLChar && b is APLChar -> {
            fnChar(a.value, b.value)
        }
        else -> fnOther(a, b)
    }
}

inline fun singleArgNumericRelationOperation(
    pos: Position,
    a: APLSingleValue,
    fnLong: (Long) -> APLValue,
    fnDouble: (Double) -> APLValue,
    fnComplex: (Complex) -> APLValue,
    fnChar: ((Int) -> APLValue) = { _ -> throwIncompatibleArg(pos) },
    fnBigInt: ((BigInt) -> APLValue) = { _ -> throwIncompatibleArg(pos) },
    fnRational: ((Rational) -> APLValue) = { _ -> throwIncompatibleArg(pos) }
): APLValue {
    return when (a) {
        is APLLong -> fnLong(a.asLong(pos))
        is APLDouble -> fnDouble(a.asDouble(pos))
        is APLComplex -> fnComplex(a.asComplex())
        is APLChar -> fnChar(a.value)
        is APLBigInt -> fnBigInt(a.value)
        is APLRational -> fnRational(a.value)
        else -> throwIncompatibleArg(pos)
    }
}

class CompareObjectsFunction : APLFunctionDescriptor {
    class CompareObjectsFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return a.compare(b).makeAPLNumber()
        }
    }

    override fun make(instantiation: FunctionInstantiation) = CompareObjectsFunctionImpl(instantiation)
}
