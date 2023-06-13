package array.builtins

import array.*
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.max

class Concatenated1DArrays(private val a: APLValue, private val b: APLValue) : APLArray() {
    init {
        assertx(a.rank == 1 && b.rank == 1)
    }

    private val aSize = a.dimensions[0]
    private val bSize = b.dimensions[0]
    override val dimensions = dimensionsOfSize(aSize + bSize)

    override val labels by lazy { resolveLabels() }

    override fun valueAt(p: Int): APLValue {
        return if (p >= aSize) b.valueAt(p - aSize) else a.valueAt(p)
    }

    override fun collapseInt(): APLValue {
        return if (a is APLString && b is APLString) {
            APLString(IntArray(dimensions[0]) { i ->
                if (i < aSize) {
                    a.content[i]
                } else {
                    b.content[i - aSize]
                }
            })
        } else {
            super.collapseInt()
        }
    }

    private fun resolveLabels(): DimensionLabels? {
        val aLabels = a.labels
        val bLabels = b.labels
        if (aLabels == null && bLabels == null) {
            return null
        }

        val newLabels = ArrayList<AxisLabel?>()
        fun addNulls(n: Int) {
            repeat(n) {
                newLabels.add(null)
            }
        }

        fun processArg(n: Int, labels: DimensionLabels?) {
            if (labels == null) {
                addNulls(n)
            } else {
                val labelsList = labels.labels[0]
                if (labelsList == null) {
                    addNulls(n)
                } else {
                    labelsList.forEach { l ->
                        newLabels.add(l)
                    }
                }
            }
        }

        processArg(aSize, aLabels)
        processArg(bSize, bLabels)

        val allLabels = ArrayList<List<AxisLabel?>?>()
        allLabels.add(newLabels)
        return DimensionLabels(allLabels)
    }
}

abstract class ConcatenateAPLFunctionImpl(pos: FunctionInstantiation) : APLFunction(pos) {
    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        // The APL concept of using a non-integer axis to specify that you want to add a dimension (i.e. the laminate
        // function) is a bit confusing and this operation should really have a different syntax.
        //
        // The reason this method is confusing is because it relies on the concept of "near integer". This is an
        // unreliable concept that KAP tries to avoid. In this particular case, it's not too bad since the axis
        // is almost always specified explicitly (and usually .5). We choose the completely arbitrary value
        // of 0.01 for the check.
        //
        // Another problem with the APL syntax is that it chooses the axis as being the argument rounded up
        // to the nearest integer. That means that when the index offset is set to 0 (which it always is for KAP)
        // to extend the first dimension, the argument have to be -0.5. That's incredibly ugly.

