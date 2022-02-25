package array.gui.arrayedit

import array.*

class MutableAPLValue(value: APLValue) {
    var dimensions = value.dimensions
    var elements = value.membersSequence().toMutableList()
    var labels = value.labels

    fun valueAt(index: Int) = elements[index]

    fun updateValueAt(p: Int, v: APLValue) {
        elements[p] = v
    }

    fun makeAPLArray(): APLValue {
        assert(dimensions.contentSize() == elements.size)
        return APLArrayImpl(dimensions, elements.toTypedArray())
    }

    fun insert(axis: Int, index: Int, n: Int) {
        if (axis < 0 || axis >= dimensions.size) {
            throw java.lang.IllegalArgumentException("Invalid axis: ${axis}, available axis: ${dimensions.size}")
        }
        val newDimensions = Dimensions(IntArray(dimensions.size) { i ->
            val s = dimensions[i]
            if (axis == i) s + n else s
        })
        val newDimensionsMultipliers = newDimensions.multipliers()
        val newContent = ArrayList<APLValue>()
        repeat(newDimensions.contentSize()) { i ->
            val p = Dimensions.positionFromIndexWithMultipliers(i, newDimensionsMultipliers)
            val positionOnAxis = p[axis]
            val res = if (positionOnAxis >= index && positionOnAxis < index + n) {
                APLLONG_0
            } else {
                val offset = if (positionOnAxis >= index + n) index * newDimensionsMultipliers[axis] else 0
                elements[i - offset]
            }
            newContent.add(res)
        }
        dimensions = newDimensions
        elements = newContent
        labels = null
    }
}
