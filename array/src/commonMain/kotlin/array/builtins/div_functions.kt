package array.builtins

import array.*

class TypeofFunction : APLFunctionDescriptor {
    class TypeofFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            return APLSymbol(context.engine.internSymbol(v.aplValueType.typeName, context.engine.coreNamespace))
        }

        override val name1Arg get() = "typeof"
    }

    override fun make(instantiation: FunctionInstantiation) = TypeofFunctionImpl(instantiation)
}

class InternalValueInfoFunction : APLFunctionDescriptor {
    class InternalValueInfoFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            return APLArrayImpl(
                dimensionsOfSize(3),
                arrayOf(APLString(v::class.simpleName ?: "unknown"), APLString(v.specialisedType.name), APLString(if (v.isDepth0) "1" else "0")))
        }
    }

    override fun make(instantiation: FunctionInstantiation) = InternalValueInfoFunctionImpl(instantiation)
}

class IsLocallyBoundFunction : APLFunctionDescriptor {
    class IsLocallyBoundFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            return makeBoolean(context.isLocallyBound(v.ensureSymbol(pos).value))
        }

        override val name1Arg get() = "isLocallyBound"
    }

    override fun make(instantiation: FunctionInstantiation) = IsLocallyBoundFunctionImpl(instantiation)
}

class CompFunction : APLFunctionDescriptor {
    class CompFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return a.collapse()
        }

        override val name1Arg get() = "comp"
    }

    override fun make(instantiation: FunctionInstantiation): APLFunction {
        return CompFunctionImpl(instantiation)
    }
}

class SystemParameterNotFound(val name: Symbol, pos: Position? = null) : APLEvalException("System parameter not found: ${name.nameWithNamespace}", pos)

class SystemParameterFunction : APLFunctionDescriptor {
    class SystemParameterFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return findSystemParameter(context, a).lookupValue()
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val param = findSystemParameter(context, a)
            val b0 = b.collapse()
            param.updateValue(b0, pos)
            return b0
        }

        private fun findSystemParameter(context: RuntimeContext, a: APLValue): SystemParameterProvider {
            val name = a.ensureSymbol(pos).value
            val param = context.engine.systemParameters[name] ?: throwAPLException(SystemParameterNotFound(name, pos))
            return param
        }
    }

    override fun make(instantiation: FunctionInstantiation) = SystemParameterFunctionImpl(instantiation)
}


/**
 * This value represents the result of a 1-arg function call which will only be performed
 * after the value is actually needed.
 */
class DeferredAPLValue1Arg(val fn: APLFunction, val context: RuntimeContext, val a: APLValue) : APLArray() {
    override val dimensions get() = a.dimensions
    override fun valueAt(p: Int) = fn.eval1Arg(context, a, null).valueAt(p)

    override fun unwrapDeferredValue(): APLValue {
        return fn.eval1Arg(context, a, null).unwrapDeferredValue()
    }
}

/**
 * This value represents the result of a 2-arg function call which will only be performed
 * after the value is actually needed.
 */
class DeferredAPLValue2Arg(val fn: APLFunction, val context: RuntimeContext, val a: APLValue, val b: APLValue) : APLArray() {
    override val dimensions get() = a.dimensions
    override fun valueAt(p: Int) = fn.eval2Arg(context, a, b, null).valueAt(p)

    override fun unwrapDeferredValue(): APLValue {
        return fn.eval2Arg(context, a, b, null).unwrapDeferredValue()
    }
}

class DeferAPLOperator : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: FunctionInstantiation): APLFunctionDescriptor {
        return DeferAPLFunction(fn)
    }

    class DeferAPLFunction(val fn: APLFunction) : APLFunctionDescriptor {
        override fun make(instantiation: FunctionInstantiation): APLFunction {
            return object : NoAxisAPLFunction(instantiation) {
                override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
                    return DeferredAPLValue1Arg(fn, context, a)
                }

                override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
                    return DeferredAPLValue2Arg(fn, context, a, b)
                }
            }
        }

    }
}

class TagCatch(
    val tag: APLValue, val data: APLValue, description: String? = null, pos: Position? = null
) : APLEvalException(description ?: data.formatted(FormatStyle.PLAIN), pos)

