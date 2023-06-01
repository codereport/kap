package array.builtins

import array.*
import array.OptimisationFlags.Companion.OPTIMISATION_FLAG_1ARG_DOUBLE
import array.OptimisationFlags.Companion.OPTIMISATION_FLAG_1ARG_LONG
import array.OptimisationFlags.Companion.OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE
import array.OptimisationFlags.Companion.OPTIMISATION_FLAG_2ARG_LONG_LONG
import array.complex.*
import com.dhsdevelopments.mpbignum.*
import kotlin.math.*

interface CellSumFunction1Arg {
    fun combine(a: APLSingleValue): APLValue
}

class ArraySum1Arg(
    private val fn: CellSumFunction1Arg,
    private val a: APLValue
) : DeferredResultArray() {
    override val dimensions get() = a.dimensions
    override val size get() = a.size
    override fun valueAt(p: Int): APLValue {
        if (a is APLSingleValue) {
            return fn.combine(a)
        }
        val v = a.valueAt(p)
        return if (v is APLSingleValue) {
            fn.combine(v)
        } else {
            ArraySum1Arg(fn, v)
        }
    }
}

private fun throwMismatchedScalarFunctionArgs(pos: Position): Nothing {
    throwAPLException(
        InvalidDimensionsException("Arguments must be of the same dimension, or one of the arguments must be a scalar", pos))
}

class GenericArraySum2Args(
    val fn: MathCombineAPLFunction,
    val a0: APLValue,
    val b0: APLValue,
    val pos: Position
) : DeferredResultArray() {
    private val aRank = a0.rank
    private val bRank = b0.rank

    override val dimensions = if (aRank == 0) b0.dimensions else a0.dimensions
    override val rank = dimensions.size

    init {
        unless(aRank == 0 || bRank == 0 || a0.dimensions.compareEquals(b0.dimensions)) {
            throwMismatchedScalarFunctionArgs(pos)
        }
    }

    override fun valueAt(p: Int): APLValue {
        val a1 = when {
            a0.isScalar() -> a0.disclose().unwrapDeferredValue()
            else -> a0.valueAt(p).unwrapDeferredValue()
        }
        val b1 = when {
            b0.isScalar() -> b0.disclose().unwrapDeferredValue()
            else -> b0.valueAt(p).unwrapDeferredValue()
        }
        return if (a1 is APLSingleValue && b1 is APLSingleValue) {
            fn.combine2Arg(a1, b1)
        } else {
            fn.makeCellSumFunction2Args(a1, b1, pos)
        }
    }
}

class LongArraySum2Args(
    val fn: MathCombineAPLFunction,
    val a0: APLValue,
    val b0: APLValue,
    val pos: Position
) : APLArray() {
    override val dimensions: Dimensions
    override val specialisedType get() = ArrayMemberType.LONG

    init {
        unless(a0.dimensions.compareEquals(b0.dimensions)) {
            throwMismatchedScalarFunctionArgs(pos)
        }
        dimensions = a0.dimensions
    }

    override fun valueAt(p: Int): APLValue {
        return try {
            valueAtLong(p, pos).makeAPLNumber()
        } catch (e: LongExpressionOverflow) {
            APLBigInt(e.result)
        }
    }

    override fun valueAtLong(p: Int, pos: Position?): Long {
        val a1 = try {
            a0.valueAtLong(p, pos)
        } catch (e: LongExpressionOverflow) {
            val a2 = APLBigInt(e.result)
            val b2 = b0.valueAt(p).ensureNumber(pos)
            val res = fn.combine2Arg(a2, b2).ensureNumber(pos)
            throw LongExpressionOverflow(res.asBigInt())
        }
        val b1 = try {
            b0.valueAtLong(p, pos)
        } catch (e: LongExpressionOverflow) {
            val b2 = APLBigInt(e.result)
            val res = fn.combine2Arg(a1.makeAPLNumber(), b2).ensureNumber(pos)
            throw LongExpressionOverflow(res.asBigInt())
        }
        return fn.combine2ArgLong(a1, b1)
    }
}

class DoubleArraySum2Args(
    val fn: MathCombineAPLFunction,
    val a0: APLValue,
    val b0: APLValue,
    val pos: Position
) : APLArray() {
    override val dimensions: Dimensions
    override val specialisedType get() = ArrayMemberType.DOUBLE

    init {
        unless(a0.dimensions.compareEquals(b0.dimensions)) {
            throwMismatchedScalarFunctionArgs(pos)
        }
        dimensions = a0.dimensions
    }

    override fun valueAt(p: Int) = valueAtDouble(p, pos).makeAPLNumber()

    override fun valueAtDouble(p: Int, pos: Position?): Double {
        return fn.combine2ArgDouble(a0.valueAtDouble(p, pos), b0.valueAtDouble(p, pos))
    }
}

class LongArraySum2ArgsLeftScalar(
    val fn: MathCombineAPLFunction,
    val a0: Long,
    val b0: APLValue,
    val pos: Position
) : APLArray() {
    override val dimensions = b0.dimensions
    override val specialisedType get() = ArrayMemberType.LONG

    override fun valueAt(p: Int) = valueAtLong(p, pos).makeAPLNumber()

    override fun valueAtLong(p: Int, pos: Position?): Long {
        val b1 = try {
            b0.valueAtLong(p, pos)
        } catch (e: LongExpressionOverflow) {
            val b2 = APLBigInt(e.result)
            val res = fn.combine2Arg(a0.makeAPLNumber(), b2).ensureNumber()
            throw LongExpressionOverflow(res.asBigInt())
        }
        return fn.combine2ArgLong(a0, b1)
    }
}

class LongArraySum2ArgsRightScalar(
    val fn: MathCombineAPLFunction,
    val a0: APLValue,
    val b0: Long,
    val pos: Position
) : APLArray() {
    override val dimensions = a0.dimensions
    override val specialisedType get() = ArrayMemberType.LONG

    override fun valueAt(p: Int) = try {
        valueAtLong(p, pos).makeAPLNumber()
    } catch (e: LongExpressionOverflow) {
        e.result.makeAPLNumber()
    }

    override fun valueAtLong(p: Int, pos: Position?): Long {
        val a1 = try {
            a0.valueAtLong(p, pos)
        } catch (e: LongExpressionOverflow) {
            val a2 = APLBigInt(e.result)
            val res = fn.combine2Arg(a2, b0.makeAPLNumber()).ensureNumber()
            throw LongExpressionOverflow(res.asBigInt())
        }
        return fn.combine2ArgLong(a1, b0)
    }
}

