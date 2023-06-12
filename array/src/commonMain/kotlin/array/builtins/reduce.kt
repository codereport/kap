package array.builtins

import array.*
import com.dhsdevelopments.mpbignum.LongExpressionOverflow
import kotlin.math.absoluteValue

class ReduceResult1Arg(
    val context: RuntimeContext,
    val fn: APLFunction,
    val arg: APLValue,
    opAxis: Int,
    val pos: Position,
    val savedStack: StorageStack.StorageStackFrame?
) : APLArray() {
    override val dimensions: Dimensions
    private val stepLength: Int
    private val sizeAlongAxis: Int
    private val fromSourceMul: Int
    private val toDestMul: Int

    init {
        val argDimensions = arg.dimensions
        val argMultipliers = argDimensions.multipliers()

        ensureValidAxis(opAxis, argDimensions, pos)

        stepLength = argMultipliers[opAxis]
        sizeAlongAxis = argDimensions[opAxis]
        dimensions = argDimensions.remove(opAxis)

        val multipliers = dimensions.multipliers()

        fromSourceMul = if (opAxis == 0) dimensions.contentSize() else multipliers[opAxis - 1]
        toDestMul = fromSourceMul * argDimensions[opAxis]
    }

    override fun valueAt(p: Int): APLValue {
        return if (sizeAlongAxis == 0) {
            fn.identityValue()
        } else {
            val highPosition = p / fromSourceMul
            val lowPosition = p % fromSourceMul
            val posInSrc = highPosition * toDestMul + lowPosition

            val specialisedType = arg.specialisedType
            val engine = context.engine
            when {
                specialisedType === ArrayMemberType.LONG && fn.optimisationFlags.is2ALongLong -> {
                    var curr = arg.valueAtLong(posInSrc, pos)
                    withPossibleSavedStack(savedStack) {
                        var i = 1
                        try {
                            while (i < sizeAlongAxis) {
                                engine.checkInterrupted(pos)
                                curr = fn.eval2ArgLongLong(context, curr, arg.valueAtLong(i++ * stepLength + posInSrc, pos), null)
                            }
                            APLLong(curr)
                        } catch (e: LongExpressionOverflow) {
                            // If we get here, the current evaluation must have overflowed, so continue in generic mode
                            var curr0: APLValue = APLBigInt(e.result)
                            while (i < sizeAlongAxis) {
                                engine.checkInterrupted(pos)
                                curr0 = fn.eval2Arg(context, curr0, arg.valueAt(i++ * stepLength + posInSrc), null)
                            }
                            curr0
                        }
                    }
                }
                specialisedType === ArrayMemberType.DOUBLE && fn.optimisationFlags.is2ADoubleDouble -> {
                    var curr = arg.valueAtDouble(posInSrc, pos)
                    withPossibleSavedStack(savedStack) {
                        for (i in 1 until sizeAlongAxis) {
                            engine.checkInterrupted(pos)
                            curr = fn.eval2ArgDoubleDouble(context, curr, arg.valueAtDouble(i * stepLength + posInSrc, pos), null)
                        }
                    }
                    curr.makeAPLNumber()
                }
                else -> {
                    fn.reduce(context, arg, sizeAlongAxis, stepLength, posInSrc, savedStack, null)
                }
            }
        }
    }

    override fun unwrapDeferredValue(): APLValue {
        return unwrapEnclosedSingleValue(this)
    }
}

fun defaultReduceImpl(
    fn: APLFunction,
    context: RuntimeContext,
    arg: APLValue,
    offset: Int,
    sizeAlongAxis: Int,
    stepLength: Int,
    pos: Position,
    savedStack: StorageStack.StorageStackFrame?,
    functionAxis: APLValue?
): APLValue {
    val engine = context.engine
    var curr = arg.valueAt(offset)
    withPossibleSavedStack(savedStack) {
        for (i in 1 until sizeAlongAxis) {
            engine.checkInterrupted(pos)
            curr = fn.eval2Arg(context, curr, arg.valueAt(i * stepLength + offset), functionAxis).collapse()
        }
    }
    return curr
}

fun unwrapEnclosedSingleValue(value: APLValue): APLValue {
    return if (value.dimensions.isEmpty()) {
        EnclosedAPLValue.make(value.valueAt(0).unwrapDeferredValue())
    } else {
        value
    }
}

