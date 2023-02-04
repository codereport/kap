package array.builtins

import array.*

class ExpandValue(override val dimensions: Dimensions, val axis: Int, val value: APLValue, val indexes: IntArray) : APLArray() {
    private val multipliers = dimensions.multipliers()
    private val valueMultipliers = value.dimensions.multipliers()

    override fun valueAt(p: Int): APLValue {
        val position = multipliers.positionFromIndex(p)
        val index = indexes[position[axis]]
        return if (index == -1) {
            APLLONG_0
        } else {
            position[axis] = index
            value.valueAt(dimensions.indexFromPosition(position, valueMultipliers))
        }
    }
}

abstract class ExpandFunctionImpl(pos: FunctionInstantiation) : APLFunction(pos) {
    abstract fun defaultAxis(dimensions: Dimensions): Int

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        val a0 = a.arrayify()
        val aDimensions = a0.dimensions
        if (aDimensions.size != 1) {
            throwAPLException(InvalidDimensionsException("A must be a scalar or 1-dimensional array"))
        }

        val b0 = b.arrayify()
        val bDimensions = b0.dimensions
        val axisInt = if (axis == null) defaultAxis(bDimensions) else axis.ensureNumber(pos).asInt(pos)
        ensureValidAxis(axisInt, bDimensions, pos)

        val dimensionAlongAxis = bDimensions[axisInt]
        var index = 0
        val indexes = ArrayList<Int>()
        a0.iterateMembers { v ->
            val vInt = v.ensureNumber(pos).asInt(pos)
            if (vInt > 0) {
                repeat(vInt) {
                    indexes.add(index)
                }
                if (dimensionAlongAxis > 1) {
                    index++
                }
            } else {
                val n = if (vInt == 0) 1 else -vInt
                repeat(n) {
                    indexes.add(-1)
                }
            }
        }

        if (dimensionAlongAxis != index && dimensionAlongAxis != 1) {
            throwAPLException(
                InvalidDimensionsException(
                    "Size of selection dimension in B must match the number of selected values in A",
                    pos))
        }

        val resultDimensions = bDimensions.replace(axisInt, indexes.size)

        return ExpandValue(resultDimensions, axisInt, b0, indexes.toIntArray())
    }
}

class ExpandFirstAxisFunction : APLFunctionDescriptor {
    class ExpandFirstAxisFunctionImpl(pos: FunctionInstantiation) : ExpandFunctionImpl(pos) {
        override fun defaultAxis(dimensions: Dimensions) = 0

        override val name2Arg get() = "expand first axis"
    }

    override fun make(instantiation: FunctionInstantiation) = ExpandFirstAxisFunctionImpl(instantiation)
}

class ExpandLastAxisFunction : APLFunctionDescriptor {
    class ExpandLastAxisFunctionImpl(pos: FunctionInstantiation) : ExpandFunctionImpl(pos) {
        override fun defaultAxis(dimensions: Dimensions) = dimensions.lastAxis(pos)

        override val name2Arg get() = "expand last axis"
    }

    override fun make(instantiation: FunctionInstantiation) = ExpandLastAxisFunctionImpl(instantiation)
}
