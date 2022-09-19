package array

abstract class APLFunction(val pos: Position, val fns: List<APLFunction> = emptyList()) {
    open fun evalArgsAndCall1Arg(context: RuntimeContext, rightArgs: Instruction): APLValue {
        val rightValue = rightArgs.evalWithContext(context)
        return eval1Arg(context, rightValue, null)
    }

    open fun evalArgsAndCall2Arg(context: RuntimeContext, leftArgs: Instruction, rightArgs: Instruction): APLValue {
        val rightValue = rightArgs.evalWithContext(context)
        val leftValue = leftArgs.evalWithContext(context)
        return eval2Arg(context, leftValue, rightValue, null)
    }

    open fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue =
        throwAPLException(Unimplemented1ArgException(pos))

    open fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue =
        throwAPLException(Unimplemented2ArgException(pos))

    open fun identityValue(): APLValue =
        throwAPLException(APLIncompatibleDomainsException("Function does not have an identity value", pos))

    open fun deriveBitwise(): APLFunctionDescriptor? = null

    open val optimisationFlags get() = OptimisationFlags(0)

    open fun eval1ArgLong(context: RuntimeContext, a: Long, axis: APLValue?): Long =
        throw IllegalStateException("Illegal call to specialised function: ${this::class.simpleName}")

    open fun eval1ArgDouble(context: RuntimeContext, a: Double, axis: APLValue?): Double =
        throw IllegalStateException("Illegal call to specialised function: ${this::class.simpleName}")

    open fun eval2ArgLongLong(context: RuntimeContext, a: Long, b: Long, axis: APLValue?): Long =
        throw IllegalStateException("Illegal call to specialised function: ${this::class.simpleName}")

    open fun eval2ArgDoubleDouble(context: RuntimeContext, a: Double, b: Double, axis: APLValue?): Double =
        throw IllegalStateException("Illegal call to specialised function: ${this::class.simpleName}")

    open fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue =
        throwAPLException(InverseNotAvailable(pos))

    open fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue =
        throwAPLException(InverseNotAvailable(pos))

    open fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue =
        throwAPLException(InverseNotAvailable(pos))

    open fun computeClosure(parser: APLParser): Pair<APLFunction, List<Instruction>> {
        return if (fns.isEmpty()) {
            Pair(this, emptyList())
        } else {
            val closureList = fns.map { fn -> fn.computeClosure(parser) }
            val instrs = closureList.asReversed().flatMap(Pair<APLFunction, List<Instruction>>::second)
            if(instrs.isEmpty()) {
                Pair(this, emptyList())
            } else {
                val newFn = copy(closureList.map(Pair<APLFunction, List<Instruction>>::first))
                Pair(newFn, instrs)
            }
        }
    }

    open fun copy(fns: List<APLFunction>): APLFunction {
        throw IllegalStateException("copy function must be implemented. class = ${this::class.simpleName}")
    }

    open val name1Arg: String = this::class.simpleName ?: "unnamed"
    open val name2Arg: String = this::class.simpleName ?: "unnamed"
}

abstract class NoAxisAPLFunction(pos: Position, fns: List<APLFunction> = emptyList()) : APLFunction(pos, fns) {
    private fun checkAxisNotNull(axis: APLValue?) {
        if (axis != null) {
            throwAPLException(AxisNotSupported(pos))
        }
    }

    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        checkAxisNotNull(axis)
        return eval1Arg(context, a)
    }

    open fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue =
        throwAPLException(Unimplemented1ArgException(pos))

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        checkAxisNotNull(axis)
        return eval2Arg(context, a, b)
    }

    open fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue =
        throwAPLException(Unimplemented2ArgException(pos))

    override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        checkAxisNotNull(axis)
        return evalInverse1Arg(context, a)
    }

    open fun evalInverse1Arg(context: RuntimeContext, a: APLValue): APLValue =
        throwAPLException(InverseNotAvailable(pos))

    override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        checkAxisNotNull(axis)
        return evalInverse2ArgA(context, a, b)
    }

    open fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue): APLValue =
        throwAPLException(InverseNotAvailable(pos))

    override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        checkAxisNotNull(axis)
        return evalInverse2ArgB(context, a, b)
    }

    open fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue): APLValue =
        throwAPLException(InverseNotAvailable(pos))
}

abstract class DelegatedAPLFunctionImpl(pos: Position, fns: List<APLFunction> = emptyList()) : APLFunction(pos, fns) {
    override fun evalArgsAndCall1Arg(context: RuntimeContext, rightArgs: Instruction) =
        innerImpl().evalArgsAndCall1Arg(context, rightArgs)