class ReduceNWiseResultValue(
    val context: RuntimeContext,
    val fn: APLFunction,
    val reductionSize: Int,
    val b: APLValue,
    operatorAxis: Int,
    val savedStack: StorageStack.StorageStackFrame?
) : APLArray() {
    override val dimensions: Dimensions

    private val axisActionFactors: AxisActionFactors
    private val highMultiplier: Int
    private val axisMultiplier: Int
    private val dir: Int
    private val reductionSizeAbsolute: Int
    private val cachedSources = makeAtomicRefArray<APLValue>(b.dimensions.contentSize())

    init {
        val bDimensions = b.dimensions
        dimensions = Dimensions(IntArray(bDimensions.size) { i ->
            val s = bDimensions[i]
            if (i == operatorAxis) {
                s - reductionSize.absoluteValue + 1
            } else {
                s
            }
        })

        val bMultipliers = bDimensions.multipliers()
        axisMultiplier = bMultipliers[operatorAxis]
        highMultiplier = axisMultiplier * bDimensions[operatorAxis]
        dir = if (reductionSize < 0) -1 else 1
        reductionSizeAbsolute = reductionSize.absoluteValue

        axisActionFactors = AxisActionFactors(dimensions, operatorAxis)
    }

    private fun lookupSource(p: Int): APLValue {
        return cachedSources.checkOrUpdate(p) {
            b.valueAt(p)
        }
    }

    override fun valueAt(p: Int): APLValue {
        axisActionFactors.withFactors(p) { high, low, axisCoord ->
            var pos = if (reductionSize < 0) reductionSizeAbsolute - 1 else 0
            var curr = lookupSource((high * highMultiplier) + ((axisCoord + pos) * axisMultiplier) + low)
            withPossibleSavedStack(savedStack) {
                repeat(reductionSizeAbsolute - 1) {
                    pos += dir
                    val value = lookupSource((high * highMultiplier) + ((axisCoord + pos) * axisMultiplier) + low)
                    curr = fn.eval2Arg(context, curr, value, null)
                }
            }
            return curr
        }
    }
}

abstract class ReduceFunctionImpl(val fn: APLFunction, pos: FunctionInstantiation) : APLFunction(pos), SaveStackCapable by SaveStackSupport(fn) {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        val axisParam = if (axis == null) null else axis.ensureNumber(pos).asInt(pos)
        return if (a.rank == 0) {
            if (axisParam != null && axisParam != 0) {
                throwAPLException(IllegalAxisException(axisParam, a.dimensions, pos))
            }
            a
        } else {
            val axisInt = axisParam ?: defaultAxis(a)
            ensureValidAxis(axisInt, a.dimensions, pos)
            ReduceResult1Arg(context, fn, a, axisInt, pos, savedStack(context))
        }
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        val bDimensions = b.dimensions
        val axisParam = if (axis == null) null else axis.ensureNumber(pos).asInt(pos)
        val size = a.ensureNumber(pos).asInt(pos)
        if (bDimensions.size == 0) {
            if (axisParam != null && axisParam != 0) {
                throwAPLException(IllegalAxisException(axisParam, bDimensions, pos))
            }
            return when (size) {
                1 -> APLArrayImpl(dimensionsOfSize(1), arrayOf(b))
                0 -> APLNullValue.APL_NULL_INSTANCE
                -1 -> APLArrayImpl(dimensionsOfSize(1), arrayOf(APLLONG_0))
                else -> throwAPLException(InvalidDimensionsException("Invalid left argument for scalar right arg", pos))
            }
        }
        val axisInt = axisParam ?: defaultAxis(b)
        ensureValidAxis(axisInt, bDimensions, pos)
        return when {
            size.absoluteValue > bDimensions[axisInt] + 1 -> {
                throwAPLException(InvalidDimensionsException("Left argument is too large", pos))
            }
            size.absoluteValue == bDimensions[axisInt] + 1 -> {
                val d = Dimensions(IntArray(bDimensions.size) { i ->
                    if (i == axisInt) 0 else bDimensions[i]
                })
                APLArrayImpl(d, emptyArray())
            }
            else -> {
                ReduceNWiseResultValue(context, fn, size, b, axisInt, savedStack(context))
            }
        }
    }

    abstract fun defaultAxis(a: APLValue): Int
}

class ReduceFunctionImplLastAxis(fn: APLFunction, pos: FunctionInstantiation) : ReduceFunctionImpl(fn, pos) {
    override fun defaultAxis(a: APLValue) = a.dimensions.size - 1
    override val name1Arg = "reduce last axis [${fn.name2Arg}]"
}

