package array.builtins

import array.*
import kotlin.math.max

class AxisEnclosedValue(val value: APLValue, axis: Int) : APLArray() {
    override val dimensions: Dimensions

    private val stepLength: Int
    private val sizeAlongAxis: Int
    private val fromSourceMul: Int
    private val toDestMul: Int

    init {
        val aDimensions = value.dimensions

        val argMultipliers = aDimensions.multipliers()

        stepLength = argMultipliers[axis]
        sizeAlongAxis = aDimensions[axis]
        dimensions = aDimensions.remove(axis)

        val multipliers = dimensions.multipliers()

        fromSourceMul = if (axis == 0) dimensions.contentSize() else multipliers[axis - 1]
        toDestMul = fromSourceMul * aDimensions[axis]
    }

    override fun valueAt(p: Int): APLValue {
        return if (sizeAlongAxis == 0) {
            APLLONG_0
        } else {
            val highPosition = p / fromSourceMul
            val lowPosition = p % fromSourceMul
            val posInSrc = highPosition * toDestMul + lowPosition

            APLArrayImpl(dimensionsOfSize(sizeAlongAxis), Array(sizeAlongAxis) { i ->
                value.valueAt(i * stepLength + posInSrc)
            })
        }
    }
}

class AxisMultiDimensionEnclosedValue(val value: APLValue, numDimensions: Int) : APLArray() {
    override val dimensions: Dimensions

    private val aDimensions = value.dimensions
    private val multipliers: IntArray
    private val highValFactor: Int
    private val lowDimensions: Dimensions

    init {
        dimensions = Dimensions(IntArray(aDimensions.size - numDimensions) { i -> aDimensions[i] })
        multipliers = aDimensions.multipliers()
        highValFactor = if (numDimensions == aDimensions.size) {
            aDimensions.contentSize()
        } else {
            multipliers[aDimensions.size - numDimensions - 1]
        }
        lowDimensions = Dimensions(IntArray(numDimensions) { i -> aDimensions[aDimensions.size - numDimensions + i] })
    }

    override fun valueAt(p: Int): APLValue {
        return if (highValFactor == 1) {
            val v = value.valueAt(p).unwrapDeferredValue()
            EnclosedAPLValue.make(v)
        } else {
            val high = p * highValFactor
            APLArrayImpl(lowDimensions, Array(lowDimensions.contentSize()) { i ->
                value.valueAt(high + i)
            })
        }
    }
}

class PartitionedValue(val b: APLValue, val axis: Int, val partitionIndexes: List<Int>) : APLArray() {
    override val dimensions: Dimensions
    private val multipliers: IntArray
    private val bDimensions: Dimensions
    private val bMult: IntArray

    init {
        bDimensions = b.dimensions
        dimensions = Dimensions(IntArray(bDimensions.size) { i ->
            if (i == axis) {
                partitionIndexes.size / 2
            } else {
                bDimensions[i]
            }
        })
        multipliers = dimensions.multipliers()
        bMult = bDimensions.multipliers()
    }

    override fun valueAt(p: Int): APLValue {
        val indexes = Dimensions.positionFromIndexWithMultipliers(p, multipliers)
        val start = partitionIndexes[indexes[axis] * 2]
        val end = partitionIndexes[indexes[axis] * 2 + 1]
        return APLArrayImpl(dimensionsOfSize(end - start), Array(end - start) { i ->
            val newPosition = IntArray(bDimensions.size) { bIndex ->
                if (bIndex == axis) {
                    start + i
                } else {
                    indexes[bIndex]
                }
            }
            b.valueAt(bDimensions.indexFromPosition(newPosition, bMult))
        })
    }
}

