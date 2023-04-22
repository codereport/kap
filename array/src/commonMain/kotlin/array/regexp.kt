package array

class InvalidRegexp(message: String, pos: Position? = null) : APLEvalException(message, pos)

private fun regexpFromValue(a: APLValue, pos: Position): Regex {
    return if (a is RegexpMatcherValue) {
        a.matcher
    } else {
        val regexpString = a.toStringValue(pos)
        try {
            toRegexpWithException(regexpString, emptySet())
        } catch (e: RegexpParseException) {
            throwAPLException(InvalidRegexp("Invalid format: ${regexpString}", pos))
        }
    }
}

class RegexpMatchesFunction : APLFunctionDescriptor {
    class RegexpMatchesFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val matchString = b.toStringValue(pos)
            val regexp = regexpFromValue(a, pos)
            return if (regexp.find(matchString) != null) APLLONG_1 else APLLONG_0
        }

        override val name2Arg get() = "match regexp"
    }

    override fun make(instantiation: FunctionInstantiation) = RegexpMatchesFunctionImpl(instantiation)
}

class RegexpFindFunction : APLFunctionDescriptor {
    class RegexpFindFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val matchString = b.toStringValue(pos)
            val regexp = regexpFromValue(a, pos)
            val result = regexp.find(matchString) ?: return APLNullValue.APL_NULL_INSTANCE
            return makeAPLValueFromGroups(result, context)
        }

        override val name2Arg get() = "find regexp"
    }

    override fun make(instantiation: FunctionInstantiation) = RegexpFindFunctionImpl(instantiation)
}

private fun makeAPLValueFromGroups(result: MatchResult, context: RuntimeContext): APLArrayImpl {
    val groups = result.groups
    var undefinedSym: Symbol? = null
    return APLArrayImpl(dimensionsOfSize(groups.size), Array(groups.size) { i ->
        val v = groups.get(i)
        assertx(!(i == 0 && v == null))
        if (v == null) {
            if (undefinedSym == null) {
                undefinedSym = context.engine.keywordNamespace.internSymbol("undefined")
            }
            APLSymbol(undefinedSym!!)
        } else {
            APLString(v.value)
        }
    })
}

class RegexpSplitFunction : APLFunctionDescriptor {
    class RegexpSplitFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val matchString = b.toStringValue(pos)
            val regexp = regexpFromValue(a, pos)
            val result = regexp.split(matchString)
            return APLArrayList(dimensionsOfSize(result.size), result.map(::APLString))
        }

        override val name2Arg get() = "split regexp"
    }

    override fun make(instantiation: FunctionInstantiation) = RegexpSplitFunctionImpl(instantiation)
}

class RegexpMatcherValue(val matcher: Regex) : APLSingleValue() {
    override val aplValueType get() = APLValueType.INTERNAL
    override fun formatted(style: FormatStyle) = "regexp-matcher"
    override fun compareEquals(reference: APLValue) = reference is RegexpMatcherValue && matcher == reference.matcher
    override fun makeKey(): APLValueKey = APLValueKeyImpl(this, matcher)
}

class CreateRegexpFunction : APLFunctionDescriptor {
    class CreateRegexpFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return RegexpMatcherValue(toRegexpWithException(a.toStringValue(pos), emptySet()))
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            fun mkFlag(v: APLValue) = valueToFlag(context.engine, v)
            val flags = when (a.dimensions.size) {
                0 -> setOf(mkFlag(a))
                1 -> a.membersSequence().map(::mkFlag).toSet()
                else -> throwAPLException(APLEvalException("Regexp flags must be a single symbol or a one-dimensional array", pos))
            }
            return RegexpMatcherValue(toRegexpWithException(b.toStringValue(pos), flags))
        }

        private fun valueToFlag(engine: Engine, v: APLValue): RegexOption {
            val s = v.unwrapDeferredValue()
            if (s !is APLSymbol) {
                throwAPLException(APLEvalException("Regexp flag must be a symbol"))
            }
            val sym = s.value
            return when {
                sym === engine.keywordNamespace.internSymbol("ignoreCase") -> RegexOption.IGNORE_CASE
                sym === engine.keywordNamespace.internSymbol("multiLine") -> RegexOption.MULTILINE
                else -> throwAPLException(APLEvalException("Unknown regexp flag: ${sym.symbolName}"))
            }
        }

        override val name1Arg get() = "create regexp"
    }

    override fun make(instantiation: FunctionInstantiation) = CreateRegexpFunctionImpl(instantiation)
}

class RegexpReplaceFunction : APLFunctionDescriptor {
    class RegexpReplaceFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val regexp = regexpFromValue(a, pos)
            val args = b.listify()
            if (args.listSize() != 2) {
                throwAPLException(IllegalArgumentNumException(2, args.listSize(), pos))
            }
            val matchString = args.listElement(0).toStringValue(pos)
            val replacementArg = args.listElement(1).collapse()
            val s = when {
                replacementArg.isStringValue() -> {
                    regexp.replace(matchString, replacementArg.toStringValue(pos))
                }
                replacementArg is LambdaValue -> {
                    val replacementClosure = replacementArg.makeClosure()
                    regexp.replace(matchString) { r ->
                        replacementClosure.eval1Arg(context, makeAPLValueFromGroups(r, context), null).toStringValue(pos)
                    }
                }
                else -> {
                    throwAPLException(APLIllegalArgumentException("Replacement must be either a string or a lambda"))
                }
            }
            return APLString(s)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = RegexpReplaceFunctionImpl(instantiation)
}

class RegexpModule : KapModule {
    override val name get() = "regex"

    override fun init(engine: Engine) {
        val namespace = engine.makeNamespace("regex")
        fun registerFn(name: String, fn: APLFunctionDescriptor) {
            engine.registerFunction(namespace.internAndExport(name), fn)
        }
        registerFn("match", RegexpMatchesFunction())
        registerFn("find", RegexpFindFunction())
        registerFn("create", CreateRegexpFunction())
        registerFn("split", RegexpSplitFunction())
        registerFn("replace", RegexpReplaceFunction())
    }
}
