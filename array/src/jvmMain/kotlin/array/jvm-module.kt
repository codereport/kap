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

private inline fun <reified T : Any?> ensureJvmInstance(a: APLValue, pos: Position): T {
    val wrapped: JvmInstanceValue = ensureJvmGenericValue(a, pos)
    val instance = wrapped.instance
    if (instance !is T) {
        throwAPLException(APLIllegalArgumentException("Expected Java type: ${T::class.qualifiedName}, got: ${if (instance == null) "null" else instance::class.qualifiedName}"))
    }
    return instance
}

fun isNullKeyword(engine: Engine, v: APLValue): Boolean {
    return if (v is APLSymbol) {
        v.value == engine.standardSymbols.nullKeyword
    } else {
        false
    }
}

fun toJava(engine: Engine, a: APLValue): Any? {
    val a0 = a.unwrapDeferredValue()
    return when {
        a0 is JvmInstanceValue -> a0.instance
        a0 is APLLong -> a0.value
        a0.isStringValue() -> a0.toStringValue()
        isNullKeyword(engine, a0) -> null
        else -> throw IllegalArgumentException("Cannot convert to Java: ${a}")
    }
}

class FindMethodFunction : APLFunctionDescriptor {
    class FindMethodFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val cl = ensureJvmInstance<Class<*>>(a, pos)
            val b0 = b.unwrapDeferredValue().listify()
            val methodName = b0.listElement(0, pos).toStringValue(pos, "method name")
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
            val instance = toJava(context.engine, b0.listElement(0, pos))
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

class ToJvmShortFunction : APLFunctionDescriptor {
    class ToJvmShortFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val n = a.ensureNumber(pos).asLong(pos)
            if (n < Short.MIN_VALUE || n > Short.MAX_VALUE) {
                throwAPLException(KAPOverflowException("Value does not fit in short: ${n}"))
            }
            return JvmInstanceValue(n.toShort())
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ToJvmShortFunctionImpl(instantiation)
}

class ToJvmIntFunction : APLFunctionDescriptor {
    class ToJvmIntFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val n = a.ensureNumber(pos).asLong(pos)
            if (n < Int.MIN_VALUE || n > Int.MAX_VALUE) {
                throwAPLException(KAPOverflowException("Value does not fit in int: ${n}"))
            }
            return JvmInstanceValue(n.toInt())
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ToJvmIntFunctionImpl(instantiation)
}

class ToJvmLongFunction : APLFunctionDescriptor {
    class ToJvmLongFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val n = a.ensureNumber(pos).asLong(pos)
            return JvmInstanceValue(n)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ToJvmLongFunctionImpl(instantiation)
}

class ToJvmByteFunction : APLFunctionDescriptor {
    class ToJvmByteFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val n = a.ensureNumber(pos).asLong(pos)
            if (n < Byte.MIN_VALUE || n > Byte.MAX_VALUE) {
                throwAPLException(KAPOverflowException("Value does not fit in byte: ${n}"))
            }
            return JvmInstanceValue(n.toByte())
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ToJvmByteFunctionImpl(instantiation)
}

class ToJvmCharFunction : APLFunctionDescriptor {
    class ToJvmCharFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val n = a.ensureNumber(pos).asLong(pos)
            if (n < Char.MIN_VALUE.code || n > Char.MAX_VALUE.code) {
                throwAPLException(KAPOverflowException("Value does not fit in char: ${n}"))
            }
            return JvmInstanceValue(n.toInt().toChar())
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ToJvmCharFunctionImpl(instantiation)
}

class ToJvmFloatFunction : APLFunctionDescriptor {
    class ToJvmFloatFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val n = a.ensureNumber(pos).asDouble(pos)
            return JvmInstanceValue(n.toFloat())
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ToJvmFloatFunctionImpl(instantiation)
}

class ToJvmDoubleFunction : APLFunctionDescriptor {
    class ToJvmDoubleFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val n = a.ensureNumber(pos).asDouble(pos)
            return JvmInstanceValue(n)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ToJvmDoubleFunctionImpl(instantiation)
}

class ToJvmBooleanFunction : APLFunctionDescriptor {
    class ToJvmBooleanFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val n = a.asBoolean()
            return JvmInstanceValue(n)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ToJvmBooleanFunctionImpl(instantiation)
}

private fun javaObjToKap(engine: Engine, value: Any?, pos: Position): APLValue {
    return when (value) {
        null -> APLSymbol(engine.standardSymbols.nullKeyword)
        is Short -> value.toLong().makeAPLNumber()
        is Int -> value.makeAPLNumber()
        is Long -> value.makeAPLNumber()
        is Byte -> value.toInt().makeAPLNumber()
        is Float -> value.toDouble().makeAPLNumber()
        is Double -> value.makeAPLNumber()
        is String -> APLString(value)
        is ByteArray -> APLArrayLong(dimensionsOfSize(value.size), LongArray(value.size) { i -> value[i].toLong() })
        is IntArray -> APLArrayLong(dimensionsOfSize(value.size), LongArray(value.size) { i -> value[i].toLong() })
        is LongArray -> APLArrayLong(dimensionsOfSize(value.size), value.copyOf())
        is FloatArray -> APLArrayDouble(dimensionsOfSize(value.size), DoubleArray(value.size) { i -> value[i].toDouble() })
        is DoubleArray -> APLArrayDouble(dimensionsOfSize(value.size), value.copyOf())
        is Array<*> -> APLArrayImpl(dimensionsOfSize(value.size), value.map { v -> javaObjToKap(engine, v, pos) }.toTypedArray())
        is List<*> -> APLArrayImpl(dimensionsOfSize(value.size), value.map { v -> javaObjToKap(engine, v, pos) }.toTypedArray())
        else -> throwAPLException(APLIllegalArgumentException("Unexpected JVM type: ${value::class.qualifiedName}"))
    }
}

class FromJvmFunction : APLFunctionDescriptor {
    class FromJvmFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val a0 = ensureJvmInstance<Any?>(a, pos)
            return javaObjToKap(context.engine, a0, pos)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = FromJvmFunctionImpl(instantiation)
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
        engine.registerFunction(ns.internAndExport("toJvmShort"), ToJvmShortFunction())
        engine.registerFunction(ns.internAndExport("toJvmInt"), ToJvmIntFunction())
        engine.registerFunction(ns.internAndExport("toJvmLong"), ToJvmLongFunction())
        engine.registerFunction(ns.internAndExport("toJvmByte"), ToJvmByteFunction())
        engine.registerFunction(ns.internAndExport("toJvmChar"), ToJvmCharFunction())
        engine.registerFunction(ns.internAndExport("toJvmFloat"), ToJvmFloatFunction())
        engine.registerFunction(ns.internAndExport("toJvmDouble"), ToJvmDoubleFunction())
        engine.registerFunction(ns.internAndExport("toJvmBoolean"), ToJvmBooleanFunction())
        engine.registerFunction(ns.internAndExport("fromJvm"), FromJvmFunction())
    }
}
