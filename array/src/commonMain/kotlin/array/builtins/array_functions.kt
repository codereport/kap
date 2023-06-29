package array.builtins

import array.*
import kotlin.math.absoluteValue
import kotlin.math.max

class AxisActionFactors(val dimensions: Dimensions, axis: Int) {
    val multipliers = dimensions.multipliers()
    val multiplierAxis = multipliers[axis]
    val highValFactor = multiplierAxis * dimensions[axis]

    inline fun <T> withFactors(p: Int, fn: (high: Int, low: Int, axisCoord: Int) -> T): T {
        val highVal = p / highValFactor
        val lowVal = p % multiplierAxis
        val axisCoord = (p % highValFactor) / multiplierAxis
        return fn(highVal, lowVal, axisCoord)
    }

    fun indexForAxis(high: Int, low: Int, axisPosition: Int): Int {
        return (highValFactor * high) + (axisPosition * multiplierAxis) + low
    }
}

object IotaArrayImpls {
    interface GenericIotaArrayLong {
        fun resizeIotaArray(d: Dimensions, updatedOffset: Long): APLValue
    }

    class IotaArray(val indexes: IntArray) : APLArray() {
        override val dimensions = Dimensions(indexes)

        private val multipliers = dimensions.multipliers()

        init {
            assertx(indexes.isNotEmpty()) { "indexes is empty" }
        }

        override fun valueAt(p: Int): APLValue {
            val index = multipliers.positionFromIndex(p)
            return APLArrayLong(dimensionsOfSize(indexes.size), LongArray(index.size) { i -> index[i].toLong() })
        }
    }

    class IotaArrayLong(val length: Int) : APLArray(), GenericIotaArrayLong {
        override val dimensions = dimensionsOfSize(length)
        override val specialisedType get() = ArrayMemberType.LONG
        override fun collapseInt() = this

        override fun valueAtInt(p: Int, pos: Position?): Int {
            if (p < 0 || p >= length) {
                throwAPLException(APLIndexOutOfBoundsException("Position in array: ${p}, size: ${length}", pos))
            }
            return p
        }

        override fun valueAtLong(p: Int, pos: Position?): Long {
            return valueAtInt(p, pos).toLong()
        }

        override fun valueAt(p: Int): APLValue {
            return valueAtLong(p, null).makeAPLNumber()
        }

        override fun resizeIotaArray(d: Dimensions, updatedOffset: Long): APLValue {
            return ResizedIotaArrayLong(d, length, updatedOffset)
        }
    }

    class ResizedIotaArrayLong(
        override val dimensions: Dimensions,
        val width: Int,
        val offset: Long
    ) : APLArray(), GenericIotaArrayLong {
        val length = dimensions.contentSize()
        override val specialisedType get() = ArrayMemberType.LONG
        override fun collapseInt() = this

        override fun valueAtLong(p: Int, pos: Position?): Long {
            return when {
                p < 0 || p >= length -> throwAPLException(APLIndexOutOfBoundsException("Position in array: ${p}, size: ${length}", pos))
                p < width -> p + offset
                else -> (p % width) + offset
            }
        }

        override fun valueAt(p: Int): APLValue {
            return valueAtLong(p, null).makeAPLNumber()
        }

        override fun resizeIotaArray(d: Dimensions, updatedOffset: Long): APLValue {
            return if (d.contentSize() == width) {
                ResizedIotaArrayLong(d, width, offset + updatedOffset)
            } else {
                ResizedArrayImpls.GenericResizedArray(d, this)
            }
        }
    }
}

class FindIndexArray(val a: APLValue, val b: APLValue, val context: RuntimeContext) : APLArray() {
    override val dimensions = b.dimensions

    override fun valueAt(p: Int): APLValue {
        val reference = b.valueAt(p)
        return findFromRef(reference)
    }

    private fun findFromRef(reference: APLValue): APLLong {
        val refColl = reference.collapse()
        val elementCount = a.size
        for (i in 0 until elementCount) {
            val v = a.valueAt(i)
            if (v.compareEquals(refColl)) {
                return i.makeAPLNumber()
            }
        }
        return elementCount.makeAPLNumber()
    }

    override fun unwrapDeferredValue(): APLValue {
        return if (dimensions.isEmpty()) {
            findFromRef(b.disclose())
        } else {
            this
        }
    }
}

class IotaAPLFunction : APLFunctionDescriptor {
    class IotaAPLFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val aDimensions = a.dimensions
            return when (aDimensions.size) {
                0 -> IotaArrayImpls.IotaArrayLong(a.ensureNumber(pos).asInt(pos))
                1 -> if (aDimensions[0] == 0) {
                    EnclosedAPLValue.make(APLNullValue.APL_NULL_INSTANCE)
                } else {
                    IotaArrayImpls.IotaArray(IntArray(aDimensions[0]) { i -> a.valueAtInt(i, pos) })
                }

                else -> throwAPLException(InvalidDimensionsException("Right argument must be rank 0 or 1", pos))
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            if (a.rank > 1) {
                throwAPLException(InvalidDimensionsException("Left argument must be rank 0 or 1", pos))
            }
            return FindIndexArray(a.arrayify(), b, context)
        }

        override val name1Arg get() = "iota"
        override val name2Arg get() = "index"
    }

    override fun make(instantiation: FunctionInstantiation) = IotaAPLFunctionImpl(instantiation)
}

object ResizedArrayImpls {
    class GenericResizedArray(override val dimensions: Dimensions, val value: APLValue) : APLArray() {
        override val specialisedType = value.specialisedType

        override fun valueAt(p: Int): APLValue {
            val s = value.size
            val p0 = if (p < s) p else p % s
            return value.valueAt(p0)
        }

        override fun valueAtInt(p: Int, pos: Position?): Int {
            val s = value.size
            val p0 = if (p < s) p else p % s
            return value.valueAtInt(p0, pos)
        }

        override fun valueAtLong(p: Int, pos: Position?): Long {
            val s = value.size
            val p0 = if (p < s) p else p % s
            return value.valueAtLong(p0, pos)
        }

        override fun valueAtDouble(p: Int, pos: Position?): Double {
            val s = value.size
            val p0 = if (p < s) p else p % s
            return value.valueAtDouble(p0, pos)
        }
    }

    class ResizedSingleValueGeneric(override val dimensions: Dimensions, private val value: APLValue) : APLArray() {
        override val specialisedType get() = ArrayMemberType.GENERIC
        override fun valueAt(p: Int) = value
    }

    class ResizedArrayLong(override val dimensions: Dimensions, private val boxed: APLLong) : APLArray() {
        override val specialisedType get() = ArrayMemberType.LONG
        override fun valueAt(p: Int) = boxed
        override fun valueAtLong(p: Int, pos: Position?) = boxed.value
    }

    class ResizedArrayDouble(override val dimensions: Dimensions, private val boxed: APLDouble) : APLArray() {
        override val specialisedType get() = ArrayMemberType.DOUBLE
        override fun valueAt(p: Int) = boxed
        override fun valueAtDouble(p: Int, pos: Position?) = boxed.value
    }

    private fun findUnderlyingArray(a: APLValue, requestedSize: Int): APLValue {
        var curr = a.unwrapDeferredValue()
        // We can only optimise resized arrays whose size is identical to the new size.
        // If array sizes are different, the resulting array has modular indexes which
        // would not be preserved.
        while (curr is GenericResizedArray && curr.dimensions.contentSize() == requestedSize) {
            curr = curr.value
        }
        return curr
    }