class EncloseAPLFunction : APLFunctionDescriptor {
    class EncloseAPLFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            val v = a.unwrapDeferredValue()
            return if (axis == null) {
                EnclosedAPLValue.make(v)
            } else {
                val axis0 = axis.arrayify()
                when {
                    axis0.dimensions.size != 1 -> {
                        throwAPLException(APLIllegalArgumentException("Illegal dimensions of axis argument", pos))
                    }
                    axis0.dimensions[0] == 1 -> {
                        val axisInt = axis0.valueAtInt(0, pos)
                        ensureValidAxis(axisInt, v.dimensions, pos)
                        AxisEnclosedValue(v, axisInt)
                    }
                    axis0.dimensions[0] > 1 -> {
                        val axisIntArray = axis0.toIntArray(pos)
                        // Check for the base case
                        if (isAxisOrdered(a, axisIntArray)) {
                            AxisMultiDimensionEnclosedValue(a, axisIntArray.size)
                        } else {
                            val orig = IntArray(a.rank) { it }.toMutableList()
                            axisIntArray.forEach { selectedAxis ->
                                val index = orig.indexOf(selectedAxis)
                                if (index == -1) {
                                    throwAPLException(APLIllegalArgumentException("Invalid axis argument", pos))
                                } else {
                                    orig.removeAt(index)
                                }
                            }
                            val transposeAxis = IntArray(orig.size + axisIntArray.size) { i ->
                                val indexFromOrig = orig.indexOf(i)
                                if (indexFromOrig == -1) {
                                    val indexFromAxisInt = axisIntArray.indexOf(i)
                                    if (indexFromAxisInt == -1) {
                                        throw IllegalStateException("Error when creating lookup array: index: ${i}, orig = ${orig.joinToString()}, axisIntArray = ${axisIntArray.joinToString()}}")
                                    } else {
                                        indexFromAxisInt + orig.size
                                    }
                                } else {
                                    indexFromOrig
                                }
                            }
                            val transposedArgument = TransposedAPLValue(transposeAxis, a, pos)
                            AxisMultiDimensionEnclosedValue(transposedArgument, axisIntArray.size)
                        }
                    }
                    else -> throwAPLException(APLIllegalArgumentException("Empty array in axis argument", pos))
                }
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val axisInt = computeAxis(b, axis, pos)
            val aDimensions = a.dimensions
            val partitionArgs = when (aDimensions.size) {
                0 -> intArrayOf(a.ensureNumber(pos).asInt(pos))
                1 -> a.toIntArray(pos)
                else -> throw APLIllegalArgumentException("Left argument to partition must be a scalar or a one-dimensional array")
            }
            if (partitionArgs.size != b.dimensions[axisInt]) {
                throw InvalidDimensionsException(
                    "Size of A must be the same size as the dimension of B along the selected axis (size of A: ${partitionArgs.size}, size of axis in B: ${b.dimensions[axisInt]})",
                    pos)
            }
            val partitionIndexes = computePartitionIndexes(partitionArgs)
            return PartitionedValue(b, axisInt, partitionIndexes)
        }

        private fun computePartitionIndexes(partitionArgs: IntArray): List<Int> {
            val result = ArrayList<Int>()
            var prevIndex = -1
            for (i in partitionArgs.indices) {
                val curr = partitionArgs[i]
                if (prevIndex >= 0 && curr == 0) {
                    result.add(prevIndex)
                    result.add(i)
                    prevIndex = -1
                } else if (i == 0 || (partitionArgs[i - 1] < curr && curr != 0)) {
                    if (prevIndex >= 0) {
                        result.add(prevIndex)
                        result.add(i)
                    }
                    prevIndex = if (curr == 0) -1 else i
                }
            }
            if (prevIndex >= 0) {
                result.add(prevIndex)
                result.add(partitionArgs.size)
            }
            return result
        }

        private fun isAxisOrdered(a: APLValue, axis: IntArray): Boolean {
            val axisDiff = (a.rank - axis.size)
            for (i in axis.indices) {
                if (axis[i] != i + axisDiff) {
                    return false
                }
            }
            return true
        }

        override val name1Arg get() = "enclose"
    }

    override fun make(pos: Position) = EncloseAPLFunctionImpl(pos)
}

class DisclosedArrayValue(value: APLValue) : APLArray() {
    private val valueInt = value.collapseFirstLevel()
    override val dimensions: Dimensions

    private val cutoffMultiplier: Int
    private val newDimensionsMultipliers: IntArray

    init {
        val d = valueInt.dimensions
        assertx(d.size > 0)

        val m = maxShapeOf(valueInt)
        val resultDimension = Dimensions(IntArray(d.size + m.size) { i ->
            if (i < d.size) {
                d[i]
            } else {
                m[i - d.size]
            }
        })

        val multipliers = resultDimension.multipliers()
        dimensions = resultDimension
        cutoffMultiplier = multipliers[d.size - 1]
        newDimensionsMultipliers = m.multipliers()
    }