abstract class MathCombineAPLFunction(pos: FunctionInstantiation) : APLFunction(pos) {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        if (a is APLSingleValue) {
            return combine1Arg(a)
        }

        val fn = object : CellSumFunction1Arg {
            override fun combine(a: APLSingleValue): APLValue {
                return combine1Arg(a)
            }
        }
        return ArraySum1Arg(fn, a)
    }

    fun makeCellSumFunction2Args(a: APLValue, b: APLValue, pos: Position): APLValue {
        return when {
            a is APLSingleValue && b is APLSingleValue -> throw AssertionError("a and b cannot be singlevalue")
            a is APLSingleValue -> {
                when {
                    a is APLLong && b.specialisedType === ArrayMemberType.LONG && optimisationFlags.is2ALongLong ->
                        LongArraySum2ArgsLeftScalar(this, a.value, b, pos)

                    b.isScalar() -> EnclosedAPLValue.make(makeCellSumFunction2Args(a, b.valueAt(0), pos))
                    else ->
                        GenericArraySum2Args(this, a, b, pos)
                }
            }

            b is APLSingleValue -> {
                when {
                    b is APLLong && a.specialisedType === ArrayMemberType.LONG && optimisationFlags.is2ALongLong ->
                        LongArraySum2ArgsRightScalar(this, a, b.value, pos)

                    a.isScalar() -> EnclosedAPLValue.make(makeCellSumFunction2Args(a.valueAt(0), b, pos))
                    else ->
                        GenericArraySum2Args(this, a, b, pos)
                }
            }

            a.rank == 0 && b.rank == 0 -> EnclosedAPLValue.make(makeCellSumFunction2Args(a.valueAt(0), b.valueAt(0), pos))
            a.specialisedType === ArrayMemberType.LONG && b.specialisedType === ArrayMemberType.LONG && optimisationFlags.is2ALongLong ->
                LongArraySum2Args(this, a, b, pos)

            a.specialisedType === ArrayMemberType.DOUBLE && b.specialisedType === ArrayMemberType.DOUBLE && optimisationFlags.is2ADoubleDouble ->
                DoubleArraySum2Args(this, a, b, pos)

            else ->
                GenericArraySum2Args(this, a, b, pos)
        }
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        val a0 = a.unwrapDeferredValue()
        val b0 = b.unwrapDeferredValue()

        if (a0 is APLSingleValue && b0 is APLSingleValue) {
            return combine2Arg(a0, b0)
        }

        if (axis != null) {
            val aDimensions = a0.dimensions
            val bDimensions = b0.dimensions

            val axisInt = axis.ensureNumber(pos).asInt()

            fun computeTransformation(baseVal: APLValue, d1: Dimensions, d2: Dimensions): APLValue {
                ensureValidAxis(axisInt, d2, pos)
                if (d1[0] != d2[axisInt]) {
                    throwAPLException(
                        InvalidDimensionsException(
                            "Dimensions of A does not match dimensions of B across axis ${axisInt}",
                            pos))
                }
                val d = d2.remove(axisInt).insert(d2.size - 1, d2[axisInt])
                val transposeAxis = IntArray(d2.size) { i ->
                    when {
                        i == d2.size - 1 -> axisInt
                        i < axisInt -> i
                        else -> i + 1
                    }
                }
                return TransposedAPLValue(transposeAxis, ResizedArrayImpls.makeResizedArray(d, baseVal), pos)
            }

            // When an axis is given, one of the arguments must be rank 1, and its dimension must be equal to the
            // dimension of the other argument across the axis
            val (a1, b1) = when {
                aDimensions.size == 1 && bDimensions.size == 1 -> {
                    if (axisInt == 0) Pair(a0, b0) else throwAPLException(IllegalAxisException(axisInt, aDimensions, pos))
                }

                aDimensions.size == 1 -> Pair(computeTransformation(a0, aDimensions, bDimensions), b0)
                bDimensions.size == 1 -> Pair(a0, computeTransformation(b0, bDimensions, aDimensions))
                else -> throwAPLException(APLIllegalArgumentException("When specifying an axis, A or B has to be rank 1", pos))
            }

            return makeCellSumFunction2Args(a1, b1, pos)
        } else {
            return makeCellSumFunction2Args(a0, b0, pos)
        }
    }

    open fun combine1Arg(a: APLSingleValue): APLValue = throwAPLException(Unimplemented1ArgException(pos))
    open fun combine1ArgLong(a: Long): Long = throw IllegalStateException("Optimisation not implemented for: ${this::class.simpleName}")
    open fun combine1ArgDouble(a: Double): Double =
        throw IllegalStateException("Optimisation not implemented for: ${this::class.simpleName}")

    open fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue = throwAPLException(Unimplemented2ArgException(pos))
    open fun combine2ArgLong(a: Long, b: Long): Long =
        throw IllegalStateException("Optimisation not implemented for: ${this::class.simpleName}")

    open fun combine2ArgDouble(a: Double, b: Double): Double =
        throw IllegalStateException("Optimisation not implemented for: ${this::class.simpleName}")

    override fun eval2ArgLongLong(context: RuntimeContext, a: Long, b: Long, axis: APLValue?) = combine2ArgLong(a, b)
    override fun eval2ArgDoubleDouble(context: RuntimeContext, a: Double, b: Double, axis: APLValue?) = combine2ArgDouble(a, b)
}

abstract class MathNumericCombineAPLFunction(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
    override fun combine1Arg(a: APLSingleValue): APLValue = numberCombine1Arg(a.ensureNumber(pos))
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue =
        numberCombine2Arg(a.ensureNumber(pos), b.ensureNumber(pos))

    open fun numberCombine1Arg(a: APLNumber): APLValue = throwAPLException(Unimplemented1ArgException(pos))
    open fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue = throwAPLException(Unimplemented2ArgException(pos))
}