class UnwindProtectAPLFunction : APLFunctionDescriptor {
    class UnwindProtectAPLFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val aDimensions = a.dimensions
            if (aDimensions.size != 1 || aDimensions[0] != 2) {
                throwAPLException(APLEvalException("Invalid dimensions in unwindProtect call", pos))
            }
            val fn = a.valueAt(0).collapseFirstLevel()
            if (fn !is LambdaValue) {
                throwAPLException(APLEvalException("Handler function is not a lambda", pos))
            }
            val finallyHandler = a.valueAt(1).collapseFirstLevel()
            if (finallyHandler !is LambdaValue) {
                throwAPLException(APLEvalException("Unwind handler is not a lambda", pos))
            }
            var thrownException: APLEvalException? = null
            var result: APLValue? = null
            try {
                result = fn.makeClosure().eval1Arg(context, APLNullValue.APL_NULL_INSTANCE, null)
            } catch (e: APLEvalException) {
                thrownException = e
            }
            finallyHandler.makeClosure().eval1Arg(context, APLNullValue.APL_NULL_INSTANCE, null)
            if (thrownException != null) {
                throw thrownException
            }
            return result!!
        }
    }

    override fun make(instantiation: FunctionInstantiation) = UnwindProtectAPLFunctionImpl(instantiation)
}

class AtLeaveScopeOperator : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: FunctionInstantiation) = AtLeaveScopeFunctionDescriptor(fn)

    class AtLeaveScopeFunctionDescriptor(val fn: APLFunction) : APLFunctionDescriptor {
        override fun make(instantiation: FunctionInstantiation): APLFunction {
            return object : APLFunction(instantiation, listOf(fn)) {
                val fn get() = fns[0]

                override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                    currentStack().currentFrame().pushReleaseCallback {
                        fn.eval1Arg(context, a, null)
                    }
                    return a
                }
            }
        }
    }
}

class ThrowFunction : APLFunctionDescriptor {
    class ThrowFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val engine = context.engine
            throwAPLException(TagCatch(APLSymbol(engine.internSymbol("error", engine.coreNamespace)), a, null, pos))
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            throwAPLException(TagCatch(a, b, null, pos))
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ThrowFunctionImpl(instantiation)
}

class CatchOperator : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: FunctionInstantiation) = CatchFunctionDescriptor(fn)

    class CatchFunctionDescriptor(
        val fn1Descriptor: APLFunction
    ) : APLFunctionDescriptor {

        override fun make(instantiation: FunctionInstantiation): APLFunction {
            val fn = fn1Descriptor
            return object : NoAxisAPLFunction(instantiation) {
                override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
                    val dimensions = a.dimensions
                    unless((dimensions.size == 1 && dimensions[0] % 2 == 0) || (dimensions.size == 2 && dimensions[1] == 2)) {
                        throwAPLException(
                            APLIllegalArgumentException(
                                "Invalid dimensions of catch argument",
                                pos))
                    }
                    try {
                        return fn.eval1Arg(context, APLNullValue.APL_NULL_INSTANCE, null)
                    } catch (e: TagCatch) {
                        val sentTag = e.tag
                        val multipliers = dimensions.multipliers()
                        for (rowIndex in 0 until dimensions[0]) {
                            val checked = a.valueAt(dimensions.indexFromPosition(intArrayOf(rowIndex, 0), multipliers))
                            if (sentTag.compareEquals(checked)) {
                                val handlerFunction =
                                    a.valueAt(dimensions.indexFromPosition(intArrayOf(rowIndex, 1), multipliers)).unwrapDeferredValue()
                                if (handlerFunction !is LambdaValue) {
                                    throwAPLException(
                                        APLIllegalArgumentException("The handler is not callable, this is currently an error.", pos))
                                }
                                return handlerFunction.makeClosure().eval2Arg(context, e.data, sentTag, null)
                            }
                        }
                        throw e
                    }
                }
            }
        }
    }
}

class LabelsFunction : APLFunctionDescriptor {
    class LabelsFunctionImpl(pos: FunctionInstantiation) : APLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            if (!b.isScalar()) {
                val bDimensions = b.dimensions
                val axisInt = if (axis == null) bDimensions.lastAxis(pos) else axis.ensureNumber(pos).asInt(pos)
                ensureValidAxis(axisInt, bDimensions, pos)

