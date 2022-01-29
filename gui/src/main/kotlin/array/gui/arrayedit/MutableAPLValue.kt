package array.gui.arrayedit

import array.APLArrayImpl
import array.APLValue
import array.membersSequence

class MutableAPLValue(value: APLValue) {
    val dimensions = value.dimensions
    val elements = value.membersSequence().toMutableList()
    val labels = value.labels

    fun makeAPLArray(): APLValue {
        assert(dimensions.contentSize() == elements.size)
        return APLArrayImpl(dimensions, elements.toTypedArray())
    }
}