    fun makeResizedArray(dimensions: Dimensions, value: APLValue): APLValue {
        assertx(dimensions.size >= 1)
        val v0 = findUnderlyingArray(value, dimensions.contentSize())
        return when {
            dimensions.compareEquals(v0.dimensions) -> v0
            value is IotaArrayImpls.GenericIotaArrayLong -> value.resizeIotaArray(dimensions, 0)
            v0 is APLLong -> ResizedArrayLong(dimensions, v0)
            v0 is APLDouble -> ResizedArrayDouble(dimensions, v0)
            v0 is APLSingleValue -> ResizedSingleValueGeneric(dimensions, v0)
            dimensions.size == 0 -> ResizedSingleValueGeneric(dimensions, v0.disclose())
            else -> GenericResizedArray(dimensions, v0)
        }
    }
}

class RhoAPLFunction : APLFunctionDescriptor {
    class RhoAPLFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val argDimensions = a.dimensions
            return APLArrayImpl.make(dimensionsOfSize(argDimensions.size)) { argDimensions[it].makeAPLNumber() }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val d = a.dimensions

            if (d.size > 1) {
                throwAPLException(InvalidDimensionsException("Left side of rho must be scalar or a one-dimensional array", pos))
            }

            val v = a.unwrapDeferredValue()
            if (d.size == 1 && d[0] == 0) {
                return EnclosedAPLValue.make(b.arrayify().valueAt(0))
            } else {
                val d1 = if (d.size == 0) {
                    val s = v.ensureNumber(pos).asInt()
                    when {
                        s == -1 -> dimensionsOfSize(b.dimensions.contentSize())
                        s < 0 -> throwAPLException(InvalidDimensionsException("Attempt to reshape to dimension with negative size: ${s}", pos))
                        else -> dimensionsOfSize(s)
                    }
                } else {
                    val dimensionsArray = IntArray(v.size) { v.valueAtInt(it, pos) }
                    var calculatedIndex: Int? = null
                    dimensionsArray.forEachIndexed { i, sizeSpecValue ->
                        when {
                            sizeSpecValue == -1 -> {
                                if (calculatedIndex != null) {
                                    throwAPLException(InvalidDimensionsException("Only one dimension may be set to -1", pos))
                                }
                                calculatedIndex = i
                            }
                            sizeSpecValue < 0 -> {
                                throwAPLException(
                                    InvalidDimensionsException(
                                        "Illegal value at index ${i} in dimensions: ${sizeSpecValue}", pos))
                            }
                        }
                    }
                    val updatedDimensionsArray = calculatedIndex.let { calcPos ->
                        if (calcPos == null) {
                            dimensionsArray
                        } else {
                            val bDimensions = b.dimensions
                            if (bDimensions.size == 0) {
                                throwAPLException(
                                    APLIllegalArgumentException(
                                        "Calculated dimensions can only be used with array arguments",
                                        pos))
                            }
                            val contentSize = bDimensions.contentSize()
                            val total = dimensionsArray.filter { it >= 0 }.reduceWithInitial(1) { o0, o1 -> o0 * o1 }
                            IntArray(v.size) { index ->
                                when {
                                    index != calcPos -> dimensionsArray[index]
                                    total == 0 -> 0
                                    contentSize % total != 0 ->
                                        throwAPLException(
                                            InvalidDimensionsException(
                                                "Invalid size of right argument: ${contentSize}. Should be divisible by ${total}.",
                                                pos))
                                    else -> contentSize / total
                                }
                            }
                        }
                    }
                    Dimensions(updatedDimensionsArray)
                }
                return ResizedArrayImpls.makeResizedArray(d1, b)
            }
        }

        override val name1Arg get() = "shape"
        override val name2Arg get() = "reshape"
    }

    override fun make(instantiation: FunctionInstantiation) = RhoAPLFunctionImpl(instantiation)
}

class IdentityAPLFunction : APLFunctionDescriptor {
    class IdentityAPLFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue) = a
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue) = b

        override fun eval1ArgLong(context: RuntimeContext, a: Long, axis: APLValue?) = a
        override fun eval1ArgDouble(context: RuntimeContext, a: Double, axis: APLValue?) = a
        override fun eval2ArgLongLong(context: RuntimeContext, a: Long, b: Long, axis: APLValue?) = b
        override fun eval2ArgDoubleDouble(context: RuntimeContext, a: Double, b: Double, axis: APLValue?) = b

        override val optimisationFlags: OptimisationFlags
            get() = OptimisationFlags(
                OptimisationFlags.OPTIMISATION_FLAG_1ARG_LONG or
                        OptimisationFlags.OPTIMISATION_FLAG_1ARG_DOUBLE or
                        OptimisationFlags.OPTIMISATION_FLAG_2ARG_LONG_LONG or
                        OptimisationFlags.OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)

        override val name1Arg get() = "identity"
        override val name2Arg get() = "right"
    }

    override fun make(instantiation: FunctionInstantiation) = IdentityAPLFunctionImpl(instantiation)
}

class HideAPLFunction : APLFunctionDescriptor {
    class HideAPLFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue) = a
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue) = a

        override fun eval1ArgLong(context: RuntimeContext, a: Long, axis: APLValue?) = a
        override fun eval1ArgDouble(context: RuntimeContext, a: Double, axis: APLValue?) = a
        override fun eval2ArgLongLong(context: RuntimeContext, a: Long, b: Long, axis: APLValue?) = a
        override fun eval2ArgDoubleDouble(context: RuntimeContext, a: Double, b: Double, axis: APLValue?) = a

        override val optimisationFlags: OptimisationFlags
            get() = OptimisationFlags(
                OptimisationFlags.OPTIMISATION_FLAG_1ARG_LONG or
                        OptimisationFlags.OPTIMISATION_FLAG_1ARG_DOUBLE or
                        OptimisationFlags.OPTIMISATION_FLAG_2ARG_LONG_LONG or
                        OptimisationFlags.OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)

        override val name1Arg get() = "identity"
        override val name2Arg get() = "left"
    }

    override fun make(instantiation: FunctionInstantiation) = HideAPLFunctionImpl(instantiation)
}

class AccessFromIndexAPLFunction : APLFunctionDescriptor {
    class AccessFromIndexAPLFunctionImpl(pos: FunctionInstantiation) : APLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val aFixed = a.arrayify()
            val ad = aFixed.dimensions

            if (ad.size != 1) {
                throwAPLException(InvalidDimensionsException("Position argument is not rank 1", pos))
            }