                val aDimensions = a.dimensions
                if (aDimensions.size != 1) {
                    throwAPLException(InvalidDimensionsException("Left argument must be an array of labels", pos))
                }
                if (aDimensions[0] != bDimensions[axisInt]) {
                    throwAPLException(InvalidDimensionsException("Label list has incorrect length", pos))
                }
                val extraLabels = ArrayList<List<AxisLabel?>?>(bDimensions.size)
                repeat(bDimensions.size) { i ->
                    val v = if (i == axisInt) {
                        val labelsList = ArrayList<AxisLabel?>(bDimensions[axisInt])
                        repeat(bDimensions[axisInt]) { i2 ->
                            val value = a.valueAt(i2)
                            if (value.dimensions.size != 1) {
                                throwAPLException(InvalidDimensionsException("Label should be a single-dimensional array", pos))
                            }
                            labelsList.add(if (value.dimensions[0] == 0) null else AxisLabel(value.toStringValue()))
                        }
                        labelsList
                    } else {
                        null
                    }
                    extraLabels.add(v)
                }
                return LabelledArray.make(b, extraLabels)
            } else {
                throwAPLException(IncompatibleTypeException("Unable to set labels on non-array instances", pos))
            }
        }

        override val name2Arg get() = "labels"
    }

    override fun make(instantiation: FunctionInstantiation) = LabelsFunctionImpl(instantiation)
}

class ForcedElementTypeArray(val inner: APLValue, val overrideType: ArrayMemberType) : DelegatedValue(inner) {
    override val specialisedType get() = overrideType

    private fun maybeWrapValue(value: APLValue): APLValue {
        return if (value.specialisedType === overrideType) {
            value
        } else {
            object : DelegatedValue(value) {
                override val specialisedType get() = overrideType
            }
        }
    }

    override fun collapseInt(withDiscard: Boolean) = maybeWrapValue(inner.collapseInt())
    override fun unwrapDeferredValue() = maybeWrapValue(inner.unwrapDeferredValue())
}

class EnsureTypeFunction(val overrideType: ArrayMemberType) : APLFunctionDescriptor {
    inner class EnsureTypeFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return ForcedElementTypeArray(a, overrideType)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = EnsureTypeFunctionImpl(instantiation)
}

class AsBigintFunction : APLFunctionDescriptor {
    class AsBigintFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val a0 = a.unwrapDeferredValue()
            return when (a0) {
                is APLBigInt -> a0
                is APLLong -> APLBigInt(a0.asBigInt())
                else -> throwAPLException(IncompatibleTypeException("Argument is not an integer: ${a0.formatted(FormatStyle.PLAIN)}", pos))
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = AsBigintFunctionImpl(instantiation)
}

class ToListFunction : APLFunctionDescriptor {
    class ToListFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val a0 = a.arrayify()
            if (a0.dimensions.size != 1) {
                throwAPLException(InvalidDimensionsException("Argument must be a scalar or 1-dimensional array", pos))
            }
            val result = a0.membersSequence().toList()
            return APLList(result)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ToListFunctionImpl(instantiation)
}

class FromListFunction : APLFunctionDescriptor {
    class FromListFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val a0 = a.collapse()
            if (a0 !is APLList) {
                throwAPLException(APLIllegalArgumentException("Argument is not a list", pos))
            }
            val content = a0.elements
            val result = Array(content.size) { i -> content[i] }
            return APLArrayImpl(dimensionsOfSize(content.size), result)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = FromListFunctionImpl(instantiation)
}

class ReturnFunction : APLFunctionDescriptor {
    class ReturnFunctionImpl(pos: FunctionInstantiation, val returnEnvironment: Environment) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            throw ReturnValue(a, returnEnvironment, pos)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            if (a.asBoolean(pos)) {
                throw ReturnValue(b, returnEnvironment, pos)
            } else {
                return b
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation): ReturnFunctionImpl {
        val returnEnvironment = findReturnEnvironment(instantiation.env)
            ?: throw ParseException("Call to return without a function call", instantiation.pos)
        instantiation.env.returnTargets.add(instantiation)
        return ReturnFunctionImpl(instantiation, returnEnvironment)
    }

    private fun findReturnEnvironment(env: Environment): Environment? {
        var curr: Environment? = env
        while (curr != null) {
            if (curr.isReturnTarget) {
                return curr
            }
            curr = curr.parent
        }
        return null
    }
}

class ToBooleanFunction : APLFunctionDescriptor {
    class ToBooleanFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return if (a.asBoolean(pos)) APLLONG_1 else APLLONG_0
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ToBooleanFunctionImpl(instantiation)
}

class ListModulesFunction : APLFunctionDescriptor {
    class ListModulesFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val moduleNameList = context.engine.modules.map(KapModule::name).sorted()
            return APLArrayList(dimensionsOfSize(moduleNameList.size), moduleNameList.map(::APLString), false)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ListModulesFunctionImpl(instantiation)
}
