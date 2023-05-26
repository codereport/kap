package array

class CustomRendererParameter(val engine: Engine) : SystemParameterProvider {
    override fun lookupValue(): APLValue {
        return engine.customRenderer ?: APLNullValue.APL_NULL_INSTANCE
    }

    override fun updateValue(newValue: APLValue, pos: Position) {
        val v = newValue.collapse()
        val res = when {
            v.dimensions.isNullDimensions() -> null
            v is LambdaValue -> v
            else -> throwAPLException(APLIllegalArgumentException("Argument must be a lambda value", pos))
        }
        engine.customRenderer = res
    }
}

fun formatResult(value: APLValue, fn: (String) -> Unit) {
    val d = value.dimensions
    when (d.size) {
        2 -> {
            // This is a two-dimensional array of characters
            var i = 0
            repeat(d[0]) {
                val buf = StringBuilder()
                repeat(d[1]) {
                    val ch = value.valueAt(i++)
                    assertx(ch is APLChar)
                    val code = ch.value
                    buf.append(charToString(code))
                }
                fn(buf.toString())
            }
        }
        1 -> {
            // This is a one-dimensional array of strings
            repeat(d[0]) { i ->
                val s = value.valueAt(i).toStringValue()
                fn(s)
            }
        }
        else -> {
            throw IllegalArgumentException("Invalid result format: ${value.dimensions}")
        }
    }
}

fun formatResultToStrings(value: APLValue): List<String> {
    val result = ArrayList<String>()
    formatResult(value, result::add)
    return result
}