            val bd = b.dimensions
            val indexList = if (axis == null) {
                if (ad[0] > bd.size) {
                    throwAPLException(
                        InvalidDimensionsException(
                            "Number of values in position argument must be equal or less than the number of dimensions",
                            pos))
                }
                Array(bd.size) { i ->
                    if (i < ad[0]) {
                        val indexValue = aFixed.valueAt(i)
                        if (indexValue.dimensions.size == 0) {
                            Either.Left(
                                indexValue.ensureNumber(pos).asInt(pos)
                                    .also { posAlongAxis -> checkAxisPositionIsInRange(posAlongAxis, bd, i, pos) })
                        } else {
                            Either.Right(IntArrayValue.fromAPLValue(indexValue, pos))
                        }
                    } else {
                        makeAllIndexList(bd[i])
                    }
                }
            } else {
                val list = ArrayList<Either<Int, IntArrayValue>?>()
                repeat(bd.size) { list.add(null) }
                val axesArray = axis.arrayify().toIntArray(pos)
                if (axesArray.any { it < 0 || it >= bd.size }) {
                    throwAPLException(IllegalAxisException("Invalid axis in axis specification", pos))
                }
                if (containsDuplicates(axesArray)) {
                    throwAPLException(IllegalAxisException("Duplicated axis in axis specification", pos))
                }
                if (ad[0] != axesArray.size) {
                    throwAPLException(
                        IllegalAxisException(
                            "Number of values in position argument must match the number of axes in axis specification", pos))
                }
                aFixed.iterateMembersWithPosition { m, p ->
                    val axisInt = axesArray[p]
                    val v = if (m.dimensions.size == 0) {
                        Either.Left(m.ensureNumber(pos).asInt(pos)
                            .also { posAlongAxis -> checkAxisPositionIsInRange(posAlongAxis, bd, axisInt, pos) })
                    } else {
                        Either.Right(IntArrayValue.fromAPLValue(m, pos))
                    }
                    list[axisInt] = v
                }
                Array(bd.size) { i ->
                    list[i] ?: makeAllIndexList(bd[i])
                }
            }
            return IndexedArrayValue(b, indexList)
        }

        private fun makeAllIndexList(size: Int): Either.Right<IntArrayValue> {
            val allIndexList = IntArrayValue.fromAPLValue(IotaArrayImpls.IotaArrayLong(size))
            return Either.Right(allIndexList)
        }

        private fun containsDuplicates(values: IntArray): Boolean {
            for (i0 in 1 until values.size) {
                val v0 = values[i0]
                for (i1 in 0 until i0) {
                    if (values[i1] == v0) return true
                }
            }
            return false
        }

        override val name2Arg get() = "lookup index"
    }

    override fun make(instantiation: FunctionInstantiation) = AccessFromIndexAPLFunctionImpl(instantiation)
}

class TakeArrayValue(val selection: IntArray, val source: APLValue, val pos: Position? = null) : APLArray() {
    override val dimensions = Dimensions(selection.map(Int::absoluteValue).toIntArray())
    private val multipliers = dimensions.multipliers()
    private val sourceDimensions = source.dimensions
    private val sourceMultipliers = sourceDimensions.multipliers()

    private fun sourceOrDefaultWithPosition(p: IntArray): APLValue {
        var curr = 0
        for (i in p.indices) {
            val pi = p[i]
            val di = sourceDimensions[i]
            if (pi < 0 || pi >= di) {
                return source.defaultValue()
            }
            curr += pi * sourceMultipliers[i]
        }
        return source.valueAt(curr)
    }

    override fun valueAt(p: Int): APLValue {
        val coords = multipliers.positionFromIndex(p)
        val adjusted = IntArray(coords.size) { i ->
            val d = selection[i]
            val v = coords[i]
            if (d >= 0) {
                v
            } else {
                sourceDimensions[i] + d + v
            }
        }
        return sourceOrDefaultWithPosition(adjusted)
    }

    fun replaceForUnder(replacement: APLValue): APLValue {
        val offset = IntArray(dimensions.size) { i ->
            val selectionValue = selection[i]
            if (selectionValue >= 0) {
                0
            } else {
                sourceDimensions[i] + selectionValue
            }
        }
        return OverlayReplacementValue(source, dimensions, replacement, offset, pos)
    }
}

class OverlayReplacementValue(
    val src: APLValue,
    val srcReplacementDimensions: Dimensions,
    val replacement: APLValue,
    val offset: IntArray,
    pos: Position?
) : APLArray() {
    private val srcDimensions = src.dimensions
    private val replacementDimensions = replacement.dimensions

    override val dimensions = Dimensions(IntArray(srcReplacementDimensions.size) { i ->
        srcDimensions[i] - srcReplacementDimensions[i] + replacementDimensions[i]
    })

    private val srcMultipliers = srcDimensions.multipliers()
    private val multipliers = dimensions.multipliers()

    init {
        if (dimensions.size != offset.size) {
            throw IllegalStateException("offset size does not match src dimensions")
        }
        if (dimensions.size != replacementDimensions.size) {
            throwAPLException(InvalidDimensionsException("replacement array rank mismatch", pos))
        }

        // After the below loop, resizableDimension will have the following value:
        //   positive integer: The dimension that can be resized
        //   null: All dimensions can be resized
        //   -1: No dimensions can be resized
        var resizableDimension: Int? = null
        repeat(dimensions.size) { i ->
            // If all but a single dimension extends over the entire range, then that dimension can be resized
            if (replacementDimensions[i] != srcDimensions[i]) {
                if (resizableDimension == null) {
                    resizableDimension = i
                } else {
                    resizableDimension = -1
                }
            }
        }
        repeat(dimensions.size) { i ->
            if (resizableDimension != null) {
                if (srcReplacementDimensions[i] != replacementDimensions[i] && i != resizableDimension) {
                    throwAPLException(InvalidDimensionsException("cannot resize axis: ${i}", pos))
                }
            }
            if (dimensions[i] < offset[i] + replacementDimensions[i]) {
                throwAPLException(InvalidDimensionsException("replacement array size overflows at index ${i}", pos))
            }
        }
    }

    override fun valueAt(p: Int): APLValue {
        val posArray = multipliers.positionFromIndex(p)
        return if (isWithinReplacement(posArray)) {
            val newPosArray = IntArray(posArray.size) { i -> posArray[i] - offset[i] }
            replacement.valueAt(replacementDimensions.indexFromPosition(newPosArray))
        } else {
            val newPosArray = IntArray(posArray.size) { i ->
                val posOnAxis = posArray[i]
                if (posOnAxis < offset[i]) {
                    posOnAxis
                } else {
                    posOnAxis - replacementDimensions[i] + srcReplacementDimensions[i]
                }
            }
            src.valueAt(srcDimensions.indexFromPosition(newPosArray, srcMultipliers))
        }
    }

    private fun isWithinReplacement(posArray: IntArray): Boolean {
        repeat(dimensions.size) { i ->
            val p = posArray[i]
            val offsetValue = offset[i]
            if (p < offsetValue || p >= replacementDimensions[i] + offsetValue) {
                return false
            }
        }
        return true
    }
}

class TakeAPLFunction : APLFunctionDescriptor {
    class TakeAPLFunctionImpl(pos: FunctionInstantiation) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            if (axis != null) {
                throwAPLException(AxisNotSupported(pos))
            }
            val v = a.unwrapDeferredValue()
            return when {
                v is APLSingleValue -> v
                v.isScalar() -> v.valueAt(0)
                v.size == 0 -> v.defaultValue()
                else -> v.valueAt(0)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val b0 = b.arrayify()
            val selection = if (axis == null) {
                val a0 = a.arrayify()
                val aDimensions = a0.dimensions
                val bDimensions = b0.dimensions
                if (aDimensions.size != 1) {
                    throwAPLException(InvalidDimensionsException("A must be a scalar or one-dimensional array", pos))
                }
                val numSelections = aDimensions[0]
                if (numSelections > bDimensions.size) {
                    throwAPLException(InvalidDimensionsException("Size of A must be less than or equal to the rank of B", pos))
                }
                IntArray(bDimensions.size) { i ->
                    if (i < numSelections) {
                        a0.valueAtInt(i, pos)
                    } else {
                        bDimensions[i]
                    }
                }
            } else {
                val axisInt = axis.ensureNumber(pos).asInt()
                val a0 = a.unwrapDeferredValue()
                val a0Dimensions = a0.dimensions
                val argInteger = when {
                    a0Dimensions.size == 0 -> a0.ensureNumber(pos).asInt()
                    a0Dimensions.size == 1 && a0Dimensions[0] == 1 -> a0.valueAt(0).ensureNumber(pos).asInt()
                    else -> throwAPLException(APLIllegalArgumentException("When given an explicit axis, the left argument must be a single integer", pos))
                }
                val bDimensions = b0.dimensions
                ensureValidAxis(axisInt, bDimensions, pos)
                IntArray(bDimensions.size) { i ->
                    if (i == axisInt) {
                        argInteger
                    } else {
                        bDimensions[i]
                    }
                }
            }
            return TakeArrayValue(selection, b0, pos)
        }