class AddAPLFunction : APLFunctionDescriptor {
    class AddAPLFunctionImpl(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
        override fun combine1Arg(a: APLSingleValue): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> x.makeAPLNumber() },
                { x -> x.makeAPLNumber() },
                { x -> Complex(x.real, -x.imaginary).makeAPLNumber() },
                fnBigInt = { x -> x.makeAPLNumber() },
                fnRational = { x -> x.makeAPLNumber() })
        }

        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> addExactWrapped(x, y).makeAPLNumber() },
                { x, y -> (x + y).makeAPLNumber() },
                { x, y -> (x + y).makeAPLNumber() },
                fnOther = { x, y ->
                    when {
                        x is APLChar && y is APLNumber -> APLChar.fromLong(x.value + y.asLong(pos), pos)
                        x is APLNumber && y is APLChar -> APLChar.fromLong(y.value + x.asLong(pos), pos)
                        else -> throwAPLException(IncompatibleTypeException("Incompatible argument types", pos))
                    }
                },
                fnBigint = { x, y -> (x + y).makeAPLNumber() },
                fnRational = { x, y -> (x + y).makeAPLNumber() })
        }

        override fun combine1ArgLong(a: Long) = a
        override fun combine1ArgDouble(a: Double) = a

        override fun combine2ArgLong(a: Long, b: Long) = addExactWrapped(a, b)
        override fun combine2ArgDouble(a: Double, b: Double) = a + b

        override fun identityValue() = APLLONG_0
        override fun deriveBitwise() = BitwiseXorFunction()

        override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?) = eval1Arg(context, a, axis)
        override fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue) =
            inversibleStructuralUnder1Arg(this, baseFn, context, a)

        override fun evalWithStructuralUnder2Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, b: APLValue): APLValue =
            inversibleStructuralUnder2Arg(this, baseFn, context, a, b)

        private val subFn by lazy { SubAPLFunction().make(pos) }
        override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return subFn.eval2Arg(context, b, a, axis)
        }

        override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return subFn.eval2Arg(context, a, b, null)
        }

        override val optimisationFlags
            get() = OptimisationFlags(
                OPTIMISATION_FLAG_1ARG_LONG or
                        OPTIMISATION_FLAG_1ARG_DOUBLE or
                        OPTIMISATION_FLAG_2ARG_LONG_LONG or
                        OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)

        override val name1Arg get() = "conjugate"
        override val name2Arg get() = "add"
    }

    override fun make(instantiation: FunctionInstantiation) = AddAPLFunctionImpl(instantiation)
}

class SubAPLFunction : APLFunctionDescriptor {
    class SubAPLFunctionImpl(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
        override fun combine1Arg(a: APLSingleValue): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> (-x).makeAPLNumber() },
                { x -> (-x).makeAPLNumber() },
                { x -> (-x).makeAPLNumber() },
                fnBigInt = { x -> (-x).makeAPLNumber() },
                fnRational = { x -> (-x).makeAPLNumber() })
        }

        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> (x - y).makeAPLNumber() },
                { x, y -> (x - y).makeAPLNumber() },
                { x, y -> (x - y).makeAPLNumber() },
                { x, y -> (x - y).makeAPLNumber() },
                { x, y ->
                    when {
                        x is APLChar && y is APLNumber -> APLChar.fromLong(x.value - y.asLong(pos), pos)
                        else -> throwAPLException(IncompatibleTypeException("Incompatible argument types", pos))
                    }
                },
                fnBigint = { x, y -> (x - y).makeAPLNumber() },
                fnRational = { x, y -> (x - y).makeAPLNumber() })
        }

        override fun combine1ArgLong(a: Long) = -a
        override fun combine1ArgDouble(a: Double) = -a
        override fun combine2ArgLong(a: Long, b: Long) = subExactWrapped(a, b)
        override fun combine2ArgDouble(a: Double, b: Double) = a - b

        override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?) =
            eval1Arg(context, a, axis)

        override fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue) =
            inversibleStructuralUnder1Arg(this, baseFn, context, a)

        override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?) =
            eval2Arg(context, a, b, axis)

        private val addFn by lazy { AddAPLFunction().make(pos) }
        override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?) =
            addFn.eval2Arg(context, a, b, axis)

        override fun evalWithStructuralUnder2Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, b: APLValue): APLValue =
            inversibleStructuralUnder2Arg(this, baseFn, context, a, b)

        override fun identityValue() = APLLONG_0
        override fun deriveBitwise() = BitwiseXorFunction()

        override val optimisationFlags
            get() = OptimisationFlags(
                OPTIMISATION_FLAG_1ARG_LONG or
                        OPTIMISATION_FLAG_1ARG_DOUBLE or
                        OPTIMISATION_FLAG_2ARG_LONG_LONG or
                        OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)

        override val name1Arg get() = "negate"
        override val name2Arg get() = "subtract"
    }

    override fun make(instantiation: FunctionInstantiation) = SubAPLFunctionImpl(instantiation)
}

class MulAPLFunction : APLFunctionDescriptor {
    class MulAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> x.sign.toLong().makeAPLNumber() },
                { x -> x.sign.toLong().makeAPLNumber() },
                { x -> x.signum().makeAPLNumber() },
                fnBigInt = { x -> x.signum().makeAPLNumber() },
                fnRational = { x -> x.signum().makeAPLNumber() })
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> mulExactWrapped(x, y).makeAPLNumber() },
                { x, y -> (x * y).makeAPLNumber() },
                { x, y -> (x * y).makeAPLNumber() },
                fnBigint = { x, y -> (x * y).makeAPLNumber() },
                fnRational = { x, y -> (x * y).makeAPLNumber() })
        }

        override fun identityValue() = APLLONG_1
        override fun deriveBitwise() = BitwiseAndFunction()

        override fun combine1ArgLong(a: Long) = a.sign.toLong()
        override fun combine1ArgDouble(a: Double) = a.sign

        override fun combine2ArgLong(a: Long, b: Long) = mulExactWrapped(a, b)
        override fun combine2ArgDouble(a: Double, b: Double) = a * b

        private val divFn by lazy { DivAPLFunction().make(pos) }
        override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return divFn.eval2Arg(context, b, a, axis)
        }

        override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return divFn.eval2Arg(context, a, b, axis)
        }

        override fun evalWithStructuralUnder2Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, b: APLValue): APLValue =
            inversibleStructuralUnder2Arg(this, baseFn, context, a, b)

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_1ARG_LONG or OPTIMISATION_FLAG_1ARG_DOUBLE or OPTIMISATION_FLAG_2ARG_LONG_LONG or OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)

        override val name1Arg get() = "magnitude"
        override val name2Arg get() = "multiply"
    }

    override fun make(instantiation: FunctionInstantiation) = MulAPLFunctionImpl(instantiation)
}

