package array.builtins

import array.*
import array.OptimisationFlags.Companion.OPTIMISATION_FLAG_2ARG_LONG_LONG
import array.complex.Complex

class EqualsAPLFunction : APLFunctionDescriptor {
    class EqualsAPLFunctionImpl(pos: Position) : MathCombineAPLFunction(pos) {
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
    }

    override fun make(pos: Position) = EqualsAPLFunctionImpl(pos.withName("equals"))
}

class NotEqualsAPLFunction : APLFunctionDescriptor {
    class NotEqualsAPLFunctionImpl(pos: Position) : MathCombineAPLFunction(pos) {
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
    }

    override fun make(pos: Position) = NotEqualsAPLFunctionImpl(pos.withName("not equals"))
}

class LessThanAPLFunction : APLFunctionDescriptor {
    class LessThanAPLFunctionImpl(pos: Position) : MathCombineAPLFunction(pos) {
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

        override fun identityValue() = APLLONG_0
    }

    override fun make(pos: Position) = LessThanAPLFunctionImpl(pos.withName("less than"))
}

class GreaterThanAPLFunction : APLFunctionDescriptor {
    class GreaterThanAPLFunctionImpl(pos: Position) : MathCombineAPLFunction(pos) {
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

        override fun identityValue() = APLLONG_0
    }

    override fun make(pos: Position) = GreaterThanAPLFunctionImpl(pos.withName("greater than"))
}

class LessThanEqualAPLFunction : APLFunctionDescriptor {
    class LessThanEqualAPLFunctionImpl(pos: Position) : MathCombineAPLFunction(pos) {
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
    }

    override fun make(pos: Position) = LessThanEqualAPLFunctionImpl(pos.withName("less than or equals"))
}

class GreaterThanEqualAPLFunction : APLFunctionDescriptor {
    class GreaterThanEqualAPLFunctionImpl(pos: Position) : MathCombineAPLFunction(pos) {
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
    }

    override fun make(pos: Position) = GreaterThanEqualAPLFunctionImpl(pos.withName("greater than or equals"))
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
    }
): APLValue {
    return when {
        a is APLNumber && b is APLNumber -> {
            when {
                a is APLComplex || b is APLComplex -> fnComplex(a.asComplex(), b.asComplex())
                a is APLDouble || b is APLDouble -> fnDouble(a.asDouble(), b.asDouble())
                else -> fnLong(a.asLong(), b.asLong())
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
    fnChar: ((Int) -> APLValue) = { _ -> throwAPLException(IncompatibleTypeException("Incompatible argument types", pos)) }
): APLValue {
    return when (a) {
        is APLLong -> fnLong(a.asLong())
        is APLDouble -> fnDouble(a.asDouble())
        is APLComplex -> fnComplex(a.asComplex())
        is APLChar -> fnChar(a.value)
        else -> throwAPLException(IncompatibleTypeException("Incompatible argument types", pos))
    }
}

class CompareObjectsFunction : APLFunctionDescriptor {
    class CompareObjectsFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return a.compare(b).makeAPLNumber()
        }
    }

    override fun make(pos: Position) = CompareObjectsFunctionImpl(pos)
}