        override fun evalWithStructuralUnder2Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val underValue = eval2Arg(context, a, b, null)
            if (underValue !is TakeArrayValue) {
                throw IllegalStateException("Result is not of the correct type. Type = ${underValue::class}")
            }
            val updated = baseFn.eval1Arg(context, underValue, null)
            return underValue.replaceForUnder(updated)
        }

        override val name1Arg get() = "take"
        override val name2Arg get() = "take"
    }

    override fun make(instantiation: FunctionInstantiation) = TakeAPLFunctionImpl(instantiation)
}

class DropArrayValue(val selection: IntArray, val source: APLValue, val pos: Position) : APLArray() {
    private val sourceDimensions = source.dimensions
    override val dimensions =
        Dimensions(selection.mapIndexed { index, v -> max(0, sourceDimensions[index] - v.absoluteValue) }.toIntArray())
    private val dimensionsMultipliers = dimensions.multipliers()

    override fun valueAt(p: Int): APLValue {
        val coords = dimensionsMultipliers.positionFromIndex(p)
        val adjusted = IntArray(coords.size) { i ->
            val d = selection[i]
            val v = coords[i]
            if (d >= 0) {
                d + v
            } else {
                v
            }
        }
        return source.valueAt(sourceDimensions.indexFromPosition(adjusted))
    }

    fun replaceForUnder(replacement: APLValue): APLValue {
        val offset = IntArray(dimensions.size) { i ->
            val selectionValue = selection[i]
            if (selectionValue >= 0) {
                selectionValue
            } else {
                0
            }
        }
        return OverlayReplacementValue(source, dimensions, replacement, offset, pos)
    }
}

class DropResultValueOneArg(val a: APLValue, val pos: Position) : APLArray() {
    override val dimensions: Dimensions

    init {
        val d = a.dimensions
        dimensions = dimensionsOfSize(d[0] - 1)
    }

    override fun valueAt(p: Int) = a.valueAt(p + 1)

    fun replaceForUnder(updated: APLValue): APLValue {
        if (updated.dimensions.size != 1) {
            throwAPLException(APLIllegalArgumentException("Result of under function must be a 1-dimensional array", pos))
        }
        return Concatenated1DArrays(APLArrayImpl(dimensionsOfSize(1), arrayOf(a.valueAt(0))), updated)
    }
}

class DropAPLFunction : APLFunctionDescriptor {
    class DropAPLFunctionImpl(pos: FunctionInstantiation) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            if (axis != null) {
                throwAPLException(AxisNotSupported(pos))
            }
            val d = a.dimensions
            return when {
                d.size != 1 -> throwAPLException(APLIllegalArgumentException("Expected 1-dimensional array. Dimensions: ${d}", pos))
                d[0] == 0 -> APLNullValue.APL_NULL_INSTANCE
                else -> DropResultValueOneArg(a, pos)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val b0 = b.arrayify()
            val axisArray = if (axis == null) {
                val a0 = a.arrayify()
                val aDimensions = a0.dimensions
                if (aDimensions.size != 1) {
                    throwAPLException(InvalidDimensionsException("Left argument to drop must be a scalar or 1-dimensional array", pos))
                }
                val bDimensions = b0.dimensions
                if (aDimensions[0] > bDimensions.size) {
                    throwAPLException(InvalidDimensionsException("Size of A must be less than or equal to the rank of B", pos))
                }
                IntArray(bDimensions.size) { i ->
                    if (i < aDimensions[0]) {
                        a0.valueAtInt(i, pos)
                    } else {
                        0
                    }
                }
            } else {
                val axisInt = axis.ensureNumber(pos).asInt()
                val a0 = a.unwrapDeferredValue()
                val a0Dimensions = a0.dimensions
                val argInteger = when {
                    a0Dimensions.size == 0 -> a0.ensureNumber(pos).asInt()
                    a0Dimensions.size == 1 && a0Dimensions[0] == 1 -> a0.valueAt(0).ensureNumber(pos).asInt()
                    else -> throwAPLException(APLIllegalArgumentException("When given an explicit axis, the left argument must be a single integer", pos))
                }
                val bDimensions = b0.dimensions
                ensureValidAxis(axisInt, bDimensions, pos)
                IntArray(bDimensions.size) { i -> if (i == axisInt) argInteger else 0 }
            }
            return DropArrayValue(axisArray, b0, pos)
        }

        override fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue): APLValue {
            val underValue = eval1Arg(context, a, null)
            if (underValue !is DropResultValueOneArg) {
                throw IllegalStateException("Result is not of the correct type. Type = ${underValue::class}")
            }
            val updated = baseFn.eval1Arg(context, underValue, null)
            return underValue.replaceForUnder(updated)
        }

        override fun evalWithStructuralUnder2Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val underValue = eval2Arg(context, a, b, null)
            if (underValue !is DropArrayValue) {
                throw IllegalStateException("Result is not of the correct type. Type = ${underValue::class}")
            }
            val updated = baseFn.eval1Arg(context, underValue, null)
            return underValue.replaceForUnder(updated)
        }

        override val name1Arg get() = "drop"
        override val name2Arg get() = "drop"
    }

    override fun make(instantiation: FunctionInstantiation) = DropAPLFunctionImpl(instantiation)
}

//time:measureTime { (n?n){+/(∨/⍵∘.=⍺)/⍵}n?n←40000 }
//Total time: 7.682
//
//799980000

class RandomAPLFunction : APLFunctionDescriptor {
    class RandomAPLFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            return if (v is APLSingleValue) {
                makeRandom(v.ensureNumber(pos).asLong(pos)).makeAPLNumber()
            } else {
                APLArrayLong(v.dimensions, LongArray(v.dimensions.contentSize()) { index -> makeRandom(v.valueAtLong(index, pos)) })
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val aInt = a.ensureNumber(pos).asInt(pos)
            val bLong = b.ensureNumber(pos).asLong(pos)
            if (aInt < 0) {
                throwAPLException(APLIncompatibleDomainsException("A should not be negative, was: ${aInt}", pos))
            }
            if (bLong < 0) {
                throwAPLException(APLIncompatibleDomainsException("B should not be negative, was: ${bLong}", pos))
            }
            if (aInt > bLong) {
                throwAPLException(
                    APLIncompatibleDomainsException(
                        "A should not be greater than B. A: ${aInt}, B: ${bLong}",
                        pos))
            }
            if (aInt == 0) {
                return APLArrayLong(dimensionsOfSize(0), longArrayOf())
            }

            val result = randSubsetC2(aInt, bLong)
            return APLArrayLong(dimensionsOfSize(result.size), result)
        }

        private fun makeRandom(limit: Long): Long {
            return (0 until limit).random()
        }

        private fun randSubsetC2(a: Int, b: Long): LongArray {
            val rp = LongArray(a) { i -> i.toLong() }
            val map = HashMap<Long, Long>(0)
            repeat(a) { i ->
                val j = makeRandom(b - i) + i
                if (j < a) {
                    val jInt = j.toInt()
                    val c = rp[jInt]
                    rp[jInt] = rp[i]
                    rp[i] = c
                } else {
                    rp[i] = (map[j] ?: j).also { map[j] = rp[i] }
                }
            }
            return rp
        }

