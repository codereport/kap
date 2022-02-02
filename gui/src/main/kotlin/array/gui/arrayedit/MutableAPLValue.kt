package array.gui.arrayedit

import array.APLArrayImpl
import array.APLValue
import array.membersSequence

class MutableAPLValue(value: APLValue) {
    val dimensions = value.dimensions
    val elements = value.membersSequence().toMutableList()
    val labels = value.labels

    fun valueAt(index: Int) = elements[index]

    fun updateValueAt(p: Int, v: APLValue) {
        elements[p] = v
    }

    fun makeAPLArray(): APLValue {
        assert(dimensions.contentSize() == elements.size)
        return APLArrayImpl(dimensions, elements.toTypedArray())
    }
}