class DivAPLFunction : APLFunctionDescriptor {
    class DivAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> if (x == 0L) APLLONG_0 else Rational.make(BigIntConstants.ONE, x.toBigInt()).makeAPLNumber() },
                { x -> if (x == 0.0) APLLONG_0 else (1.0 / x).makeAPLNumber() },
                { x -> if (x == Complex.ZERO) APLLONG_0 else x.reciprocal().makeAPLNumber() },
                fnBigInt = { x -> Rational.make(BigIntConstants.ONE, x).makeAPLNumber() },
                fnRational = { x -> Rational.make(x.denominator, x.numerator).makeAPLNumber() })
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y ->
                    when {
                        y == 0L -> APLLONG_0
                        x % y == 0L -> (x / y).makeAPLNumber()
                        else -> Rational.make(x.toBigInt(), y.toBigInt()).makeAPLNumber()
                    }
                },
                { x, y -> APLDouble(if (y == 0.0) 0.0 else x / y) },
                { x, y -> if (y == Complex.ZERO) APLDOUBLE_0 else (x / y).makeAPLNumber() },
                fnBigint = { x, y ->
                    when {
                        y == BigIntConstants.ZERO -> APLLONG_0
                        x % y == BigIntConstants.ZERO -> (x / y).makeAPLNumber()
                        else -> (Rational.make(x, BigIntConstants.ONE) / Rational.make(y, BigIntConstants.ONE)).makeAPLNumber()
                    }
                },
                fnRational = { x, y ->
                    when {
                        y == Rational.ZERO -> APLLONG_0
                        else -> (x / y).makeAPLNumber()
                    }
                })
        }

        override fun combine1ArgDouble(a: Double) = 1.0 / a
        override fun combine2ArgDouble(a: Double, b: Double) = a / b

        override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?) = eval1Arg(context, a, axis)
        override fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue) =
            inversibleStructuralUnder1Arg(this, baseFn, context, a)

        private val mulFn by lazy { MulAPLFunction().make(pos) }
        override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?) =
            eval2Arg(context, a, b, axis)

        override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?) =
            mulFn.eval2Arg(context, b, a, axis)

        override fun evalWithStructuralUnder2Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, b: APLValue): APLValue =
            inversibleStructuralUnder2Arg(this, baseFn, context, a, b)

        override fun identityValue() = APLLONG_1

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_1ARG_DOUBLE or OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)

        override val name1Arg: String
            get() = "reciprocal"
        override val name2Arg: String
            get() = "divide"
    }

    override fun make(instantiation: FunctionInstantiation) = DivAPLFunctionImpl(instantiation)
}

class NotAPLFunction : APLFunctionDescriptor {
    class NotAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> notOp(x, pos).makeAPLNumber() },
                { x -> notOp(x.toLong(), pos).makeAPLNumber() },
                { x ->
                    if (x.imaginary == 0.0) {
                        notOp(x.real.toLong(), pos).makeAPLNumber()
                    } else {
                        throwAPLException(APLIncompatibleDomainsException("Operation not supported for complex", pos))
                    }
                })
        }

        private fun notOp(v: Long, pos: Position): Long {
            val result = when (v) {
                0L -> 1L
                1L -> 0L
                else -> throwAPLException(APLIncompatibleDomainsException("Operation not supported for value", pos))
            }
            return result
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            if (axis != null) {
                throwAPLException(AxisNotSupported(pos))
            }
            val a1 = a.arrayify()
            if (a1.dimensions.size != 1) {
                throwAPLException(InvalidDimensionsException("Left argument must be a scalar or a 1-dimensional array", pos))
            }
            val b1 = b.arrayify()
            val map = HashSet<Any>()
            b1.iterateMembers { v ->
                map.add(v.makeKey())
            }
            val result = ArrayList<APLValue>()
            a1.iterateMembers { v ->
                if (!map.contains(v.makeKey())) {
                    result.add(v)
                }
            }
            return APLArrayList(dimensionsOfSize(result.size), result)
        }

        override fun combine1ArgLong(a: Long) = notOp(a, pos)

        override fun deriveBitwise() = BitwiseNotFunction()

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_1ARG_LONG)

        override val name1Arg get() = "not"
        override val name2Arg get() = "without"
    }

    override fun make(instantiation: FunctionInstantiation): APLFunction {
        return NotAPLFunctionImpl(instantiation)
    }
}

class ModAPLFunction : APLFunctionDescriptor {
    class ModAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> abs(x).makeAPLNumber() },
                { x -> abs(x).makeAPLNumber() },
                { x -> hypot(x.real, x.imaginary).makeAPLNumber() },
                fnBigInt = { x -> x.absoluteValue.makeAPLNumber() },
                fnRational = { x -> x.absoluteValue.makeAPLNumber() })
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> opLong(x, y).makeAPLNumber() },
                { x, y -> opDouble(x, y).makeAPLNumber() },
                { x, y -> complexMod(x, y).makeAPLNumber() },
                fnBigint = { x, y -> bigintMod(x, y).makeAPLNumber() },
                fnRational = { x, y -> rationalMod(x, y).makeAPLNumber() })
        }

        private fun opLong(x: Long, y: Long) =
            if (x == 0L) y else (y % x).let { result -> if ((x < 0 && y > 0) || (x > 0 && y < 0)) x + result else result }

        private fun opDouble(x: Double, y: Double) =
            if (x == 0.0) y else (y % x).let { result -> if ((x < 0 && y > 0) || (x > 0 && y < 0)) x + result else result }

        private fun bigintMod(x: BigInt, y: BigInt) =
            if (x == BigIntConstants.ZERO) y else (y % x).let { result -> if ((x < 0 && y > 0) || (x > 0 && y < 0)) x + result else result }

        private fun rationalMod(x: Rational, y: Rational) =
            if (x == Rational.ZERO) y else (y % x).let { result -> if ((x < 0 && y > 0) || (x > 0 && y < 0)) x + result else result }

        override fun combine2ArgLong(a: Long, b: Long) = opLong(a, b)
        override fun combine2ArgDouble(a: Double, b: Double) = opDouble(a, b)

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG or OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)

        override val name1Arg get() = "abs"
        override val name2Arg get() = "mod"
    }

    override fun make(instantiation: FunctionInstantiation) = ModAPLFunctionImpl(instantiation)
}

