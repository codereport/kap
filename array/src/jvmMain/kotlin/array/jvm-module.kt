package array.jvmmod

import array.*
import java.lang.reflect.Constructor
import java.lang.reflect.Method

class JvmModuleException(message: String, pos: Position? = null) : APLEvalException(message, pos)

class JvmInstanceValue(val instance: Any?) : APLSingleValue() {
    override val aplValueType: APLValueType get() = APLValueType.INTERNAL

    override fun formatted(style: FormatStyle): String {
        return instance.toString()
    }

    override fun compareEquals(reference: APLValue): Boolean {
        if (reference !is JvmInstanceValue) {
            return false
        }
        return instance == reference.instance
    }

    override fun makeKey(): APLValueKey {
        return APLValueKeyImpl(this, instance as Any)
    }
}

class FindClassFunction : APLFunctionDescriptor {
    class FindClassFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val className = a.toStringValue(pos)
            val cl = try {
                Class.forName(className)
            } catch (e: ClassNotFoundException) {
                throwAPLException(JvmModuleException("Class not found: ${className}", pos))
            }
            return JvmInstanceValue(cl)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = FindClassFunctionImpl(instantiation)
}

private fun ensureJvmGenericValue(a: APLValue, pos: Position): JvmInstanceValue {
    val a0 = a.unwrapDeferredValue()
    if (a0 !is JvmInstanceValue) {
        throwAPLException(APLIncompatibleDomainsException("Expected JVM instance", pos))
    }
    return a0
}

private inline fun <reified T : Any> ensureJvmInstance(a: APLValue, pos: Position): T {
    val wrapped: JvmInstanceValue = ensureJvmGenericValue(a, pos)
    val instance = wrapped.instance
    if (instance !is T) {
        throwAPLException(APLIllegalArgumentException("Expected Java type: ${T::class.qualifiedName}, got: ${if (instance == null) "null" else instance::class.qualifiedName}"))
    }
    return instance
}

fun toJava(engine: Engine, a: APLValue): Any? {
    val a0 = a.unwrapDeferredValue()

    fun isNullKeyword(): Boolean {
        return if (a0 is APLSymbol) {
            val sym = a0.value
            sym.namespace === engine.keywordNamespace && sym.symbolName == "null"
        } else {
            false
        }
    }

    return when {
        a0 is JvmInstanceValue -> a0.instance
        a0 is APLLong -> a0.value
        a0.isStringValue() -> a0.toStringValue()
        isNullKeyword() -> null
        else -> throw IllegalArgumentException("Cannot convert to Java: ${a}")
    }
}

class FindMethodFunction : APLFunctionDescriptor {
    class FindMethodFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val cl = ensureJvmInstance<Class<*>>(a, pos)
            val b0 = b.unwrapDeferredValue().listify()
            val methodName = b0.listElement(0).toStringValue(pos, "method name")
            val method = try {
                cl.getMethod(methodName, *b0.elements.drop(1).map { v -> ensureJvmInstance<Class<*>>(v, pos) }.toTypedArray())
            } catch (e: NoSuchMethodException) {
                throwAPLException(JvmModuleException("Method not found: ${methodName}"))
            }
            return JvmInstanceValue(method)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = FindMethodFunctionImpl(instantiation)
}

class FindConstructorFunction : APLFunctionDescriptor {
    class FindConstructorFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val cl = ensureJvmInstance<Class<*>>(a, pos)
            val b0 = b.listify()
            val argTypes = b0.elements.map { v -> ensureJvmInstance<Class<*>>(v, pos) }.toTypedArray()
            val constructor = try {
                cl.getConstructor(*argTypes)
            } catch (e: NoSuchMethodException) {
                throwAPLException(JvmModuleException("Constructor not found with args: ${argTypes}", pos))
            }
            return JvmInstanceValue(constructor)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = FindConstructorFunctionImpl(instantiation)
}

class CallMethodFunction : APLFunctionDescriptor {
    class CallMethodFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val method = ensureJvmInstance<Method>(a, pos)
            val b0 = b.listify()
            val instance = toJava(context.engine, b0.listElement(0))
            val result = method.invoke(instance, *b0.elements.drop(1).map { v -> toJava(context.engine, v) }.toTypedArray())
            return JvmInstanceValue(result)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = CallMethodFunctionImpl(instantiation)
}

class CreateInstanceFunction : APLFunctionDescriptor {
    class CreateInstanceFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val constructor = ensureJvmInstance<Constructor<*>>(a, pos)
            val args = b.listify().elements.map { v -> ensureJvmInstance<Any>(v, pos) }.toTypedArray()
            val result = constructor.newInstance(*args)
            return JvmInstanceValue(result)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = CreateInstanceFunctionImpl(instantiation)
}

class ToJvmStringFunction : APLFunctionDescriptor {
    class ToJvmStringFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val s = a.toStringValue(pos)
            return JvmInstanceValue(s)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ToJvmStringFunctionImpl(instantiation)
}

class JvmModule : KapModule {
    override val name get() = "jvm"

    override fun init(engine: Engine) {
        val ns = engine.makeNamespace("jvm")
        engine.registerFunction(ns.internAndExport("findClass"), FindClassFunction())
        engine.registerFunction(ns.internAndExport("findMethod"), FindMethodFunction())
        engine.registerFunction(ns.internAndExport("findConstructor"), FindConstructorFunction())
        engine.registerFunction(ns.internAndExport("callMethod"), CallMethodFunction())
        engine.registerFunction(ns.internAndExport("createInstance"), CreateInstanceFunction())
        engine.registerFunction(ns.internAndExport("toJvmString"), ToJvmStringFunction())
    }
}
