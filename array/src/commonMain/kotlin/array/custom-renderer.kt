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
