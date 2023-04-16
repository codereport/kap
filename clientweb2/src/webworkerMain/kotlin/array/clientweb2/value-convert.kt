package array.clientweb2

import array.*

fun formatValueToJs(value: APLValue): JsKapValue {
    return when (value) {
        is APLArray -> if (value.isStringValue()) JsKapString(value.toStringValue()) else formatArrayToJs(value)
        is APLLong -> JsKapInteger(value.formatted(FormatStyle.PLAIN))
        is APLBigInt -> JsKapInteger(value.formatted(FormatStyle.PLAIN))
        is APLDouble -> JsKapDouble(value.value)
        is APLChar -> JsKapChar(charToString(value.value))
        is APLList -> JsKapList(value.elements.map(::formatValueToJs).toList())
        else -> JsKapUndefined(value.formatted(FormatStyle.PRETTY))
    }
}

fun formatArrayToJs(value: APLArray): JsKapValue {
    return JsKapArray(value.dimensions.dimensions.toList(), value.membersSequence().map(::formatValueToJs).toList())
}