        return if (axis == null) {
            joinNoAxis(a, b)
        } else {
            val (isLaminate, newAxis) = computeLaminateAxis(axis.ensureNumber(pos))
            if (isLaminate) {
                joinByLaminate(a, b, newAxis)
            } else {
                joinByAxis(a, b, newAxis)
            }
        }
    }

    private fun computeLaminateAxis(axis: APLNumber): Pair<Boolean, Int> {
        if (axis is APLLong) {
            return Pair(false, axis.asInt())
        }
        val d = axis.asDouble()
        if (d.rem(1).absoluteValue < 0.01) {
            return Pair(false, d.toInt())
        }
        return Pair(true, ceil(d).toInt())
    }

    private fun joinByLaminate(a: APLValue, b: APLValue, axis: Int): APLValue {
        val aIsScalar = a.isScalar()
        val bIsScalar = b.isScalar()
        if (aIsScalar && bIsScalar) {
            throwAPLException(IllegalAxisException("Both arguments are scalar", pos))
        }
        val a1 = if (aIsScalar) {
            ResizedArrayImpls.makeResizedArray(b.dimensions, a)
        } else {
            a
        }
        val b1 = if (bIsScalar) {
            ResizedArrayImpls.makeResizedArray(a.dimensions, b)
        } else {
            b
        }
        if (a1.rank != b1.rank) {
            throwAPLException(InvalidDimensionsException("Ranks of A and B are different", pos))
        }
        val aDimensions = a1.dimensions
        if (axis < 0 || axis > aDimensions.size) {
            throwAPLException(
                IllegalAxisException(
                    "Axis must be between 0 and ${aDimensions.size} inclusive. Found: ${axis}",
                    pos))
        }
        val bDimensions = b1.dimensions
        if (!aDimensions.compareEquals(bDimensions)) {
            throwAPLException(InvalidDimensionsException(aDimensions, bDimensions, pos))
        }
        val rd = aDimensions.insert(axis, 1)
        val a2 = ResizedArrayImpls.makeResizedArray(rd, a1)
        val b2 = ResizedArrayImpls.makeResizedArray(rd, b1)
        return joinByAxis(a2, b2, axis)
    }

    private fun joinNoAxis(a: APLValue, b: APLValue): APLValue {
        val aDimensions = a.dimensions
        val bDimensions = b.dimensions
        return when {
            aDimensions.isEmpty() && bDimensions.isEmpty() -> APLArrayImpl.make(dimensionsOfSize(2)) { i -> if (i == 0) a.disclose() else b.disclose() }
            aDimensions.size <= 1 && bDimensions.size <= 1 -> Concatenated1DArrays(a.arrayify(), b.arrayify())
            else -> joinByAxis(a, b, defaultAxis(aDimensions, bDimensions))
        }
    }

    abstract fun defaultAxis(aDimensions: Dimensions, bDimensions: Dimensions): Int

    private fun joinByAxis(a: APLValue, b: APLValue, axis: Int): APLValue {
        if (axis < 0) {
            throwAPLException(IllegalAxisException("Axis is negative", pos))
        }

        if (a.rank == 0 && b.rank == 0) {
            throwAPLException(InvalidDimensionsException("Both a and b are scalar", pos))
        }

        val a1 = if (a.rank == 0) {
            val bDimensions = b.dimensions
            ConstantArray(
                Dimensions(IntArray(bDimensions.size) { index -> if (index == axis) 1 else bDimensions[index] }),
                a.disclose())
        } else {
            a
        }

        val b1 = if (b.rank == 0) {
            val aDimensions = a.dimensions
            ConstantArray(
                Dimensions(IntArray(aDimensions.size) { index -> if (index == axis) 1 else aDimensions[index] }),
                b.disclose())
        } else {
            b
        }

        val a2 = if (b1.rank - a1.rank == 1) {
            // Reshape a1, inserting a new dimension at the position of the axis
            ResizedArrayImpls.makeResizedArray(a1.dimensions.insert(axis, 1), a1)
        } else {
            a1
        }

        val b2 = if (a1.rank - b1.rank == 1) {
            ResizedArrayImpls.makeResizedArray(b1.dimensions.insert(axis, 1), b1)
        } else {
            b1
        }

        val da = a2.dimensions
        val db = b2.dimensions

        if (da.size != db.size) {
            throwAPLException(InvalidDimensionsException("Different ranks: ${da.size} compared to ${db.size}", pos))
        }

        for (i in da.indices) {
            if (i != axis && da[i] != db[i]) {
                throwAPLException(
                    InvalidDimensionsException(
                        "Dimensions at axis $axis does not match: $da compared to $db",
                        pos))
            }
        }

        if (a2.size == 0) {
            return b2
        }

        if (b2.size == 0) {
            return a2
        }

        if (axis < 0 || axis >= da.size) {
            throwAPLException(IllegalAxisException(axis, da, pos))
        }

        if (da.size == 1 && db.size == 1) {
            if (axis != 0) {
                throwAPLException(IllegalAxisException(axis, emptyDimensions(), pos))
            }
            return Concatenated1DArrays(a2, b2)
        }

        return ConcatenatedMultiDimensionalArrays(a2, b2, axis)
    }

    override fun identityValue() = APLNullValue.APL_NULL_INSTANCE

    override fun reduce(
        context: RuntimeContext,
        arg: APLValue,
        sizeAlongAxis: Int,
        stepLength: Int,
        offset: Int,
        savedStack: StorageStack.StorageStackFrame?,
        functionAxis: APLValue?
    ): APLValue {
        return when (sizeAlongAxis) {
            0 -> APLNullValue.APL_NULL_INSTANCE
            1 -> arg.valueAt(offset)
            else -> loopedReduce(context, arg, sizeAlongAxis, stepLength, offset, savedStack, functionAxis)
        }
    }

    private fun loopedReduce(
        context: RuntimeContext,
        arg: APLValue,
        sizeAlongAxis: Int,
        stepLength: Int,
        offset: Int,
        savedStack: StorageStack.StorageStackFrame?,
        functionAxis: APLValue?
    ): APLValue {
        val a = arg.valueAt(offset)
        val b = arg.valueAt(offset + stepLength)
        val aDimensions = a.dimensions
        val bDimensions = b.dimensions
        val axis = when {
            functionAxis != null -> functionAxis.ensureNumber(pos).asInt(pos)
            aDimensions.isEmpty() && bDimensions.isEmpty() -> 0
            else -> defaultAxis(aDimensions, bDimensions)
        }

//        val ad0: Dimensions
//        val bd0: Dimensions
//        when {
//            aDimensions.size == bDimensions.size - 1 -> run { ad0 = aDimensions.insert(axis, 1); bd0 = bDimensions }
//            bDimensions.size == aDimensions.size - 1 -> run { ad0 = aDimensions; bd0 = bDimensions.insert(axis, 1) }
//            else -> throwAPLException(InvalidDimensionsException(aDimensions, bDimensions, pos))
//        }

        // If the first element is undersized, conform the dimensions
        val a0: APLValue
        when {
            aDimensions.size == 0 -> {
                val resultDimensions = if (bDimensions.size == 0) dimensionsOfSize(1) else bDimensions.replace(axis, 1)
                a0 = ResizedArrayImpls.makeResizedArray(resultDimensions, a)
            }
            aDimensions.size == bDimensions.size - 1 -> {
                val resultDimensions = aDimensions.insert(axis, 1)
                a0 = ResizedArrayImpls.makeResizedArray(resultDimensions, a)
            }
            else -> {
                a0 = a
            }
        }

        val a0Dimensions = a0.dimensions

        val transposeAxis = if (axis == 0) {
            null
        } else {
            IntArray(a0Dimensions.size) { i ->
                when {
                    i == 0 -> axis
                    i <= axis -> i - 1
                    i > axis -> i
                    else -> throw IllegalStateException("Unexpected value from intarray initialiser")
                }
            }
        }

        val resultDimensions = Dimensions(IntArray(a0Dimensions.size) { i ->
            when {
                i == 0 -> 1
                transposeAxis == null -> a0Dimensions[i]
                else -> a0Dimensions[transposeAxis[i]]
            }
        })

        //              |
        //  0   1   2   3   4   5   6
        //  3   0   1   2   4   5   6
        val result = ArrayList<APLValue>()

        var p = offset
        var i = 0
        var activeElement: APLValue? = null
        while (i < sizeAlongAxis) {
            val v = when (i) {
                0 -> a0
                1 -> b
                else -> arg.valueAt(p).also { activeElement = it }
            }
            val vDimensions = v.dimensions
            if (vDimensions.size == 0) {
                val disclosed = v.disclose()
                repeat(resultDimensions.contentSize()) {
                    result.add(disclosed)
                }
            } else {
                val v0 = when (vDimensions.size) {
                    resultDimensions.size -> v
                    aDimensions.size - 1 -> ResizedArrayImpls.makeResizedArray(resultDimensions, v)
                    else -> break
                }
                val v1 = if (transposeAxis == null) v0 else TransposedAPLValue(transposeAxis, v0, pos)

                val v1Dimensions = v1.dimensions
                for (i1 in 1 until v1Dimensions.size) {
                    if (v1Dimensions[i1] != resultDimensions[i1]) {
                        break
                    }
                }

                v1.iterateMembers { innerElement ->
                    result.add(innerElement)
                }
            }
            i++
            p += stepLength
        }

        val resultVal = APLArrayList(resultDimensions.replace(0, result.size / resultDimensions.contentSize()), result)

        val fixedRes = if (transposeAxis == null) {
            resultVal
        } else {
            val inverseTransposedAxis = TransposedAPLValue.makeInverseTransposeIndex(transposeAxis)
            assertx(inverseTransposedAxis != null)
            TransposedAPLValue(inverseTransposedAxis, resultVal, pos)
        }

        return if (i < sizeAlongAxis) {
            // At this point, we've already completed part of the reduction, so we construct a result
            // array for the completed part and then iterate over the remaining elements.
            val firstElement = activeElement
            assertx(firstElement != null)
            var curr: APLValue = eval2Arg(context, fixedRes, firstElement, functionAxis).collapse()
            i++
            p += stepLength
            while (i < sizeAlongAxis) {
                curr = eval2Arg(context, curr, arg.valueAt(p), functionAxis).collapse()
                i++
                p += stepLength
            }
            curr
        } else {
            fixedRes
        }
    }

    // This is an inner class since it's highly dependent on invariants that are established in the parent class
    class ConcatenatedMultiDimensionalArrays(val a: APLValue, val b: APLValue, val axis: Int) : APLArray() {
        override val dimensions: Dimensions
        private val axisA: Int
        private val multiplierAxis: Int
        private val highValFactor: Int
        private val aMultiplierAxis: Int
        private val bMultiplierAxis: Int
        private val aDimensionAxis: Int
        private val bDimensionAxis: Int

        override val labels by lazy { resolveLabels() }

        init {
            val ad = a.dimensions
            val bd = b.dimensions

            axisA = ad[axis]

            dimensions = Dimensions(IntArray(ad.size) { i -> if (i == axis) ad[i] + bd[i] else ad[i] })
            val multipliers = dimensions.multipliers()
            val aMultipliers = ad.multipliers()
            val bMultipliers = bd.multipliers()
            multiplierAxis = multipliers[axis]
            highValFactor = multiplierAxis * dimensions[axis]
            aMultiplierAxis = aMultipliers[axis]
            bMultiplierAxis = bMultipliers[axis]
            aDimensionAxis = a.dimensions[axis]
            bDimensionAxis = b.dimensions[axis]
        }

        override fun valueAt(p: Int): APLValue {
            val highVal = p / highValFactor
            val lowVal = p % multiplierAxis
            val axisCoord = (p % highValFactor) / multiplierAxis
            return if (axisCoord < axisA) {
                a.valueAt((highVal * aMultiplierAxis * aDimensionAxis) + (axisCoord * aMultiplierAxis) + lowVal)
            } else {
                b.valueAt((highVal * bMultiplierAxis * bDimensionAxis) + ((axisCoord - axisA) * bMultiplierAxis) + lowVal)
            }
        }

        private fun resolveLabels(): DimensionLabels? {
            val aLabels = a.labels
            val bLabels = b.labels
            if (aLabels == null && bLabels == null) {
                return null
            }
            val axisLabelsA = aLabels?.run { labels[axis] }
            val axisLabelsB = bLabels?.run { labels[axis] }
            val axisLabels = ArrayList<AxisLabel?>()
            val aSize = a.dimensions[axis]
            val bSize = b.dimensions[axis]
            var hasLabels = false
            val d = dimensions
            if (axisLabelsA == null) {
                repeat(aSize) {
                    axisLabels.add(null)
                }
            } else {
                axisLabelsA.forEach { label ->
                    axisLabels.add(label)
                    hasLabels = true
                }
            }
            if (axisLabelsB == null) {
                repeat(bSize) {
                    axisLabels.add(null)
                }
            } else {
                axisLabelsB.forEach { label ->
                    axisLabels.add(label)
                    hasLabels = true
                }
            }

            if (!hasLabels) {
                return null
            }

            val newLabels = ArrayList<List<AxisLabel?>?>()
            repeat(d.size) { i ->
                newLabels.add(if (i == axis) axisLabels else null)
            }
            return DimensionLabels(newLabels)
        }
    }
}

