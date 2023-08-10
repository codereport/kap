package array.builtins

import array.*

class MakeCodepoints : APLFunctionDescriptor {
    class MakeCodepointsImpl(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
        override fun combine1Arg(a: APLSingleValue): APLValue {
            return if (a is APLChar) {
                a.value.makeAPLNumber()
            } else {
                a
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = MakeCodepointsImpl(instantiation)
}

class MakeCharsFromCodepoints : APLFunctionDescriptor {
    class MakeCharsFromCodepointsImpl(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
        override fun combine1Arg(a: APLSingleValue): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> APLChar.fromLong(x, pos) },
                { x -> APLChar.fromLong(x.toLong(), pos) },
                { x ->
                    if (x.imaginary == 0.0) {
                        APLChar.fromLong(x.real.toLong(), pos)
                    } else {
                        throwAPLException(APLIllegalArgumentException("Complex numbers can't be represented as characters: ${x}", pos))
                    }
                },
                { x -> APLChar(x) })
        }
    }

    override fun make(instantiation: FunctionInstantiation) = MakeCharsFromCodepointsImpl(instantiation)
}

class GraphemesFunction : APLFunctionDescriptor {
    class GraphemesFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val graphemeList = a.toStringValue(pos).asGraphemeList()
            return APLArrayImpl(dimensionsOfSize(graphemeList.size), Array(graphemeList.size) { i ->
                APLString(graphemeList[i])
            })
        }
    }

    override fun make(instantiation: FunctionInstantiation) = GraphemesFunctionImpl(instantiation)
}

class ToLowerFunction : APLFunctionDescriptor {
    class ToLowerFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val s = a.toStringValue(pos)
            return APLString(s.lowercase())
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ToLowerFunctionImpl(instantiation)
}

class ToUpperFunction : APLFunctionDescriptor {
    class ToUpperFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val s = a.toStringValue(pos)
            return APLString(s.uppercase())
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ToUpperFunctionImpl(instantiation)
}

class ToNamesFunction : APLFunctionDescriptor {
    class ToNamesFunctionImpl(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
        override fun combine1Arg(a: APLSingleValue): APLValue {
            if (a is APLChar) {
                val name = codepointToName(a.value)
                return if (name == null) {
                    APLNullValue.APL_NULL_INSTANCE
                } else {
                    APLString(name)
                }
            } else {
                throwAPLException(IncompatibleTypeException("Value is not a char: ${a}", pos))
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ToNamesFunctionImpl(instantiation)
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
        engine.registerFunction(namespace.internAndExport("toNames"), ToNamesFunction())
    }
}
