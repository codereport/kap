package array.builtins

import array.*

open class OuterJoinResult(
    val context: RuntimeContext,
    val a: APLValue,
    val b: APLValue,
    val fn: APLFunction,
    val pos: Position
) : APLArray() {
    final override val dimensions: Dimensions
    protected val divisor: Int
    protected val aScalar: Boolean
    protected val bScalar: Boolean

    init {
        val aDimensions = a.dimensions
        aScalar = aDimensions.size == 0
        val bDimensions = b.dimensions
        bScalar = bDimensions.size == 0
        dimensions = Dimensions(IntArray(aDimensions.size + bDimensions.size) { index ->
            if (index < aDimensions.size) aDimensions[index] else bDimensions[index - aDimensions.size]
        })

        divisor = b.size
    }

    override fun valueAt(p: Int): APLValue {
        val aPosition = p / divisor
        val bPosition = p % divisor
        return fn.eval2Arg(context, if (aScalar) a else a.valueAt(aPosition), if (bScalar) b else b.valueAt(bPosition), null)
    }
}

class OuterJoinResultLong(
    context: RuntimeContext,
    a: APLValue,
    b: APLValue,
    fn: APLFunction,
    pos: Position
) : OuterJoinResult(context, a, b, fn, pos) {
    override val specialisedType get() = ArrayMemberType.LONG

    init {
        assertx(!aScalar && !bScalar)
    }

    override fun valueAtLong(p: Int, pos: Position?): Long {
        val aPosition = p / divisor
        val bPosition = p % divisor
        return fn.eval2ArgLongLong(context, a.valueAtLong(aPosition, pos), b.valueAtLong(bPosition, pos), null)
    }
}

class OuterJoinResultDouble(
    context: RuntimeContext,
    a: APLValue,
    b: APLValue,
    fn: APLFunction,
    pos: Position
) : OuterJoinResult(context, a, b, fn, pos) {
    override val specialisedType get() = ArrayMemberType.DOUBLE

    init {
        assertx(!aScalar && !bScalar)
    }

    override fun valueAtDouble(p: Int, pos: Position?): Double {
        val aPosition = p / divisor
        val bPosition = p % divisor
        return fn.eval2ArgDoubleDouble(context, a.valueAtDouble(aPosition, pos), b.valueAtDouble(bPosition, pos), null)
    }
}

class InnerJoinResult(
    val context: RuntimeContext,
    val a: APLValue,
    val b: APLValue,
    val fn1: APLFunction,
    val fn2: APLFunction,
    val pos: Position
) : APLArray() {

    override val dimensions: Dimensions
    private val aDimensions: Dimensions
    private val bDimensions: Dimensions
    private val highFactor: Int
    private val axisSize: Int
    private val axisDimensions: Dimensions
    private val bStepSize: Int

    init {
        aDimensions = a.dimensions
        bDimensions = b.dimensions
        val leftSize = aDimensions.size - 1
        val rightSize = bDimensions.size - 1
        dimensions = Dimensions(IntArray(leftSize + rightSize) { index ->
            if (index < leftSize) aDimensions[index] else bDimensions[index - leftSize + 1]
        })

        axisSize = aDimensions[aDimensions.size - 1]
        axisDimensions = dimensionsOfSize(axisSize)
        bStepSize = bDimensions.multipliers()[0]

        highFactor = if (leftSize == 0) {
            dimensions.contentSize()
        } else {
            val m = dimensions.multipliers()
            m[leftSize - 1]
        }
    }

    override fun valueAt(p: Int): APLValue {
        val posInA = (p / highFactor) * axisSize
        val posInB = p % highFactor

        var pa = posInA
        val leftArg = APLArrayImpl.make(axisDimensions) { a.valueAt(pa++) }

        var pb = posInB
        val rightArg = APLArrayImpl.make(axisDimensions) { b.valueAt(pb).also { pb += bStepSize } }

        val v = fn2.eval2Arg(context, leftArg, rightArg, null)
        return ReduceResult1Arg(context, fn1, v, 0, pos)
    }
}

class OuterJoinOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: Position): APLFunctionDescriptor {
        return OuterInnerJoinOp.OuterJoinFunctionDescriptor(fn)
    }
}