class PowerAPLFunction : APLFunctionDescriptor {
    class PowerAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> exp(x.toDouble()).makeAPLNumber() },
                { x -> exp(x).makeAPLNumber() },
                { x -> E.pow(x).makeAPLNumber() },
                fnBigInt = { x -> E.pow(x.toDouble()).makeAPLNumber() },
                fnRational = { x -> E.pow(x.toDouble()).makeAPLNumber() })
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y ->
                    when {
                        y > 0 -> (x.toBigInt().pow(y)).makeAPLNumber()
                        y < 0 -> if (x > 0) x.toRational().pow(y).makeAPLNumber() else x.toDouble().pow(y.toDouble()).makeAPLNumber()
                        else -> APLLONG_1
                    }
                },
                { x, y ->
                    if (x < 0) {
                        x.toComplex().pow(y.toComplex()).makeAPLNumber()
                    } else {
                        x.pow(y).makeAPLNumber()
                    }
                },
                { x, y -> x.pow(y).makeAPLNumber() },
                fnBigint = { x, y ->
                    if (y > 0) {
                        checkBigIntInRangeLong(y, pos)
                        x.pow(y.toLong()).makeAPLNumber()
                    } else if (y < 0) {
                        if (x > 0) {
                            checkBigIntInRangeLong(y, pos)
                            Rational.make(x, BigIntConstants.ONE).pow(y.toLong()).makeAPLNumber()
                        } else {
                            x.toDouble().pow(y.toDouble()).makeAPLNumber()
                        }
                    } else {
                        APLLONG_1
                    }
                },
                fnRational = { x, y ->
                    if (y.denominator != BigIntConstants.ONE) {
                        if (x < 0) {
                            x.toDouble().toComplex().pow(y.toDouble().toComplex()).makeAPLNumber()
                        } else {
                            x.toDouble().pow(y.toDouble()).makeAPLNumber()
                        }
                    } else {
                        val v0 = y.numerator
                        checkBigIntInRangeLong(v0, pos)
                        val v1 = y.toLongTruncated()
                        x.pow(v1).makeAPLNumber()
                    }
                })
        }

        private val logFn by lazy { LogAPLFunction().make(pos) }
        override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?) =
            logFn.eval1Arg(context, a, axis)

        override fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue) =
            inversibleStructuralUnder1Arg(this, baseFn, context, a)

        override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?) =
            logFn.eval2Arg(context, a, b, axis)

        override fun identityValue() = APLLONG_1

        override val name1Arg get() = "exp"
        override val name2Arg get() = "pow"
    }

    override fun make(instantiation: FunctionInstantiation) = PowerAPLFunctionImpl(instantiation)
}

fun complexFloor(z: Complex): Complex {
    var fr = floor(z.real)
    var dr = z.real - fr
    var fi = floor(z.imaginary)
    var di = z.imaginary - fi
    if (dr > 1) {
        fr += 1.0
        dr = 0.0
    }
    if (di > 1) {
        fi += 1.0
        di = 0.0
    }
    return when {
        dr + di < 1 -> Complex(fr, fi)
        dr < di -> Complex(fr, fi + 1.0)
        else -> Complex(fr + 1.0, fi)
    }
}

fun complexMod(a: Complex, b: Complex): Complex {
    return b - a * complexFloor(b / (a + if (a == Complex.ZERO) 1.0 else 0.0))
}

class MinAPLFunction : APLFunctionDescriptor {
    class MinAPLFunctionImpl(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
        override fun combine1Arg(a: APLSingleValue): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> x.makeAPLNumber() },
                { x -> floor(x).makeAPLNumber() },
                { x -> complexFloor(x).makeAPLNumber() },
                fnBigInt = { x -> x.makeAPLNumber() },
                fnRational = { x -> x.floor().makeAPLNumber() })
        }

        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> if (x < y) x.makeAPLNumber() else y.makeAPLNumber() },
                { x, y -> if (x < y) x.makeAPLNumber() else y.makeAPLNumber() },
                { x, y -> (if (x.real < y.real || (x.real == y.real && x.imaginary < y.imaginary)) x else y).makeAPLNumber() },
                { x, y -> if (x < y) APLChar(x) else APLChar(y) },
                fnBigint = { x, y -> if (x < y) x.makeAPLNumber() else y.makeAPLNumber() },
                fnRational = { x, y -> if (x < y) x.makeAPLNumber() else y.makeAPLNumber() })
        }

        override fun combine2ArgLong(a: Long, b: Long) = if (a < b) a else b
        override fun combine2ArgDouble(a: Double, b: Double) = if (a < b) a else b

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG or OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)

        override val name1Arg get() = "round down"
        override val name2Arg get() = "min"
    }

    override fun make(instantiation: FunctionInstantiation) = MinAPLFunctionImpl(instantiation)
}

fun complexCeiling(value: Complex): Complex {
    return -complexFloor(-value)
}

class MaxAPLFunction : APLFunctionDescriptor {
    class MaxAPLFunctionImpl(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
        override fun combine1Arg(a: APLSingleValue): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> x.makeAPLNumber() },
                { x -> ceil(x).makeAPLNumber() },
                { x -> complexCeiling(x).makeAPLNumber() },
                fnBigInt = { x -> x.makeAPLNumber() },
                fnRational = { x -> x.ceil().makeAPLNumber() })
        }

        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> if (x > y) x.makeAPLNumber() else y.makeAPLNumber() },
                { x, y -> if (x > y) x.makeAPLNumber() else y.makeAPLNumber() },
                { x, y -> (if (x.real > y.real || (x.real == y.real && x.imaginary > y.imaginary)) x else y).makeAPLNumber() },
                { x, y -> if (x > y) APLChar(x) else APLChar(y) },
                fnBigint = { x, y -> if (x > y) x.makeAPLNumber() else y.makeAPLNumber() },
                fnRational = { x, y -> if (x > y) x.makeAPLNumber() else y.makeAPLNumber() })
        }

        override fun combine2ArgLong(a: Long, b: Long) = if (a > b) a else b
        override fun combine2ArgDouble(a: Double, b: Double) = if (a > b) a else b

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG or OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)

        override val name1Arg get() = "round up"
        override val name2Arg get() = "max"
    }

    override fun make(instantiation: FunctionInstantiation) = MaxAPLFunctionImpl(instantiation)
}

class LogAPLFunction : APLFunctionDescriptor {
    class LogAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun combine1Arg(a: APLSingleValue): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> if (x < 0) x.toDouble().toComplex().ln().makeAPLNumber() else ln(x.toDouble()).makeAPLNumber() },
                { x -> if (x < 0) x.toComplex().ln().makeAPLNumber() else ln(x).makeAPLNumber() },
                { x -> x.ln().makeAPLNumber() })
        }

        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y ->
                    if (x < 0 || y < 0) {
                        y.toDouble().toComplex().log(x.toDouble()).makeAPLNumber()
                    } else {
                        log(y.toDouble(), x.toDouble()).makeAPLNumber()
                    }
                },
                { x, y -> if (x < 0 || y < 0) y.toComplex().log(x.toComplex()).makeAPLNumber() else log(y, x).makeAPLNumber() },
                { x, y -> y.log(x).makeAPLNumber() })
        }

        private val powerFn by lazy { PowerAPLFunction().make(pos) }
        override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?) =
            powerFn.eval1Arg(context, a, axis)

        override fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue) =
            inversibleStructuralUnder1Arg(this, baseFn, context, a)

        override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?) =
            powerFn.eval2Arg(context, a, b, axis)

        override val name1Arg get() = "natural log"
        override val name2Arg get() = "log"
    }

    override fun make(instantiation: FunctionInstantiation) = LogAPLFunctionImpl(instantiation)
}