        override val name1Arg get() = "deal"
        override val name2Arg get() = "random"
    }

    override fun make(instantiation: FunctionInstantiation) = RandomAPLFunctionImpl(instantiation)
}

class RotatedAPLValue private constructor(val source: APLValue, val axis: Int, val numShifts: Long) : APLArray() {
    private val axisActionFactors = AxisActionFactors(source.dimensions, axis)

    override val dimensions get() = source.dimensions
    override val specialisedType get() = source.specialisedType

    private fun computeValueAt(p: Int): Int {
        axisActionFactors.withFactors(p) { highVal, lowVal, axisCoord ->
            val coord = (axisCoord + numShifts).plusMod(dimensions[axis].toLong()).toInt()
            return (highVal * axisActionFactors.highValFactor) + (coord * axisActionFactors.multipliers[axis]) + lowVal
        }
    }

    override fun valueAt(p: Int) = source.valueAt(computeValueAt(p))
    override fun valueAtLong(p: Int, pos: Position?) = source.valueAtLong(computeValueAt(p), pos)
    override fun valueAtDouble(p: Int, pos: Position?) = source.valueAtDouble(computeValueAt(p), pos)

    companion object {
        fun make(source: APLValue, axis: Int, numShifts: Long): APLValue {
            val dimensions = source.dimensions
            return if (dimensions.isEmpty() || numShifts % (dimensions[axis]) == 0L) {
                source
            } else {
                RotatedAPLValue(source, axis, numShifts)
            }
        }
    }
}

class MultiRotationRotatedAPLValue(
    val source: APLValue,
    val axis: Int,
    val selectionMultipliers: Dimensions.DimensionMultipliers,
    val selection: IntArray
) : APLArray() {
    override val dimensions = source.dimensions

    private val axisActionFactors = AxisActionFactors(source.dimensions, axis)
    private val sourceMultipliers = dimensions.multipliers()

    override fun valueAt(p: Int): APLValue {
        val coords = sourceMultipliers.positionFromIndex(p)
        var curr = 0
        repeat(selectionMultipliers.size) { i ->
            val dimensionIndex = coords[if (i < axis) i else i + 1]
            curr += selectionMultipliers[i] * dimensionIndex
        }
        val numShifts = selection[curr]
        return axisActionFactors.withFactors(p) { highVal, lowVal, axisCoord ->
            val coord = (axisCoord + numShifts.toLong()).plusMod(dimensions[axis].toLong()).toInt()
            source.valueAt((highVal * axisActionFactors.highValFactor) + (coord * axisActionFactors.multipliers[axis]) + lowVal)
        }
    }
}

class InverseAPLValue private constructor(val source: APLValue, val axis: Int) : APLArray() {
    private val axisActionFactors = AxisActionFactors(source.dimensions, axis)

    override val dimensions = source.dimensions
    override val specialisedType get() = source.specialisedType

    override val labels by lazy { resolveLabels() }

    override fun valueAt(p: Int): APLValue {
        return source.valueAt(destinationIndex(p))
    }

    override fun valueAtLong(p: Int, pos: Position?): Long {
        return source.valueAtLong(destinationIndex(p), pos)
    }

    override fun valueAtDouble(p: Int, pos: Position?): Double {
        return source.valueAtDouble(destinationIndex(p), pos)
    }

    private fun destinationIndex(p: Int): Int {
        return axisActionFactors.withFactors(p) { highVal, lowVal, axisCoord ->
            val coord = axisActionFactors.dimensions[axis] - axisCoord - 1
            (highVal * axisActionFactors.highValFactor) + (coord * axisActionFactors.multipliers[axis]) + lowVal
        }
    }

    private fun resolveLabels(): DimensionLabels? {
        val parent = source.labels ?: return null
        val parentList = parent.labels
        val newLabels = ArrayList<List<AxisLabel?>?>()
        parentList.forEachIndexed { i, axisLabels ->
            val newAxisLabels = when {
                axisLabels == null -> null
                i == axis -> axisLabels.asReversed()
                else -> axisLabels
            }
            newLabels.add(newAxisLabels)
        }
        return DimensionLabels(newLabels)
    }

    companion object {
        fun make(source: APLValue, axis: Int): APLValue {
            val dimensions = source.dimensions
            return if (dimensions.isEmpty() || dimensions[axis] <= 1) {
                source
            } else {
                InverseAPLValue(source, axis)
            }
        }
    }
}

abstract class RotateFunction(pos: FunctionInstantiation) : APLFunction(pos) {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        val axisInt = if (axis == null) defaultAxis(a) else axis.ensureNumber(pos).asInt(pos).also { ensureValidAxis(it, a.dimensions, pos) }
        return InverseAPLValue.make(a, axisInt)
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        val bDimensions = b.dimensions
        val axisInt = if (axis == null) defaultAxis(b) else axis.ensureNumber(pos).asInt(pos).also { ensureValidAxis(it, bDimensions, pos) }
        if (a.isScalar()) {
            val numShifts = a.ensureNumber(pos).asLong(pos)
            return RotatedAPLValue.make(b, axisInt, numShifts)
        } else {
            val aCollapsed = a.collapse()
            val aDimensions = aCollapsed.dimensions
            repeat(aDimensions.size) { i ->
                if (aDimensions[i] != bDimensions[if (i < axisInt) i else i + 1]) {
                    throwAPLException(InvalidDimensionsException("Invalid dimension", pos))
                }
            }
            return MultiRotationRotatedAPLValue(
                b,
                axisInt,
                aCollapsed.dimensions.multipliers(),
                aCollapsed.toIntArray(pos))
        }
    }

    override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?) =
        eval1Arg(context, a, axis)

    override fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue) =
        inversibleStructuralUnder1Arg(this, baseFn, context, a)

    abstract fun defaultAxis(value: APLValue): Int
}

class RotateHorizFunction : APLFunctionDescriptor {
    class RotateHorizFunctionImpl(pos: FunctionInstantiation) : RotateFunction(pos) {
        override fun defaultAxis(value: APLValue) = value.rank - 1
        override fun deriveBitwise() = BitwiseShiftFunction()

        override val name1Arg get() = "reverse horiz"
        override val name2Arg get() = "rotate horiz"
    }

    override fun make(instantiation: FunctionInstantiation) = RotateHorizFunctionImpl(instantiation)
}

class RotateVertFunction : APLFunctionDescriptor {
    class RotateVertFunctionImpl(pos: FunctionInstantiation) : RotateFunction(pos) {
        override fun defaultAxis(value: APLValue) = 0
        override val name1Arg get() = "reverse vert"
        override val name2Arg get() = "rotate vert"
    }

    override fun make(instantiation: FunctionInstantiation) = RotateVertFunctionImpl(instantiation)
}

class TransposedAPLValue(val transposeAxis: IntArray, val b: APLValue, pos: Position) : APLArray() {
    override val dimensions: Dimensions
    private val multipliers: Dimensions.DimensionMultipliers
    private val bDimensions: Dimensions
    override val specialisedType get() = b.specialisedType

    override val labels by lazy { resolveLabels() }

