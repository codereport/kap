package array.clientweb2

import array.*
import com.dhsdevelopments.mpbignum.BigInt
import com.dhsdevelopments.mpbignum.of
import com.dhsdevelopments.mpbignum.rangeInLong
import com.dhsdevelopments.mpbignum.toLong

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

fun formatJsToValue(value: JsKapValue): APLValue {
    return when (value) {
        is JsKapString -> APLString(value.value)
        is JsKapChar -> APLChar(stringToCode(value.value))
        is JsKapInteger -> stringToKapInteger(value.value)
        is JsKapDouble -> APLDouble(value.value)
        is JsKapArray -> processKapArray(value)
        is JsKapList -> APLList(value.values.map(::formatJsToValue))
        is JsKapUndefined -> APLString("no value")
    }
}

private fun stringToCode(s: String): Int {
    return when (s.length) {
        1 -> s[0].code
        2 -> {
            if ((s[0].code !in 0xD800..0xDBFF) || (s[1].code !in 0xDC00..0xDFFF)) {
                0xFFFD
            } else {
                makeCharFromSurrogatePair(s[0], s[1])
            }
        }
        else -> 0xFFFD // replacement char
    }
}

private fun stringToKapInteger(s: String): APLNumber {
    val b = BigInt.of(s)
    return if (b.rangeInLong()) {
        APLLong(b.toLong())
    } else {
        APLBigInt(b)
    }
}

fun processKapArray(array: JsKapArray): APLValue {
    val values = array.values
    val d = Dimensions(array.dimensions.toIntArray())
    return APLArrayImpl(d, Array(d.contentSize()) { i -> formatJsToValue(values[i]) })
}