class SinAPLFunction : APLFunctionDescriptor {
    class SinAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> sin(x.toDouble()).makeAPLNumber() },
                { x -> sin(x).makeAPLNumber() },
                { x -> complexSin(x).makeAPLNumber() })
        }

        override val name1Arg get() = "sin"
    }

    override fun make(instantiation: FunctionInstantiation) = SinAPLFunctionImpl(instantiation)
}

class CosAPLFunction : APLFunctionDescriptor {
    class CosAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> cos(x.toDouble()).makeAPLNumber() },
                { x -> cos(x).makeAPLNumber() },
                { x -> complexCos(x).makeAPLNumber() })
        }
    }

    override fun make(instantiation: FunctionInstantiation) = CosAPLFunctionImpl(instantiation)
}

class TanAPLFunction : APLFunctionDescriptor {
    class TanAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber) = APLDouble(tan(a.asDouble()))
        override val name1Arg get() = "tan"
    }

    override fun make(instantiation: FunctionInstantiation) = TanAPLFunctionImpl(instantiation)
}

class AsinAPLFunction : APLFunctionDescriptor {
    class AsinAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber) = APLDouble(asin(a.asDouble()))
        override val name1Arg get() = "asin"
    }

    override fun make(instantiation: FunctionInstantiation) = AsinAPLFunctionImpl(instantiation)
}

class AcosAPLFunction : APLFunctionDescriptor {
    class AcosAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber) = APLDouble(acos(a.asDouble()))
        override val name1Arg get() = "acos"
    }

    override fun make(instantiation: FunctionInstantiation) = AcosAPLFunctionImpl(instantiation)
}

class AtanAPLFunction : APLFunctionDescriptor {
    class AtanAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber) = APLDouble(atan(a.asDouble()))
        override val name1Arg get() = "atan"
    }

    override fun make(instantiation: FunctionInstantiation) = AtanAPLFunctionImpl(instantiation)
}

class SqrtAPLFunction : APLFunctionDescriptor {
    class SqrtAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> sqrtDouble(x.toDouble()) },
                { x -> sqrtDouble(x) },
                { x -> x.pow(COMPLEX_HALF).makeAPLNumber() },
                fnBigInt = { x -> sqrtDouble(x.toDouble()) },
                fnRational = { x -> sqrtDouble(x.toDouble()) })
        }

        private fun sqrtDouble(x: Double) = if (x < 0) x.pow(COMPLEX_HALF).makeAPLNumber() else sqrt(x).makeAPLNumber()

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> nthRootDouble(x.toDouble(), y.toDouble()) },
                { x, y -> nthRootDouble(x, y) },
                { x, y -> y.pow(1.0 / x).makeAPLNumber() },
                fnBigint = { x, y -> nthRootDouble(x.toDouble(), y.toDouble()) },
                fnRational = { x, y -> nthRootDouble(x.toDouble(), y.toDouble()) })
        }

        private fun nthRootDouble(x: Double, y: Double): APLNumber {
            return if (y < 0) {
                y.pow(x.toComplex().reciprocal()).makeAPLNumber()
            } else {
                y.pow(1.0 / x).makeAPLNumber()
            }
        }

        override val name1Arg get() = "square root"
        override val name2Arg get() = "nth root"

        companion object {
            val COMPLEX_HALF = 0.5.toComplex()
        }
    }

    override fun make(instantiation: FunctionInstantiation) = SqrtAPLFunctionImpl(instantiation)
}

class AndAPLFunction : APLFunctionDescriptor {
    class AndAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> opLong(x, y).makeAPLNumber() },
                { x, y ->
                    when {
                        x == 0.0 && (y == 0.0 || y == 1.0) -> APLDOUBLE_0
                        (x == 0.0 || x == 1.0) && y == 0.0 -> APLDOUBLE_0
                        x == 1.0 && y == 1.0 -> APLDOUBLE_1
                        else -> throwIllegalArgument()
                    }
                },
                { x, y -> if (x.imaginary == 0.0 && y.imaginary == 0.0) opDouble(x.real, y.real).makeAPLNumber() else throwIllegalArgument() },
                fnBigint = { x, y -> opBigint(x, y) },
                fnRational = { x, y -> opRational(x, y) })
        }

        private fun opLong(x: Long, y: Long): Long {
            return when {
                x == 0L && (y == 0L || y == 1L) -> 0L
                (x == 0L || x == 1L) && y == 0L -> 0L
                x == 1L && y == 1L -> 1L
                else -> throwIllegalArgument()
            }
        }

        private fun opDouble(x: Double, y: Double): Double {
            return when {
                x == 0.0 && (y == 0.0 || y == 1.0) -> 0.0
                (x == 0.0 || x == 1.0) && y == 0.0 -> 0.0
                x == 1.0 && y == 1.0 -> 1.0
                else -> throwIllegalArgument()
            }
        }

        private fun opBigint(x: BigInt, y: BigInt): APLValue {
            return when {
                x == BigIntConstants.ZERO && (y == BigIntConstants.ZERO || y == BigIntConstants.ONE) -> APLLONG_0
                (x == BigIntConstants.ZERO || x == BigIntConstants.ONE) && y == BigIntConstants.ZERO -> APLLONG_0
                x == BigIntConstants.ONE && x == BigIntConstants.ONE -> APLLONG_1
                else -> throwIllegalArgument()
            }
        }

        private fun opRational(x: Rational, y: Rational): APLValue {
            return when {
                x == Rational.ZERO && (y == Rational.ZERO || y == Rational.ONE) -> APLLONG_0
                (x == Rational.ZERO || y == Rational.ONE) && y == Rational.ZERO -> APLLONG_0
                x == Rational.ONE && y == Rational.ONE -> APLLONG_1
                else -> throwIllegalArgument()
            }
        }

        override fun combine2ArgLong(a: Long, b: Long) = opLong(a, b)
        override fun combine2ArgDouble(a: Double, b: Double) = opDouble(a, b)
        override fun deriveBitwise() = BitwiseAndFunction()
        override fun identityValue() = APLLONG_1

        private fun throwIllegalArgument(): Nothing {
            throwAPLException(APLIllegalArgumentException("Arguments to and must be 0 or 1", pos))
        }

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG or OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)

        override val name2Arg get() = "and"
    }

    override fun make(instantiation: FunctionInstantiation) = AndAPLFunctionImpl(instantiation)
}