class OuterInnerJoinOp : APLOperatorTwoArg {
    override fun combineFunction(fn0: APLFunction, fn1: APLFunction, opPos: Position): APLFunctionDescriptor {
        return if (fn0 is NullFunction.NullFunctionImpl) {
            OuterJoinFunctionDescriptor(fn1)
        } else {
            InnerJoinFunctionDescriptor(fn0, fn1)
        }
    }


    class OuterJoinFunctionDescriptor(val fnInner: APLFunction) : APLFunctionDescriptor {
        class OuterJoinFunctionImpl(pos: Position, fn: APLFunction) : NoAxisAPLFunction(pos, listOf(fn)) {
            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
                val sta = a.specialisedType
                val stb = b.specialisedType
                return when {
                    sta === ArrayMemberType.LONG && stb === ArrayMemberType.LONG && fn.optimisationFlags.is2ALongLong -> {
                        OuterJoinResultLong(context, a, b, fn, pos)
                    }
                    sta === ArrayMemberType.DOUBLE && stb === ArrayMemberType.DOUBLE && fn.optimisationFlags.is2ADoubleDouble -> {
                        OuterJoinResultDouble(context, a, b, fn, pos)
                    }
                    else -> OuterJoinResult(context, a, b, fn, pos)
                }
            }

            override fun copy(fns: List<APLFunction>) = OuterJoinFunctionImpl(pos, fns[0])

            val fn = fns[0]

            override val name2Arg get() = "outer product"
        }

        override fun make(pos: Position): APLFunction {
            return OuterJoinFunctionImpl(pos, fnInner)

        }
    }

    class InnerJoinFunctionDescriptor(val fn0Inner: APLFunction, val fn1Inner: APLFunction) : APLFunctionDescriptor {
        class InnerJoinFunctionImpl(pos: Position, fn0: APLFunction, fn1: APLFunction) : NoAxisAPLFunction(pos, listOf(fn0, fn1)) {
            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
                val aDimensions = a.dimensions
                val bDimensions = b.dimensions

                fun scalarOrVector(d: Dimensions) = d.size == 0 || (d.size == 1 && d[0] == 1)

                val a1 = when {
                    scalarOrVector(aDimensions) && scalarOrVector(bDimensions) -> a.arrayify()
                    scalarOrVector(aDimensions) -> ConstantArray(dimensionsOfSize(bDimensions[0]), a.singleValueOrError())
                    else -> a
                }
                val b1 = when {
                    scalarOrVector(aDimensions) && scalarOrVector(bDimensions) -> b.arrayify()
                    scalarOrVector(bDimensions) -> ConstantArray(
                        dimensionsOfSize(aDimensions[aDimensions.size - 1]),
                        b.singleValueOrError())
                    else -> b
                }
                val a1Dimensions = a1.dimensions
                val b1Dimensions = b1.dimensions
                if (a1Dimensions[a1Dimensions.size - 1] != b1Dimensions[0]) {
                    throwAPLException(InvalidDimensionsException("a and b dimensions are incompatible", pos))
                }
                return if (a1Dimensions.size == 1 && b1Dimensions.size == 1) {
                    val v = fn1.eval2Arg(context, a1, b1, null)
                    ReduceResult1Arg(context, fn0, v, 0, pos)
                } else {
                    InnerJoinResult(context, a1, b1, fn0, fn1, pos)
                }
            }

            override fun copy(fns: List<APLFunction>) = InnerJoinFunctionImpl(pos, fns[0], fns[1])

            val fn0 = fns[0]
            val fn1 = fns[1]

            override val name2Arg get() = "inner product"
        }

        override fun make(pos: Position): APLFunction {
            return InnerJoinFunctionImpl(pos, fn0Inner, fn1Inner)
        }
    }
}

class NullFunction : APLFunctionDescriptor {
    class NullFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            throwAPLException(APLEvalException("null function cannot be called", pos))
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            throwAPLException(APLEvalException("null function cannot be called", pos))
        }
    }

    override fun make(pos: Position): APLFunction {
        return NullFunctionImpl(pos)
    }

}
