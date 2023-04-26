package array

class KapClassNotFound(name: Symbol, pos: Position? = null) : APLEvalException("Class not found: ${name.nameWithNamespace}", pos)

interface KapClass {
    val name: Symbol
}

class SystemClass(override val name: Symbol) : KapClass

class UserDefinedClass(override val name: Symbol) : KapClass

class ClassManager(val engine: Engine) {
    val systemArrayClass by lazy { SystemClass(engine.internSymbol("array", engine.coreNamespace)) }
    val registeredClasses = HashMap<Symbol, KapClass>()

    fun registerClass(cl: UserDefinedClass) {
        registeredClasses.put(cl.name, cl)
    }

    fun init() {
        val ns = engine.makeNamespace("objects")
        engine.registerFunction(engine.internSymbol("defclass", ns), DefclassFunctionDescriptor())
        engine.registerFunction(engine.internSymbol("make", ns), MakeClassInstanceFunctionDescriptor())
        engine.registerFunction(engine.internSymbol("classof", ns), TagOfFunctionDescriptor())
        engine.registerFunction(engine.internSymbol("extract", ns), ExtractFunctionDescriptor())
    }
}

class DefclassFunctionDescriptor : APLFunctionDescriptor {
    class DefclassFunctionImpl(instantiation: FunctionInstantiation) : NoAxisAPLFunction(instantiation) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val a0 = a.listify()
            val name = a0.listElement(0).ensureSymbol(pos)
            context.engine.classManager.registerClass(UserDefinedClass(name.value))
            return name
        }
    }

    override fun make(instantiation: FunctionInstantiation) = DefclassFunctionImpl(instantiation)
}

class MakeClassInstanceFunctionDescriptor : APLFunctionDescriptor {
    class MakeClassInstanceFunctionImpl(instantiation: FunctionInstantiation) : NoAxisAPLFunction(instantiation) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val className = a.ensureSymbol(pos).value
            val cl = context.engine.classManager.registeredClasses[className] ?: throwAPLException(KapClassNotFound(className, pos))
            return TypedAPLValue(b, cl)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = MakeClassInstanceFunctionImpl(instantiation)
}

class TagOfFunctionDescriptor : APLFunctionDescriptor {
    class TagOfFunctionImpl(instantiation: FunctionInstantiation) : NoAxisAPLFunction(instantiation) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val cl = a.kapClass ?: context.engine.classManager.systemArrayClass
            return APLSymbol(cl.name)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = TagOfFunctionImpl(instantiation)
}

class ExtractFunctionDescriptor : APLFunctionDescriptor {
    class ExtractFunctionImpl(instantiation: FunctionInstantiation) : NoAxisAPLFunction(instantiation) {
        private fun ensureTypedAPLValue(a: APLValue): TypedAPLValue {
            val a0 = a.unwrapDeferredValue()
            if (a0 !is TypedAPLValue) {
                throwAPLException(APLIncompatibleDomainsException("Value is not a class instance: ${a.aplValueType.typeName}"))
            }
            return a0
        }

        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return ensureTypedAPLValue(a).delegate
        }

        override fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue): APLValue {
            val a0 = ensureTypedAPLValue(a)
            val result = baseFn.eval1Arg(context, a0.delegate, null)
            return TypedAPLValue(result, a0.kapClass)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ExtractFunctionImpl(instantiation)
}

class TypedAPLValue(val delegate: APLValue, override val kapClass: KapClass) : APLSingleValue() {
    override val aplValueType get() = APLValueType.OBJECT
    override fun formatted(style: FormatStyle) = "instance"

    override fun compareEquals(reference: APLValue): Boolean {
        TODO("Not yet implemented")
    }

    override fun makeKey(): APLValueKey {
        TODO("Not yet implemented")
    }
}