class NandAPLFunction : APLFunctionDescriptor {
    class NandAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> opLong(x, y).makeAPLNumber() },
                { x, y -> opLong(x.toLong(), y.toLong()).makeAPLNumber() },
                { _, _ -> throwIllegalArgument() },
                fnBigint = { x, y -> opBigint(x, y).makeAPLNumber() })
        }

        private fun opLong(a: Long, b: Long) = when {
            a == 0L && b == 0L -> 1L
            a == 0L && b == 1L -> 1L
            a == 1L && b == 0L -> 1L
            a == 1L && b == 1L -> 0L
            else -> throwIllegalArgument()
        }

        private fun opBigint(a: BigInt, b: BigInt) = when {
            a == BigIntConstants.ZERO && b == BigIntConstants.ZERO -> 1L
            a == BigIntConstants.ZERO && b == BigIntConstants.ONE -> 1L
            a == BigIntConstants.ONE && b == BigIntConstants.ZERO -> 1L
            a == BigIntConstants.ONE && b == BigIntConstants.ONE -> 0L
            else -> throwIllegalArgument()
        }

        override fun combine2ArgLong(a: Long, b: Long) = opLong(a, b)

        override fun deriveBitwise() = BitwiseNandFunction()

        private fun throwIllegalArgument(): Nothing {
            throwAPLException(APLIllegalArgumentException("Arguments to nand must be 0 or 1", pos))
        }

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG)

        override val name1Arg get() = "nand"
    }

    override fun make(instantiation: FunctionInstantiation) = NandAPLFunctionImpl(instantiation)
}

class NorAPLFunction : APLFunctionDescriptor {
    class NorAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> opLong(x, y).makeAPLNumber() },
                { x, y -> opLong(x.toLong(), y.toLong()).makeAPLNumber() },
                { _, _ -> throwIllegalArgument() },
                fnBigint = { x, y -> opBigint(x, y).makeAPLNumber() })
        }

        private fun opLong(x: Long, y: Long): Long {
            return when {
                x == 0L && y == 0L -> 1L
                x == 0L && y == 1L -> 0L
                x == 1L && y == 0L -> 0L
                x == 1L && y == 1L -> 0L
                else -> throwIllegalArgument()
            }
        }

        private fun opBigint(x: BigInt, y: BigInt): Long {
            return when {
                x == BigIntConstants.ZERO && y == BigIntConstants.ZERO -> 1L
                x == BigIntConstants.ZERO && y == BigIntConstants.ONE -> 0L
                x == BigIntConstants.ONE && y == BigIntConstants.ZERO -> 0L
                x == BigIntConstants.ONE && y == BigIntConstants.ONE -> 0L
                else -> throwIllegalArgument()
            }
        }

        override fun combine2ArgLong(a: Long, b: Long) = opLong(a, b)

        override fun deriveBitwise() = BitwiseNorFunction()

        private fun throwIllegalArgument(): Nothing {
            throwAPLException(APLIllegalArgumentException("Arguments to nor must be 0 or 1", pos))
        }

        override val name1Arg get() = "nor"
    }

    override fun make(instantiation: FunctionInstantiation) = NorAPLFunctionImpl(instantiation)
}

fun integerGcd(m: Long, n: Long): Long {
    if (m == 0L) return n
    if (n == 0L) return m
    var aa = 1L
    var b = 1L
    var a = 0L
    var bb = 0L
    var c = m.absoluteValue
    var d = n.absoluteValue
    while (true) {
        val r = c % d
        if (r == 0L) return d
        val q = c / d
        val ta = aa
        val tb = bb
        c = d
        d = r
        aa = a
        a = ta - q * a
        bb = b
        b = tb - q * b
    }
}

fun floatGcd(a: Double, b: Double): Double {
    if (!a.isFinite() || !b.isFinite()) {
        throw ArithmeticException("gcd on non-finite doubles")
    }
    var a1 = a.absoluteValue
    var b1 = b.absoluteValue
    if (b1 < a1) {
        val tmp = b1
        b1 = a1
        a1 = tmp
    }
    while (true) {
        if (a1.absoluteValue < 0.00001) return b1
        val r = b1.rem(a1)
        b1 = a1
        a1 = r
    }
}

fun rationalGcd(a: Rational, b: Rational): Rational {
    var a1 = a.absoluteValue
    var b1 = b.absoluteValue
    if (b1 < a1) {
        val tmp = b1
        b1 = a1
        a1 = tmp
    }
    while (true) {
        if (a1.signum() == 0) {
            return b1
        }
        val r = b1.rem(a1)
        b1 = a1
        a1 = r
    }
}

fun complexGcd(a: Complex, b: Complex): Complex {
    var a1 = a.nearestGaussian()
    var b1 = b.nearestGaussian()
    while (true) {
        if (a1.abs() > b1.abs()) {
            val tmp = a1
            a1 = b1
            b1 = tmp
        }
        if (a1.abs() < 0.2) {
            return b1
        }
        val quot = b1 / a1
        val q = quot.nearestGaussian()
        val r = b1 - q * a1
        b1 = a1
        a1 = r
    }
}