    override fun valueAt(p: Int): APLValue {
        val index = p / cutoffMultiplier
        val v = valueInt.valueAt(index)

        val innerIndex = p % cutoffMultiplier
        val d = v.dimensions
        return if (innerIndex == 0) {
            when {
                d.size == 0 -> v.disclose()
                d.contentSize() == 0 -> v.defaultValue()
                else -> v.valueAt(0)
            }
        } else if (d.size == 0) {
            v.defaultValue()
        } else {
            val position = Dimensions.positionFromIndexWithMultipliers(innerIndex, newDimensionsMultipliers)
            val n = position.size - d.size
            for (i in position.indices) {
                val size = if (i < n) 1 else d[i - n]
                if (position[i] >= size) {
                    return v.defaultValue()
                }
            }
            val updatedPosition = if (position.size == d.size) {
                position
            } else {
                IntArray(d.size) { i -> position[i + n] }
            }
            return v.valueAt(d.indexFromPosition(updatedPosition))
        }
    }

    private fun maxShapeOf(v: APLValue): Dimensions {
        if (v.dimensions.contentSize() == 0) {
            return emptyDimensions()
        }
        var elements: IntArray? = null // null actually means empty dimensions
        v.iterateMembers { value ->
            val dimensions = value.dimensions
            when {
                elements == null -> {
                    elements = IntArray(dimensions.size) { i -> dimensions[i] }
                }
                elements!!.size < dimensions.size -> {
                    elements = IntArray(dimensions.size) { i ->
                        val n = dimensions.size - elements!!.size
                        if (i < n) dimensions[i] else max(elements!![i - n], dimensions[i])
                    }
                }
                else -> {
                    val n = elements!!.size - dimensions.size
                    for (i in n until elements!!.size) {
                        val size = dimensions[i - n]
                        if (elements!![i] < size) {
                            elements!![i] = size
                        }
                    }
                }
            }

        }
        return if (elements != null) Dimensions(elements!!) else emptyDimensions()
    }
}

