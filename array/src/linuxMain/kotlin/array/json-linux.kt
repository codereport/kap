@file:OptIn(ExperimentalForeignApi::class)

package array.json

import array.*
import jansson.*
import kotlinx.cinterop.*

actual val backendSupportsJson = true

actual fun parseJsonToAPL(input: CharacterProvider): APLValue {
    val buf = StringBuilder()
    input.lines().forEach { line ->
        buf.append(line)
        buf.append("\n")
    }
    memScoped {
        val error = alloc<json_error_t>()
        val root = json_loads(buf.toString(), 0U, error.ptr) ?: throw JsonParseException("Error parsing JSON: ${error.text.toKString()}")
        try {
            return parseEntry(root)
        } finally {
            json_decref(root)
        }
    }
}

private fun parseEntry(value: CPointer<json_t>): APLValue {
    return when (val type = jsonTypeof(value)) {
        json_type.JSON_ARRAY -> parseArray(value)
        json_type.JSON_OBJECT -> parseObject(value)
        json_type.JSON_REAL -> json_real_value(value).makeAPLNumber()
        json_type.JSON_INTEGER -> json_integer_value(value).makeAPLNumber()
        json_type.JSON_STRING -> parseString(value)
        json_type.JSON_TRUE -> APLLONG_1
        json_type.JSON_FALSE -> APLLONG_0
        json_type.JSON_NULL -> APLNullValue.APL_NULL_INSTANCE
        else -> throw JsonParseException("Unexpected json type: ${type}")
    }
}

private fun parseArray(obj: CPointer<json_t>): APLValue {
    val content = ArrayList<APLValue>()
    val size = json_array_size(obj)
    if (size > Int.MAX_VALUE.toUInt()) {
        throw JsonParseException("Array too large: ${size}")
    }
    for (i in 0 until size.toInt()) {
        val v = json_array_get(obj, i.toULong()) ?: throw JsonParseException("Null value from array at index: ${i}")
        content.add(parseEntry(v))
    }
    return APLArrayList(dimensionsOfSize(content.size), content)
}

private fun parseObject(obj: CPointer<json_t>): APLValue {
    val content = ArrayList<Pair<APLValue.APLValueKey, APLValue>>()
    var iterator = json_object_iter(obj)
    while (iterator != null) {
        val key = json_object_iter_key(iterator) ?: throw JsonParseException("Null key from object iterator")
        val value = json_object_iter_value(iterator) ?: throw JsonParseException("Null value from object iterator")
        content.add(APLString(key.toKString()).makeKey() to parseEntry(value))
        iterator = json_object_iter_next(obj, iterator)
    }
    return APLMap(ImmutableMap2.makeFromContent(content))
}

private fun parseString(obj: CPointer<json_t>): APLValue {
    val stringVal = json_string_value(obj) ?: throw JsonParseException("Null value from string")
    return APLString(stringVal.toKString())
}