    init {
        bDimensions = b.dimensions
        val inverseTransposedAxis = makeInverseTransposeIndex(transposeAxis)
            ?: throwAPLException(InvalidDimensionsException("Not all axis represented in transpose definition: ${Arrays.toString(transposeAxis)}", pos))
        dimensions = Dimensions(IntArray(bDimensions.size) { index -> bDimensions[inverseTransposedAxis[index]] })
        multipliers = dimensions.multipliers()
    }

    override fun valueAt(p: Int): APLValue {
        return b.valueAt(destinationIndex(p))
    }

    override fun valueAtLong(p: Int, pos: Position?): Long {
        return b.valueAtLong(destinationIndex(p), pos)
    }

    override fun valueAtDouble(p: Int, pos: Position?): Double {
        return b.valueAtDouble(destinationIndex(p), pos)
    }

    private fun destinationIndex(p: Int): Int {
        val c = dimensions.positionFromIndex(p)
        val newPos = IntArray(dimensions.size) { index -> c[transposeAxis[index]] }
        return bDimensions.indexFromPosition(newPos)
    }

    private fun resolveLabels(): DimensionLabels? {
        val parent = b.labels ?: return null
        val parentList = parent.labels
        val newLabels = ArrayList<List<AxisLabel?>?>()
        for (origAxis in transposeAxis) {
            newLabels.add(parentList[origAxis])
        }
        return DimensionLabels(newLabels)
    }

    companion object {
        fun makeInverseTransposeIndex(transposeAxis: IntArray): IntArray? {
            return IntArray(transposeAxis.size) { index ->
                var res = -1
                for (i in transposeAxis.indices) {
                    if (transposeAxis[i] == index) {
                        res = i
                        break
                    }
                }
                if (res == -1) {
                    return null
                }
                res
            }
        }
    }
}

class TransposeFunction : APLFunctionDescriptor {
    class TransposeFunctionImpl(pos: FunctionInstantiation) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return if (a.isScalar()) {
                a
            } else {
                val size = a.dimensions.size
                val axisArg = IntArray(size) { i -> size - i - 1 }
                TransposedAPLValue(axisArg, a, pos)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val a1 = a.arrayify()
            val aDimensions = a1.dimensions
            val bDimensions = b.dimensions
            if (aDimensions.size != 1 || aDimensions[0] != bDimensions.size) {
                throwAPLException(InvalidDimensionsException("Transpose arguments have wrong dimensions", pos))
            }

            if (b.isScalar()) {
                if (aDimensions[0] == 0) {
                    return b
                } else {
                    throwAPLException(
                        InvalidDimensionsException(
                            "Transpose of scalar values requires empty left argument",
                            pos))
                }
            }

            val transposeAxis = IntArray(aDimensions[0]) { index -> a1.valueAtInt(index, pos) }
            return TransposedAPLValue(transposeAxis, b, pos)
        }

        override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            if (axis != null) {
                throw AxisNotSupported(pos)
            }
            return eval1Arg(context, a, axis)
        }

        override fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue) =
            inversibleStructuralUnder1Arg(this, baseFn, context, a)

        override val name1Arg get() = "transpose"
        override val name2Arg get() = "transpose"
    }

    override fun make(instantiation: FunctionInstantiation) = TransposeFunctionImpl(instantiation)
}

class CompareFunction : APLFunctionDescriptor {
    class CompareFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            fun recurse(v: APLValue): Int {
                val d = v.dimensions
                return when {
                    v is APLSingleValue -> 0
                    d.size == 0 -> recurse(v.disclose()) + 1
                    d.contentSize() == 0 -> 1
                    else -> {
                        var first = true
                        var currentSize = 0
                        v.iterateMembers { inner ->
                            val size = recurse(inner)
                            if (first) {
                                currentSize = size
                                first = false
                            } else {
                                currentSize = max(currentSize, size)
                            }
                        }
                        currentSize + 1
                    }
                }
            }
            return recurse(a).makeAPLNumber()
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return makeBoolean(a.compareEquals(b))
        }

        override val name1Arg get() = "depth"
        override val name2Arg get() = "compare"
    }

    override fun make(instantiation: FunctionInstantiation) = CompareFunctionImpl(instantiation)
}

class CompareNotEqualFunction : APLFunctionDescriptor {
    class CompareFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val dimensions = a.dimensions
            val ret = if (dimensions.size == 0) 0 else dimensions[0]
            return ret.makeAPLNumber()
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return makeBoolean(!a.compareEquals(b))
        }

        override val name1Arg get() = "size"
        override val name2Arg get() = "compare not equals"
    }

    override fun make(instantiation: FunctionInstantiation) = CompareFunctionImpl(instantiation)
}

object MemberResultValueImpls {
    open class MemberResultValue(val context: RuntimeContext, val a: APLValue, val b: APLValue, val pos: Position) : APLArray() {
        override val dimensions = a.dimensions
        override val specialisedType get() = ArrayMemberType.LONG

        override fun valueAt(p: Int): APLValue {
            return valueAtLong(p, null).makeAPLNumber()
        }

        override fun valueAtLong(p: Int, pos: Position?): Long {
            return findInArray(a.valueAt(p).unwrapDeferredValue())
        }

        override fun unwrapDeferredValue(): APLValue {
            return if (dimensions.isEmpty()) {
                findInArray(a.disclose()).makeAPLNumber()
            } else {
                this
            }
        }

        protected open fun findInArray(target: APLValue): Long {
            return findGeneric(target)
        }

        fun findGeneric(target: APLValue): Long {
            b.iterateMembers { value ->
                if (target.compareEquals(value)) {
                    return 1
                }
            }
            return 0
        }
    }

    class MemberResultValueRightLong(
        context: RuntimeContext, a: APLValue, b: APLValue, pos: Position
    ) : MemberResultValue(context, a, b, pos) {
        override fun findInArray(target: APLValue): Long {
            val targetNum = target.ensureNumberOrNull() ?: return 0
            when (targetNum) {
                is APLLong -> {
                    val targetLong = targetNum.asLong(pos)
                    repeat(b.size) { i ->
                        if (b.valueAtLong(i, pos) == targetLong) {
                            return 1
                        }
                    }
                }
                is APLDouble -> {
                    val targetDouble = targetNum.asDouble(pos)
                    repeat(b.size) { i ->
                        if (b.valueAtLong(i, pos).toDouble() == targetDouble) {
                            return 1
                        }
                    }
                }
                else -> {
                    findGeneric(target)
                }
            }
            return 0
        }
    }

    fun make(context: RuntimeContext, a: APLValue, b: APLValue, pos: Position): MemberResultValue {
        return when (b.specialisedType) {
            ArrayMemberType.LONG -> MemberResultValueRightLong(context, a, b, pos)
            else -> MemberResultValue(context, a, b, pos)
        }
    }
}

class MemberFunction : APLFunctionDescriptor {
    class MemberFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return MemberResultValueImpls.make(context, a, b, pos)
        }

        override val name2Arg get() = "member"
    }

    override fun make(instantiation: FunctionInstantiation) = MemberFunctionImpl(instantiation)
}


class FindResultValue(val context: RuntimeContext, val a: APLValue, val b: APLValue) : APLArray() {
    override val dimensions = b.dimensions
    private val aDimensions = a.dimensions
    private val aMultipliers = aDimensions.multipliers()
    private val bMultipliers = dimensions.multipliers()

