package array.builtins

import array.*
import array.OptimisationFlags.Companion.OPTIMISATION_FLAG_2ARG_LONG_LONG
import array.complex.Complex
import com.dhsdevelopments.mpbignum.BigInt
import com.dhsdevelopments.mpbignum.LongExpressionOverflow
import com.dhsdevelopments.mpbignum.Rational

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
                    { x, y -> if (x.compareEquals(y)) APLLONG_1 else APLLONG_0 })
            }
        }

        override fun identityValue() = APLLONG_1
        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG)
        override fun combine2ArgLong(a: Long, b: Long) = if (a == b) 1L else 0L

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
                    { x, y -> if (x.compareEquals(y)) APLLONG_0 else APLLONG_1 })
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
                { x, y -> makeBoolean(x < y) })
        }

        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            if (axis != null) {
                throw AxisNotSupported(pos)
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
                { x, y -> makeBoolean(x > y) })
        }

        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            if (axis != null) {
                throw AxisNotSupported(pos)
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
                { x, y -> makeBoolean(x <= y) })
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
                { x, y -> makeBoolean(x >= y) })
        }

        override fun identityValue() = APLLONG_1

        override val name2Arg get() = "greater than or equals"
    }

    override fun make(instantiation: FunctionInstantiation) = GreaterThanEqualAPLFunctionImpl(instantiation)
}

fun makeBoolean(value: Boolean): APLValue {
    return if (value) APLLONG_1 else APLLONG_0
}

inline fun numericRelationOperation(
    pos: Position,
    a: APLSingleValue,
    b: APLSingleValue,
    fnLong: (Long, Long) -> APLValue,
    fnDouble: (Double, Double) -> APLValue,
    fnComplex: (Complex, Complex) -> APLValue,
    fnChar: ((Int, Int) -> APLValue) = { _, _ ->
        throwAPLException(IncompatibleTypeException("Incompatible argument types", pos))
    },
    fnOther: ((aOther: APLValue, bOther: APLValue) -> APLValue) = { _, _ ->
        throwAPLException(IncompatibleTypeException("Incompatible argument types", pos))
    },
    fnBigint: ((aBigint: BigInt, bBigint: BigInt) -> APLValue) = { _, _ ->
        throwAPLException(IncompatibleTypeException("Bigint is not supported", pos))
    },
    fnRational: ((aRational: Rational, bRational: Rational) -> APLValue) = { _, _ ->
        throwAPLException(IncompatibleTypeException("Rational is not supported", pos))
    }
): APLValue {
    return when {
        a is APLNumber && b is APLNumber -> {
            when {
                a is APLComplex || b is APLComplex -> fnComplex(a.asComplex(), b.asComplex())
                a is APLDouble || b is APLDouble -> fnDouble(a.asDouble(), b.asDouble())
                a is APLRational || b is APLRational -> fnRational(a.asRational(), b.asRational())
                a is APLBigInt || b is APLBigInt -> fnBigint(a.asBigInt(), b.asBigInt())
                else -> try {
                    fnLong(a.asLong(pos), b.asLong(pos))
                } catch (e: LongExpressionOverflow) {
                    APLBigInt(e.result)
                }
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
    fnChar: ((Int) -> APLValue) = { _ -> throwAPLException(IncompatibleTypeException("Incompatible argument types", pos)) },
    fnBigInt: ((BigInt) -> APLValue) = { _ -> throwAPLException(IncompatibleTypeException("Incompatible argument types", pos)) },
    fnRational: ((Rational) -> APLValue) = { _ -> throwAPLException(IncompatibleTypeException("Incompatible argument types", pos)) }
): APLValue {
    return when (a) {
        is APLLong -> fnLong(a.asLong(pos))
        is APLDouble -> fnDouble(a.asDouble(pos))
        is APLComplex -> fnComplex(a.asComplex())
        is APLChar -> fnChar(a.value)
        is APLBigInt -> fnBigInt(a.value)
        is APLRational -> fnRational(a.value)
        else -> throwAPLException(IncompatibleTypeException("Incompatible argument types", pos))
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