    override fun evalArgsAndCall2Arg(context: RuntimeContext, leftArgs: Instruction, rightArgs: Instruction) =
        innerImpl().evalArgsAndCall2Arg(context, leftArgs, rightArgs)

    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?) =
        innerImpl().eval1Arg(context, a, axis)

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?) =
        innerImpl().eval2Arg(context, a, b, axis)

    override fun identityValue() = innerImpl().identityValue()
    override fun deriveBitwise() = innerImpl().deriveBitwise()
    override val optimisationFlags: OptimisationFlags get() = innerImpl().optimisationFlags

    override fun eval1ArgLong(context: RuntimeContext, a: Long, axis: APLValue?) =
        innerImpl().eval1ArgLong(context, a, axis)

    override fun eval1ArgDouble(context: RuntimeContext, a: Double, axis: APLValue?) =
        innerImpl().eval1ArgDouble(context, a, axis)

    override fun eval2ArgLongLong(context: RuntimeContext, a: Long, b: Long, axis: APLValue?) =
        innerImpl().eval2ArgLongLong(context, a, b, axis)

    override fun eval2ArgDoubleDouble(context: RuntimeContext, a: Double, b: Double, axis: APLValue?) =
        innerImpl().eval2ArgDoubleDouble(context, a, b, axis)

    override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue =
        innerImpl().evalInverse1Arg(context, a, axis)

    override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue =
        innerImpl().evalInverse2ArgA(context, a, b, axis)

    override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue =
        innerImpl().evalInverse2ArgB(context, a, b, axis)

    override fun computeClosure(parser: APLParser) =
        innerImpl().computeClosure(parser)

//    abstract override fun copy(fns: List<APLFunction>): APLFunction

    @Suppress("LeakingThis")
    override val name1Arg = innerImpl().name1Arg

    @Suppress("LeakingThis")
    override val name2Arg = innerImpl().name2Arg

    abstract fun innerImpl(): APLFunction
}

//abstract class FunctionalFunction(val fns: List<APLFunction>, pos: Position) : APLFunction(pos) {
//}
//
//abstract class NoAxisFunctionalFunction(fns: List<APLFunction>, pos: Position) : FunctionalFunction(fns, pos) {
//    private fun checkAxisNotNull(axis: APLValue?) {
//        if (axis != null) {
//            throwAPLException(AxisNotSupported(pos))
//        }
//    }
//
//    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
//        checkAxisNotNull(axis)
//        return eval1Arg(context, a)
//    }
//
//    open fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue =
//        throwAPLException(Unimplemented1ArgException(pos))
//
//    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
//        checkAxisNotNull(axis)
//        return eval2Arg(context, a, b)
//    }
//
//    open fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue =
//        throwAPLException(Unimplemented2ArgException(pos))
//
//    override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
//        checkAxisNotNull(axis)
//        return evalInverse1Arg(context, a)
//    }
//
//    open fun evalInverse1Arg(context: RuntimeContext, a: APLValue): APLValue =
//        throwAPLException(InverseNotAvailable(pos))
//
//    override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
//        checkAxisNotNull(axis)
//        return evalInverse2ArgA(context, a, b)
//    }
//
//    open fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue): APLValue =
//        throwAPLException(InverseNotAvailable(pos))
//
//    override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
//        checkAxisNotNull(axis)
//        return evalInverse2ArgB(context, a, b)
//    }
//
//    open fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue): APLValue =
//        throwAPLException(InverseNotAvailable(pos))
//}

/**
 * A function that is declared directly in a { ... } expression.
 */
class DeclaredFunction(
    val name: String,
    val instruction: Instruction,
    val leftArgName: EnvironmentBinding,
    val rightArgName: EnvironmentBinding,
    val env: Environment
) : APLFunctionDescriptor {
    inner class DeclaredFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return context.withLinkedContext(env, "declaredFunction1arg(${name})", pos) { localContext ->
                localContext.setVar(rightArgName, a)
                instruction.evalWithContext(localContext)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return context.withLinkedContext(env, "declaredFunction2arg(${name})", pos) { localContext ->
                localContext.setVar(leftArgName, a)
                localContext.setVar(rightArgName, b)
                instruction.evalWithContext(localContext)
            }
        }

        override val name1Arg: String get() = name
        override val name2Arg: String get() = name
    }

    override fun make(pos: Position) = DeclaredFunctionImpl(pos)
}

/**
 * A special declared function which ignores its arguments. Its primary use is inside defsyntax rules
 * where the functions are only used to provide code structure and not directly called by the user.
 */
class DeclaredNonBoundFunction(val instruction: Instruction) : APLFunctionDescriptor {
    inner class DeclaredNonBoundFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return instruction.evalWithContext(context)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return instruction.evalWithContext(context)
        }
    }

    override fun make(pos: Position) = DeclaredNonBoundFunctionImpl(pos)
}