class OrAPLFunction : APLFunctionDescriptor {
    class OrAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> opLong(x, y).makeAPLNumber() },
                { x, y ->
                    when {
                        x == 0.0 && y == 0.0 -> APLDOUBLE_0
                        (x == 0.0 || x == 1.0) && y == 1.0 -> APLDOUBLE_1
                        x == 1.0 && (y == 0.0 || y == 1.0) -> APLDOUBLE_1
                        else -> throwIllegalArgument()
                    }
                },
                { x, y -> if (x.imaginary == 0.0 && y.imaginary == 0.0) opDouble(x.real, y.real).makeAPLNumber() else throwIllegalArgument() },
                fnBigint = { x, y -> opBigInt(x, y) },
                fnRational = { x, y -> opRational(x, y) })
        }

        private fun opLong(x: Long, y: Long) = when {
            x == 0L && y == 0L -> 0L
            (x == 0L || x == 1L) && y == 1L -> 1L
            x == 1L && (y == 0L || y == 1L) -> 1L
            else -> throwIllegalArgument()
        }

        private fun opDouble(x: Double, y: Double) = when {
            x == 0.0 && y == 0.0 -> 0.0
            (x == 0.0 || x == 1.0) && y == 1.0 -> 1.0
            x == 1.0 && (y == 0.0 || y == 1.0) -> 1.0
            else -> throwIllegalArgument()
        }

        private fun opBigInt(x: BigInt, y: BigInt): APLValue = when {
            x == BigIntConstants.ZERO && y == BigIntConstants.ZERO -> APLLONG_0
            (x == BigIntConstants.ZERO || y == BigIntConstants.ONE) && y == BigIntConstants.ONE -> APLLONG_1
            x == BigIntConstants.ONE && (y == BigIntConstants.ZERO || y == BigIntConstants.ONE) -> APLLONG_1
            else -> throwIllegalArgument()
        }

        private fun opRational(x: Rational, y: Rational): APLValue = when {
            x == Rational.ZERO && y == Rational.ZERO -> APLLONG_0
            (x == Rational.ZERO || x == Rational.ONE) && y == Rational.ONE -> APLLONG_1
            x == Rational.ONE && (y == Rational.ZERO || y == Rational.ONE) -> APLLONG_1
            else -> throwIllegalArgument()
        }

        override fun combine2ArgLong(a: Long, b: Long) = opLong(a, b)
        override fun combine2ArgDouble(a: Double, b: Double) = opDouble(a, b)

        override fun identityValue() = APLLONG_0
        override fun deriveBitwise() = BitwiseOrFunction()

        private fun throwIllegalArgument(): Nothing {
            throwAPLException(APLIllegalArgumentException("Arguments to or must be 0 or 1", pos))
        }

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG or OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)

        override val name1Arg get() = "or"
    }

    override fun make(instantiation: FunctionInstantiation) = OrAPLFunctionImpl(instantiation)
}

class BinomialAPLFunction : APLFunctionDescriptor {
    class BinomialAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(pos, a,
                { x -> doubleGamma((x + 1).toDouble()).makeAPLNumber() },
                { x -> doubleGamma(x + 1.0).makeAPLNumber() },
                { x -> complexGamma(x + 1.0).makeAPLNumber() })
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y ->
                    if (x >= 0 && x <= Int.MAX_VALUE && y >= 0 && y <= Int.MAX_VALUE && y >= x) {
                        longBinomial(y.toInt(), x.toInt()).makeAPLNumber()
                    } else {
                        doubleBinomialWithException(x.toDouble(), y.toDouble(), pos).makeAPLNumber()
                    }
                },
                { x, y -> doubleBinomialWithException(x, y, pos).makeAPLNumber() },
                { x, y -> complexBinomial(x, y).makeAPLNumber() })
        }

        private fun doubleBinomialWithException(a: Double, b: Double, pos: Position): Double {
            fun nearInt(n: Double) = n.rem(1) == 0.0

            try {
                val row = (if (a < 0) 4 else 0) or (if (b < 0) 2 else 0) or (if (b < a) 1 else 0)
                val caseTable = arrayOf(1, 0, -1, 1, 0, -1, 1, 0)
                val e = caseTable[row]
                return when {
                    e == 0 -> 0.0
                    e != 1 -> throw IllegalStateException("caseTable value is -1. ${a}, ${b}, ${row}, ${e}")
                    !nearInt(a) || !nearInt(b) -> doubleBinomial(a, b)
                    else -> doubleBinomial(a, b)
                }
            } catch (e: IllegalArgumentException) {
                throw APLIncompatibleDomainsException("Binomial: invalid arguments: ${a},${b}", pos, e)
            }
        }

        override val name1Arg get() = "gamma"
        override val name2Arg get() = "binomial"
    }

    override fun make(instantiation: FunctionInstantiation) = BinomialAPLFunctionImpl(instantiation)
}

class GcdAPLFunction : APLFunctionDescriptor {
    class GcdAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> opLong(x, y).makeAPLNumber() },
                { x, y -> floatGcd(x, y).makeAPLNumber() },
                { x, y -> complexGcd(x, y).makeAPLNumber() },
                fnBigint = { x, y -> x.gcd(y).makeAPLNumber() },
                fnRational = { x, y -> opRational(x, y) })
        }

        private fun opLong(x: Long, y: Long) = integerGcd(x, y)
        private fun opDouble(x: Double, y: Double) = floatGcd(x, y)
        private fun opRational(x: Rational, y: Rational): APLValue = rationalGcd(x, y).makeAPLNumber()

        override fun combine2ArgLong(a: Long, b: Long) = opLong(a, b)
        override fun combine2ArgDouble(a: Double, b: Double) = opDouble(a, b)

        override fun identityValue() = APLLONG_0
        override fun deriveBitwise() = BitwiseOrFunction()

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG or OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)

        override val name1Arg get() = "gcd"
    }

    override fun make(instantiation: FunctionInstantiation) = GcdAPLFunctionImpl(instantiation)
}

class LcmAPLFunction : APLFunctionDescriptor {
    class LcmAPLFunctionImpl(pos: FunctionInstantiation) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> opLong(x, y).makeAPLNumber() },
                { x, y -> (x * (y / floatGcd(x, y))).makeAPLNumber() },
                { x, y -> (y * (x / complexGcd(x, y))).nearestGaussian().makeAPLNumber() },
                fnBigint = { x, y -> opBigint(x, y) },
                fnRational = { x, y -> opRational(x, y) })
        }

        private fun opLong(x: Long, y: Long): Long {
            val gcd = integerGcd(x, y)
            return if (gcd == 0L) {
                0
            } else {
                return x * (y / gcd)
            }
        }

        private fun opDouble(x: Double, y: Double): Double {
            val gcd = floatGcd(x, y)
            return if (gcd == 0.0) {
                0.0
            } else {
                (x * (y / gcd))
            }
        }

        private fun opBigint(x: BigInt, y: BigInt): APLBigInt {
            val gcd = x.gcd(y)
            return if (gcd == BigIntConstants.ZERO) {
                BigIntConstants.ZERO.makeAPLNumber()
            } else {
                (x * (y / gcd)).makeAPLNumber()
            }
        }

        private fun opRational(x: Rational, y: Rational): APLValue {
            val gcd = rationalGcd(x, y)
            return if (gcd == Rational.ZERO) {
                Rational.ZERO.makeAPLNumber()
            } else {
                (x * (y / gcd)).makeAPLNumber()
            }
        }

        override fun combine2ArgLong(a: Long, b: Long) = opLong(a, b)
        override fun combine2ArgDouble(a: Double, b: Double) = opDouble(a, b)
        override fun deriveBitwise() = BitwiseAndFunction()
        override fun identityValue() = APLLONG_1

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG or OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)

        override val name2Arg get() = "lcm"
    }

    override fun make(instantiation: FunctionInstantiation) = LcmAPLFunctionImpl(instantiation)
}