    override fun valueAt(p: Int): APLValue {
        val dimensionsDiff = dimensions.size - aDimensions.size
        val coord = dimensions.positionFromIndex(p)

        if (aDimensions.size > dimensions.size) {
            return makeBoolean(false)
        }

        aDimensions.dimensions.forEachIndexed { i, v ->
            if (coord[dimensionsDiff + i] > dimensions[dimensionsDiff + i] - v) {
                return makeBoolean(false)
            }
        }

        fun processOneLevel(level: Int, aCurr: Int, bCurr: Int): Boolean {
            if (level == aDimensions.size) {
                return a.valueAt(aCurr).compareEquals(b.valueAt(bCurr))
            } else {
                val axis = dimensionsDiff + level
                val aStride = aMultipliers[level]
                val bStride = bMultipliers[axis]
                val length = aDimensions[level]
                for (i in 0 until length) {
                    val ap = aCurr + i * aStride
                    val bp = bCurr + i * bStride
                    if (!processOneLevel(level + 1, ap, bp)) {
                        return false
                    }
                }
                return true
            }
        }

        return makeBoolean(processOneLevel(0, 0, p))
    }
}

class FindFunction : APLFunctionDescriptor {
    class FindFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return if (b.dimensions.size == 0) {
                if (a.compareEquals(b)) APLLONG_1 else APLLONG_0
            } else {
                FindResultValue(context, a, b)
            }
        }

        override val name2Arg get() = "find"
    }

    override fun make(instantiation: FunctionInstantiation) = FindFunctionImpl(instantiation)
}

class SelectElementsValue(selectIndexes: IntArray, val b: APLValue, val axis: Int) : APLArray() {
    private val bDimensions = b.dimensions

    override val dimensions: Dimensions

    private val axisActionFactors: AxisActionFactors
    private val highMultiplier: Int
    private val bStride: Int
    private val aIndex: IntArray

    init {
        val sizeAlongAxis = selectIndexes.reduceWithInitial(0) { a, b -> a + b }
        dimensions = Dimensions(IntArray(bDimensions.size) { i ->
            if (i == axis) {
                sizeAlongAxis
            } else {
                bDimensions[i]
            }
        })

        val m = bDimensions.multipliers()
        highMultiplier = if (axis == 0) bDimensions.size else m[axis - 1]
        bStride = m[axis]

        aIndex = IntArray(sizeAlongAxis)
        var aIndexPos = 0
        var bIndexPos = 0
        for (dimensionsIndex in selectIndexes) {
            for (i2 in 0 until dimensionsIndex) {
                aIndex[aIndexPos++] = bIndexPos
            }
            bIndexPos++
        }

        axisActionFactors = AxisActionFactors(dimensions, axis)
    }

    override fun valueAt(p: Int): APLValue {
        axisActionFactors.withFactors(p) { high, low, axisCoord ->
            val bIndexPos = aIndex[axisCoord]
            val resultPos = high * highMultiplier + bIndexPos * bStride + low
            return b.valueAt(resultPos)
        }
    }
}

@Suppress("IfThenToElvis")
abstract class SelectElementsFunctionImpl(pos: FunctionInstantiation) : APLFunction(pos) {
    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        val bFixed = b.arrayify()
        val aDimensions = a.dimensions
        val bDimensions = bFixed.dimensions
        val axisInt = if (axis == null) defaultAxis(bFixed) else axis.ensureNumber(pos).asInt(pos)
        ensureValidAxis(axisInt, bDimensions, pos)
        if (!(aDimensions.size == 0 || (aDimensions.size == 1 && aDimensions[0] == bDimensions[axisInt]))) {
            throwAPLException(
                InvalidDimensionsException(
                    "A must be a single-dimensional array of the same size as the dimension of B along the selected axis.",
                    pos))
        }
        val selectIndexes = if (a.isScalar()) {
            a.ensureNumber(pos).asInt(pos).let { v ->
                if (v < 0) {
                    throwAPLException(APLIncompatibleDomainsException("Selection index is negative", pos))
                }
                IntArray(bDimensions[axisInt]) { v }
            }
        } else {
            a.toIntArray(pos).onEach { v ->
                if (v < 0) {
                    throwAPLException(APLIncompatibleDomainsException("Selection index is negative", pos))
                }
            }
        }
        return SelectElementsValue(selectIndexes, bFixed, axisInt)
    }

    abstract fun defaultAxis(value: APLValue): Int
}

class SelectElementsFirstAxisFunction : APLFunctionDescriptor {
    class SelectElementsFirstAxisFunctionImpl(pos: FunctionInstantiation) : SelectElementsFunctionImpl(pos) {
        override fun defaultAxis(value: APLValue): Int {
            return 0
        }

        override val name2Arg get() = "select first axis"
    }

    override fun make(instantiation: FunctionInstantiation) = SelectElementsFirstAxisFunctionImpl(instantiation)
}

class SelectElementsLastAxisFunction : APLFunctionDescriptor {
    class SelectElementsLastAxisFunctionImpl(pos: FunctionInstantiation) : SelectElementsFunctionImpl(pos) {
        override fun defaultAxis(value: APLValue): Int {
            return value.dimensions.lastAxis(pos)
        }

        override val name2Arg get() = "select last axis"
    }

    override fun make(instantiation: FunctionInstantiation) = SelectElementsLastAxisFunctionImpl(instantiation)
}

class FormatAPLFunction : APLFunctionDescriptor {
    class FormatAPLFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return APLString.make(a.formatted(FormatStyle.PLAIN))
        }

        override val name1Arg get() = "format"
    }

    override fun make(instantiation: FunctionInstantiation) = FormatAPLFunctionImpl(instantiation)
}

class ParseNumberFunction : APLFunctionDescriptor {
    class ParseNumberFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val s = a.toStringValue(pos)

            fun throwParseError(): Nothing = throwAPLException(APLEvalException("Value cannot be parsed as a number: '${s}'", pos))

            val intMatch = INTEGER_PATTERN.matchEntire(s)
            if (intMatch != null) {
                return intMatch.groups.get(1)!!.value.toInt().makeAPLNumber()
            }
            val doubleMatch = DOUBLE_PATTERN.matchEntire(s)
            if (doubleMatch != null) {
                val doubleAsString = doubleMatch.groups.get(1)!!.value
                if (doubleAsString == ".") throwParseError()
                return doubleAsString.toDouble().makeAPLNumber()
            }
            throwParseError()
        }

        override val name1Arg get() = "parse number"

        companion object {
            private val INTEGER_PATTERN = "^[ \t]*(-?[0-9]+)[ \t]*$".toRegex()
            private val DOUBLE_PATTERN = "^[ \t]*(-?[0-9]*\\.[0-9]*)[ \t]*$".toRegex()
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ParseNumberFunctionImpl(instantiation)
}


