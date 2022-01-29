package array.gui.arrayedit

import array.APLValue
import array.Dimensions
import array.membersSequence
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class MutableAPLValue(value: APLValue) {
    private val dimensionsProperty: ObjectProperty<Dimensions> = SimpleObjectProperty(value.dimensions)
    val elements: ObservableList<APLValue> = FXCollections.observableArrayList(value.membersSequence().toList())

    val dimensions: Dimensions by delegatedProperty(dimensionsProperty)

    private fun delegatedProperty(prop: ObjectProperty<Dimensions>): ReadWriteProperty<MutableAPLValue, Dimensions> {
        return object : ReadWriteProperty<MutableAPLValue, Dimensions> {
            override fun getValue(thisRef: MutableAPLValue, property: KProperty<*>): Dimensions {
                return prop.get()
            }

            override fun setValue(thisRef: MutableAPLValue, property: KProperty<*>, value: Dimensions) {
                prop.set(value)
            }
        }
    }
}