class DiscloseAPLFunction : APLFunctionDescriptor {
    class DiscloseAPLFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            val v = a.unwrapDeferredValue()
            return when {
                v.isScalar() -> processScalarValue(a, axis)
                axis == null -> DisclosedArrayValue(v)
                else -> processAxis(v, a, axis)
            }
        }

        private fun processScalarValue(a: APLValue, axis: APLValue?): APLValue {
            if (axis != null) {
                val axisInt = makeAxisIntArray(axis, 1)
                if (axisInt[0] != 0) {
                    throwAPLException(IllegalAxisException("Only axis 0 is allowed for scalars", pos))
                }
            }
            return if (a is APLSingleValue) a else a.valueAt(0)
        }

        private fun processAxis(v: APLValue, a: APLValue, axis: APLValue): TransposedAPLValue {
            val z1 = DisclosedArrayValue(v)
            val z1Dimensions = z1.dimensions
            val maxAxis = z1Dimensions.size - a.dimensions.size
            val axisInt = makeAxisIntArray(axis, maxAxis)
            axisInt.forEach { v0 ->
                ensureValidAxis(v0, z1Dimensions, pos)
            }
            if (axisInt.distinct().count() != axisInt.size) {
                throwAPLException(APLIllegalArgumentException("Duplicated values in axis", pos))
            }
            val newAxisList = ArrayList<Int>()
            repeat(z1Dimensions.size) { i ->
                if (axisInt.indexOf(i) == -1) {
                    newAxisList.add(i)
                }
            }
            axisInt.forEach { v0 ->
                newAxisList.add(v0)
            }
            assertx(newAxisList.size == z1Dimensions.size)
            return TransposedAPLValue(newAxisList.toIntArray(), z1, pos)
        }

        private fun makeAxisIntArray(axis: APLValue, maxAxis: Int): IntArray {
            val axisArray = axis.arrayify()
            if (axisArray.dimensions.size != 1) {
                throwAPLException(APLIllegalArgumentException("Axis specifier must be a scalar or a rank-1 array", pos))
            }
            val v0 = axisArray.toIntArray(pos)
            return when {
                v0.size == maxAxis -> v0
                v0.size < maxAxis -> IntArray(maxAxis) { i -> if (i < v0.size) v0[i] else v0[v0.size - 1] + i - v0.size + 1 }
                else -> throwAPLException(
                    IllegalAxisException(
                        "Too many axis specifiers. Max allowed: ${maxAxis}. Got ${v0.size}.",
                        pos))
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            if (axis != null) {
                throwAPLException(AxisNotSupported(pos))
            }
            if (a.dimensions.size !in 0..1) {
                throwAPLException(InvalidDimensionsException("Left argument to pick should be rank 0 or 1", pos))
            }
            var curr = b
            a.arrayify().iterateMembers { v ->
                val d = v.dimensions
                val index = when (d.size) {
                    0 -> {
                        if (curr.dimensions.size != 1) {
                            throwAPLException(InvalidDimensionsException("Mismatched dimensions for selection", pos))
                        }
                        v.ensureNumber(pos).asInt(pos)
                    }
                    1 -> curr.dimensions.indexFromPosition(v.toIntArray(pos), pos = pos)
                    else -> throwAPLException(InvalidDimensionsException("Selection should be rank 0 or 1", pos))
                }
                if (index !in (0 until curr.size)) {
                    throwAPLException(APLIndexOutOfBoundsException("Selection index out of bounds", pos))
                }
                curr = curr.valueAt(index)
            }
            return curr
        }

        override val name1Arg get() = "disclose"
        override val name2Arg get() = "pick"
    }

    override fun make(pos: Position) = DiscloseAPLFunctionImpl(pos)

    companion object {
        fun discloseValue(value: APLValue): APLValue {
            val v = value.unwrapDeferredValue()
            return when {
                v is APLSingleValue -> v
                v.isScalar() -> v.valueAt(0)
                else -> DisclosedArrayValue(v)
            }

        }
    }
}

class PartitionedEncloseFunction : APLFunctionDescriptor {
    class PartitionedEncloseFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val axisInt = computeAxis(b, axis, pos)
            val aDimensions = a.dimensions
            val partitionArgs = when (aDimensions.size) {
                0 -> intArrayOf(a.ensureNumber(pos).asInt(pos))
                1 -> a.toIntArray(pos)
                else -> throw APLIllegalArgumentException("Left argument to partition must be a scalar or a one-dimensional array")
            }
            if (partitionArgs.size != b.dimensions[axisInt]) {
                throw InvalidDimensionsException(
                    "Size of A must be the same size as the dimension of B along the selected axis (size of A: ${partitionArgs.size}, size of axis in B: ${b.dimensions[axisInt]})",
                    pos)
            }
            val partitionIndexes = computePartitionIndexes(partitionArgs)
            return PartitionedValue(b, axisInt, partitionIndexes)
        }

        private fun computePartitionIndexes(partitionArgs: IntArray): List<Int> {
            val result = ArrayList<Int>()
            var currStart = 0

            fun collectPartition(i: Int) {
                result.add(currStart)
                result.add(i)
                currStart = i
            }

            partitionArgs.forEachIndexed { i, partitionIndicator ->
                if (partitionIndicator > 0 && i > 0) {
                    collectPartition(i)
                }
                if (partitionIndicator > 1) {
                    repeat(partitionIndicator - 1) {
                        result.add(i)
                        result.add(i)
                    }
                }
            }
            collectPartition(partitionArgs.size)
            return result
        }

        override val name2Arg get() = "partitioned enclose"
    }

    override fun make(pos: Position) = PartitionedEncloseFunctionImpl(pos)
}

private fun computeAxis(b: APLValue, axis: APLValue?, pos: Position? = null): Int {
    val bDimensions = b.dimensions
    val axisInt = if (axis == null) {
        bDimensions.lastAxis()
    } else {
        axis.ensureNumber(pos).asInt(pos)
    }
    if (axisInt < 0 || axisInt >= bDimensions.size) {
        throw IllegalAxisException(axisInt, bDimensions, pos)
    }
    return axisInt
}