class WhereAPLFunction : APLFunctionDescriptor {
    class WhereAPLFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return if (a.isScalar()) {
                val v = a.unwrapDeferredValue()
                if (v is APLNumber) {
                    APLNullValue.APL_NULL_INSTANCE
                } else {
                    throwAPLException(APLIncompatibleDomainsException("Argument must be a number", pos))
                }
            } else {
                val aDimensions = a.dimensions
                val multipliers = aDimensions.multipliers()
                val result = ArrayList<APLValue>()
                a.iterateMembersWithPosition { value, i ->
                    val n = value.ensureNumber(pos).asInt(pos)
                    if (n > 0) {
                        val index = if (aDimensions.size == 1) {
                            i.makeAPLNumber()
                        } else {
                            val positionIndex = multipliers.positionFromIndex(i)
                            val valueArray = Array<APLValue>(positionIndex.size) { v -> positionIndex[v].makeAPLNumber() }
                            APLArrayImpl(dimensionsOfSize(valueArray.size), valueArray)
                        }
                        repeat(n) {
                            result.add(index)
                        }
                    } else if (n < 0) {
                        throwAPLException(
                            APLIncompatibleDomainsException(
                                "Negative value found in right argument",
                                pos))
                    }
                }
                APLArrayList(dimensionsOfSize(result.size), result)
            }
        }

        data class LocationWithValue(val location: IntArray, var value: Long = 1) {
            fun isLocationBefore(other: IntArray): Boolean {
                require(location.size == other.size)
                location.indices.forEach { i ->
                    val otherPos = other[i]
                    val locationPos = location[i]
                    when {
                        otherPos < locationPos -> return true
                        otherPos > locationPos -> return false
                    }
                }
                return false
            }
        }

        private fun makeLocationWithValue(location: IntArray): LocationWithValue {
            location.forEach { v ->
                if (v < 0) {
                    throwAPLException(APLIncompatibleDomainsException("Negative argument", pos))
                }
            }
            return LocationWithValue(location)
        }

        override fun evalInverse1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val a0 = a.collapse()
            if (a0.dimensions.size != 1) {
                throwAPLException(APLIncompatibleDomainsException("Argument must be a one-dimensional array, got ${a0.dimensions}", pos))
            }
            if (a0.dimensions[0] == 0) {
                return APLNullValue.APL_NULL_INSTANCE
            }
            val valuesList = ArrayList<LocationWithValue>()
            var maxSize: IntArray? = null
            var prevElement: LocationWithValue? = null
            a0.iterateMembers { v ->
                val v0 = v.arrayify()
                if (v0.rank != 1) {
                    throwAPLException(InvalidDimensionsException("All arguments must be scalars or one-dimensional arrays", pos))
                }
                val v0Array = v0.toIntArray(pos)
                when {
                    maxSize == null -> maxSize = IntArray(v0Array.size) { i -> v0Array[i] + 1 }
                    v0Array.size != maxSize!!.size -> throwAPLException(InvalidDimensionsException("All arguments must have the same size", pos))
                    else -> {
                        v0Array.indices.forEach { i ->
                            val size = v0Array[i] + 1
                            if (maxSize!![i] < size) maxSize!![i] = size
                        }
                    }
                }
                if (prevElement != null) {
                    if (v0Array.contentEquals(prevElement!!.location)) {
                        prevElement!!.value++
                    } else if (prevElement!!.isLocationBefore(v0Array)) {
                        throwAPLException(APLIncompatibleDomainsException("All arguments must be ordered", pos))
                    } else {
                        prevElement = makeLocationWithValue(v0Array).also { location -> valuesList.add(location) }
                    }
                } else {
                    prevElement = makeLocationWithValue(v0Array).also { location -> valuesList.add(location) }
                }
            }
            val d = Dimensions(maxSize!!)
            var currIndex = 0
            var curr: LocationWithValue? = valuesList[0]
            val position = IntArray(maxSize!!.size)
            val valuesListSize = valuesList.size
            val content = LongArray(d.contentSize()) { i ->
                val result = if (currIndex < valuesListSize && position.contentEquals(curr!!.location)) {
                    curr!!.value.also {
                        curr = if (++currIndex < valuesListSize) {
                            valuesList[currIndex]
                        } else {
                            null
                        }
                    }
                } else {
                    0
                }
                d.incrementMutablePosition(position)
                result
            }
            return APLArrayLong(d, content)
        }

        override val name1Arg get() = "where"
    }

    override fun make(instantiation: FunctionInstantiation) = WhereAPLFunctionImpl(instantiation)
}

class UniqueFunction : APLFunctionDescriptor {
    class UniqueFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        private fun iterateUnique(input: List<APLValue>): APLArray {
            val map = HashSet<APLValue.APLValueKey>()
            val result = ArrayList<APLValue>()
            input.forEach { a ->
                a.iterateMembers { v ->
                    val key = v.makeKey()
                    if (!map.contains(key)) {
                        result.add(v)
                        map.add(key)
                    }
                }
            }
            return APLArrayList(dimensionsOfSize(result.size), result)
        }

        private fun collapseAndCheckRank(a: APLValue): APLValue {
            val a1 = a.arrayify().collapse()
            if (a1.rank != 1) {
                throwAPLException(
                    InvalidDimensionsException(
                        "Argument to unique must be a scalar or a 1-dimensional array",
                        pos))
            }
            return a1
        }

        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return iterateUnique(listOf(collapseAndCheckRank(a)))
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return iterateUnique(listOf(collapseAndCheckRank(a), collapseAndCheckRank(b)))
        }

        override val name1Arg get() = "unique"
        override val name2Arg get() = "unique"
    }

    override fun make(instantiation: FunctionInstantiation) = UniqueFunctionImpl(instantiation)
}

class IntersectionAPLFunction : APLFunctionDescriptor {
    class IntersectionAPLFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val map = HashSet<APLValue.APLValueKey>()
            val leftKeys = HashSet<APLValue.APLValueKey>()
            val a0 = collapseAndCheckRank(a)
            val b0 = collapseAndCheckRank(b)
            b0.iterateMembers { v ->
                map.add(v.makeKey())
            }
            val result = ArrayList<APLValue>()
            a0.iterateMembers { v ->
                val key = v.makeKey()
                if (map.contains(key) && !leftKeys.contains(key)) {
                    result.add(v)
                    leftKeys.add(key)
                }
            }
            return APLArrayList(dimensionsOfSize(result.size), result)
        }

        private fun collapseAndCheckRank(a: APLValue): APLValue {
            val a1 = a.arrayify().collapse()
            if (a1.rank != 1) {
                throwAPLException(
                    InvalidDimensionsException(
                        "Argument to intersection must be a scalar or a 1-dimensional array",
                        pos))
            }
            return a1
        }

        override val name2Arg get() = "intersection"
    }

    override fun make(instantiation: FunctionInstantiation) = IntersectionAPLFunctionImpl(instantiation)
}

class CaseValue(val selectionArray: APLValue, val values: List<APLValue>, val pos: Position) : APLArray() {
    override val dimensions get() = selectionArray.dimensions

    override fun valueAt(p: Int): APLValue {
        val index = selectionArray.valueAtInt(p, pos)
        if (index < 0 || index >= values.size) {
            throwAPLException(InvalidDimensionsException("Attempt to read index ${index} from array (size=${values.size})", pos))
        }
        val v = values[index]
        return if (v.isScalar()) {
            v.disclose()
        } else {
            v.valueAt(p)
        }
    }
}

class CaseFunction : APLFunctionDescriptor {
    class CaseFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val bDimensions = b.dimensions
            if (bDimensions.size != 1) {
                throwAPLException(InvalidDimensionsException("Right argument must be a 1-dimensional array", pos))
            }
            val aDimensions = a.dimensions
            val values = b.membersSequence().map { v ->
                val d = v.dimensions
                unless(d.size == 0 || d.compareEquals(aDimensions)) {
                    throwAPLException(InvalidDimensionsException("Unmatched dimensions in selection list", pos))
                }
                v
            }.toList()
            return CaseValue(a, values, pos)
        }

        override val name2Arg get() = "case"
    }

    override fun make(instantiation: FunctionInstantiation) = CaseFunctionImpl(instantiation)
}

class AssignPrototypeFunction : APLFunctionDescriptor {
    class AssignPrototypeFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return OverriddenPrototypeValue(b, a)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = AssignPrototypeFunctionImpl(instantiation)
}

class OverriddenPrototypeValue(inner: APLValue, val prototype: APLValue) : DelegatedValue(inner) {
    override fun defaultValue() = prototype
}
