package array.builtins

import array.*

private fun toUnicodeCodepoint(value: APLValue): APLValue {
    return when (val v = value.unwrapDeferredValue()) {
        is APLChar -> v.value.makeAPLNumber()
        is APLSingleValue -> v
        else -> ToUnicodeValue(v)
    }
}

private class ToUnicodeValue(val value: APLValue) : APLArray() {
    override val dimensions = value.dimensions

    override fun valueAt(p: Int): APLValue {
        return toUnicodeCodepoint(value.valueAt(p))
    }
}

class MakeCodepoints : APLFunctionDescriptor {
    class MakeCodepointsImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return toUnicodeCodepoint(a)
        }
    }

    override fun make(pos: Position) = MakeCodepointsImpl(pos.withName("toCodepoints"))
}

private fun fromUnicodeCodepoint(value: APLValue): APLValue {
    return when (val v = value.unwrapDeferredValue()) {
        is APLNumber -> APLChar(v.asInt())
        is APLSingleValue -> v
        else -> FromUnicodeValue(v)
    }
}

private class FromUnicodeValue(val value: APLValue) : APLArray() {
    override val dimensions = value.dimensions

    override fun valueAt(p: Int): APLValue {
        return fromUnicodeCodepoint(value.valueAt(p))
    }
}

class MakeCharsFromCodepoints : APLFunctionDescriptor {
    class MakeCharsFromCodepointsImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return fromUnicodeCodepoint(a)
        }
    }

    override fun make(pos: Position) = MakeCharsFromCodepointsImpl(pos.withName("fromCodepoints"))
}

class GraphemesFunction : APLFunctionDescriptor {
    class GraphemesFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val graphemeList = a.toStringValue(pos).asGraphemeList()
            return APLArrayImpl(dimensionsOfSize(graphemeList.size), Array(graphemeList.size) { i ->
                APLString(graphemeList[i])
            })
        }
    }

    override fun make(pos: Position) = GraphemesFunctionImpl(pos.withName("toGraphemes"))
}

class ToLowerFunction : APLFunctionDescriptor {
    class ToLowerFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val s = a.toStringValue(pos)
            return APLString(s.lowercase())
        }
    }

    override fun make(pos: Position) = ToLowerFunctionImpl(pos.withName("toLower"))
}

class ToUpperFunction : APLFunctionDescriptor {
    class ToUpperFunctionImpl(pos: Position) : NoAxisAPLFunction(pos.withName("toUpper")) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val s = a.toStringValue(pos)
            return APLString(s.uppercase())
        }
    }

    override fun make(pos: Position) = ToUpperFunctionImpl(pos)
}


class UnicodeModule : KapModule {
    override val name get() = "unicode"

    override fun init(engine: Engine) {
        val namespace = engine.makeNamespace("unicode")
        engine.registerFunction(namespace.internAndExport("toCodepoints"), MakeCodepoints())
        engine.registerFunction(namespace.internAndExport("fromCodepoints"), MakeCharsFromCodepoints())
        engine.registerFunction(namespace.internAndExport("toGraphemes"), GraphemesFunction())
        engine.registerFunction(namespace.internAndExport("toLower"), ToLowerFunction())
        engine.registerFunction(namespace.internAndExport("toUpper"), ToUpperFunction())
    }
}