class ReduceFunctionImplFirstAxis(fn: APLFunction, pos: FunctionInstantiation) : ReduceFunctionImpl(fn, pos) {
    override fun defaultAxis(a: APLValue) = 0
    override val name1Arg = "reduce first axis [${fn.name2Arg}]"
}

class ReduceOpLastAxis : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: FunctionInstantiation): APLFunctionDescriptor {
        return ReduceOpFunctionDescriptor(fn)
    }

    class ReduceOpFunctionDescriptor(val fn: APLFunction) : APLFunctionDescriptor {
        override fun make(instantiation: FunctionInstantiation): APLFunction {
            return ReduceFunctionImplLastAxis(fn, instantiation)
        }
    }
}

class ReduceOpFirstAxis : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: FunctionInstantiation): APLFunctionDescriptor {
        return ReduceOpFunctionDescriptor(fn)
    }

    class ReduceOpFunctionDescriptor(val fn: APLFunction) : APLFunctionDescriptor {
        override fun make(instantiation: FunctionInstantiation): APLFunction {
            return ReduceFunctionImplFirstAxis(fn, instantiation)
        }
    }
}

// Scan is similar in concept to reduce, so we'll keep it in this file

class ScanResult1Arg(val context: RuntimeContext, val fn: APLFunction, val fnAxis: APLValue?, val a: APLValue, axis: Int) : APLArray() {
    override val dimensions = a.dimensions

    private val cachedResults = makeAtomicRefArray<APLValue>(dimensions.contentSize())
    private val axisActionFactors = AxisActionFactors(dimensions, axis)

    override fun valueAt(p: Int): APLValue {
        axisActionFactors.withFactors(p) { high, low, axisCoord ->
            var currIndex = axisCoord
            var leftValue: APLValue
            while (true) {
                val index = axisActionFactors.indexForAxis(high, low, currIndex)
                if (currIndex == 0) {
                    leftValue = cachedResults.checkOrUpdate(index) { a.valueAt(index) }
                    break
                } else {
                    val cachedVal = cachedResults[index]
                    if (cachedVal != null) {
                        leftValue = cachedVal
                        break
                    }
                }
                currIndex--
            }

            if (currIndex < axisCoord) {
                for (i in (currIndex + 1)..axisCoord) {
                    val index = axisActionFactors.indexForAxis(high, low, i)
                    leftValue = cachedResults.checkOrUpdate(index) { fn.eval2Arg(context, leftValue, a.valueAt(index), fnAxis).collapse() }
                }
            }

            return leftValue
        }
    }
}

abstract class ScanFunctionImpl(val fn: APLFunction, pos: FunctionInstantiation) : APLFunction(pos) {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        val axisParam = if (axis != null) axis.ensureNumber(pos).asInt(pos) else null
        return if (a.rank == 0) {
            if (axisParam != null && axisParam != 0) {
                throwAPLException(IllegalAxisException(axisParam, a.dimensions, pos))
            }
            a
        } else {
            val v = axisParam ?: defaultAxis(a)
            ensureValidAxis(v, a.dimensions, pos)
            ScanResult1Arg(context, fn, axis, a, v)
        }
    }

    abstract fun defaultAxis(a: APLValue): Int
}

class ScanLastAxisFunctionImpl(fn: APLFunction, pos: FunctionInstantiation) : ScanFunctionImpl(fn, pos) {
    override fun defaultAxis(a: APLValue) = a.dimensions.size - 1
}

class ScanFirstAxisFunctionImpl(fn: APLFunction, pos: FunctionInstantiation) : ScanFunctionImpl(fn, pos) {
    override fun defaultAxis(a: APLValue) = 0
}

class ScanLastAxisOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: FunctionInstantiation): APLFunctionDescriptor {
        return ScanOpFunctionDescriptor(fn)
    }

    class ScanOpFunctionDescriptor(val fn: APLFunction) : APLFunctionDescriptor {
        override fun make(instantiation: FunctionInstantiation): APLFunction {
            return ScanLastAxisFunctionImpl(fn, instantiation)
        }
    }
}

class ScanFirstAxisOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: FunctionInstantiation): APLFunctionDescriptor {
        return ScanOpFunctionDescriptor(fn)
    }

    class ScanOpFunctionDescriptor(val fn: APLFunction) : APLFunctionDescriptor {
        override fun make(instantiation: FunctionInstantiation): APLFunction {
            return ScanFirstAxisFunctionImpl(fn, instantiation)
        }
    }
}