class ConcatenateAPLFunctionFirstAxis : APLFunctionDescriptor {
    class ConcatenateAPLFunctionFirstAxisImpl(pos: FunctionInstantiation) : ConcatenateAPLFunctionImpl(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            val aDimensions = a.dimensions
            val c = if (aDimensions.size == 0) {
                dimensionsOfSize(1, 1)
            } else {
                dimensionsOfSize(
                    aDimensions[0],
                    aDimensions.dimensions.drop(1).reduceWithInitial(1) { v1, v2 -> v1 * v2 })
            }
            return ResizedArrayImpls.makeResizedArray(c, a)
        }

        override fun defaultAxis(aDimensions: Dimensions, bDimensions: Dimensions) = 0

        override val name1Arg get() = "table"
        override val name2Arg get() = "concatenate first axis"
    }

    override fun make(instantiation: FunctionInstantiation) = ConcatenateAPLFunctionFirstAxisImpl(instantiation)
}

class ConcatenateAPLFunctionLastAxis : APLFunctionDescriptor {
    class ConcatenateAPLFunctionLastAxisImpl(pos: FunctionInstantiation) : ConcatenateAPLFunctionImpl(pos) {
        private class DelegatedAPLArrayValue(override val dimensions: Dimensions, val value: APLValue) : APLArray() {
            override fun valueAt(p: Int): APLValue {
                return value.valueAt(0)
            }
        }

        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return when {
                a is APLSingleValue -> APLArrayImpl.make(dimensionsOfSize(1)) { a }
                a.rank == 0 -> DelegatedAPLArrayValue(dimensionsOfSize(1), a)
                else -> ResizedArrayImpls.makeResizedArray(dimensionsOfSize(a.size), a)
            }
        }

        override fun defaultAxis(aDimensions: Dimensions, bDimensions: Dimensions) = max(aDimensions.size, bDimensions.size) - 1

        override val name1Arg get() = "ravel"
        override val name2Arg get() = "concatenate last axis"
    }

    override fun make(instantiation: FunctionInstantiation) = ConcatenateAPLFunctionLastAxisImpl(instantiation)
}
