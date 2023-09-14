package array

import array.builtins.defaultReduceImpl

/**
 * Class representing a function in KAP. Any subclass of this class that contains
 * a reference to another function should store this reference in the [fns] property.
 * This ensures that any closures created from this function will properly delegate
 * to dependent functions.
 *
 * @param instantiation The instantiation information, including the position and environment, where the function was defined
 * @param fns A list of functions that is used to implement this function.
 */
abstract class APLFunction(instantiation: FunctionInstantiation, val fns: List<APLFunction> = emptyList()) {
    val pos = instantiation.pos
    val instantiationEnv = instantiation.env
    val instantiation get() = FunctionInstantiation(pos, instantiationEnv)

    ///////////////////////////////////
    // 1-arg evaluation functions
    ///////////////////////////////////

    open fun evalArgsAndCall1Arg(context: RuntimeContext, rightArgs: Instruction): APLValue {
        val rightValue = rightArgs.evalWithContext(context)
        return eval1Arg(context, rightValue, null)
    }

    open fun evalArgsAndCall1ArgDiscardResult(context: RuntimeContext, rightArgs: Instruction) {
        val rightValue = rightArgs.evalWithContext(context)
        eval1ArgDiscardResult(context, rightValue, null)
    }

    open fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue =
        throwAPLException(Unimplemented1ArgException(pos))

    open fun eval1ArgDiscardResult(context: RuntimeContext, a: APLValue, axis: APLValue?) {
        eval1Arg(context, a, axis).collapse(withDiscard = true)
    }

    ///////////////////////////////////
    // 2-arg evaluation functions
    ///////////////////////////////////

    open fun evalArgsAndCall2Arg(context: RuntimeContext, leftArgs: Instruction, rightArgs: Instruction): APLValue {
        val rightValue = rightArgs.evalWithContext(context)
        val leftValue = leftArgs.evalWithContext(context)
        return eval2Arg(context, leftValue, rightValue, null)
    }

    open fun evalArgsAndCall2ArgDiscardResult(context: RuntimeContext, leftArgs: Instruction, rightArgs: Instruction) {
        val rightValue = rightArgs.evalWithContext(context)
        val leftValue = leftArgs.evalWithContext(context)
        eval2ArgDiscardResult(context, leftValue, rightValue, null)
    }

    open fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue =
        throwAPLException(Unimplemented2ArgException(pos))

    open fun eval2ArgDiscardResult(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?) {
        eval2Arg(context, a, b, axis).collapse(withDiscard = true)
    }

    /**
     * Return the identity value for this function
     *
     * @throws APLIncompatibleDomainsException is the function does not have an identity value
     */
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

    /**
     * Compute `x` given the equation `a FN x = b`.
     *
     * @throws InverseNotAvailable if the inverse cannot be computed
     */
    open fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue =
        throwAPLException(InverseNotAvailable(pos))

    /**
     * Compute `x` given the equation `x FN b = a`.
     *
     * @throws InverseNotAvailable if the inverse cannot be computed
     */
    open fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue =
        throwAPLException(InverseNotAvailable(pos))

    open fun computeClosure(parser: APLParser): Pair<APLFunction, List<Instruction>> {
        return if (fns.isEmpty()) {
            Pair(this, emptyList())
        } else {
            val closureList = fns.map { fn -> fn.computeClosure(parser) }
            val instrs = closureList.flatMap(Pair<APLFunction, List<Instruction>>::second)
            if (instrs.isEmpty()) {
                Pair(this, emptyList())
            } else {
                val newFn = copy(closureList.map(Pair<APLFunction, List<Instruction>>::first))
                Pair(newFn, instrs)
            }
        }
    }

    open fun copy(fns: List<APLFunction>): APLFunction {
        throw NotImplementedError("copy function must be implemented. class = ${this::class.simpleName}")
    }

    open fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue =
        throwAPLException(StructuralUnderNotSupported(pos))

    open fun evalWithStructuralUnder2Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue =
        throwAPLException(StructuralUnderNotSupported(pos))

    open fun capturedEnvironments(): List<Environment> = emptyList()

    fun allCapturedEnvironments(): List<Environment> {
        val result = ArrayList<Environment>()
        iterateFunctionTree { fn ->
            result.addAll(fn.capturedEnvironments())
        }
        return result
    }

    fun markEscapeEnvironment() {
        allCapturedEnvironments().forEach(Environment::markCanEscape)
    }

    open val name1Arg get() = this::class.simpleName ?: "unnamed"
    open val name2Arg get() = this::class.simpleName ?: "unnamed"

    fun inversibleStructuralUnder1Arg(underFn: APLFunction, baseFn: APLFunction, context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        val v = underFn.eval1Arg(context, a, axis)
        val baseRes = baseFn.eval1Arg(context, v, null)
        return underFn.evalInverse1Arg(context, baseRes, axis)
    }

    fun inversibleStructuralUnder2Arg(
        underFn: APLFunction,
        baseFn: APLFunction,
        context: RuntimeContext,
        a: APLValue,
        b: APLValue,
        axis: APLValue?
    ): APLValue {
        val v = underFn.eval2Arg(context, a, b, axis)
        val baseRes = baseFn.eval1Arg(context, v, null)
        return underFn.evalInverse2ArgB(context, a, baseRes, axis)
    }

    open fun reduce(
        context: RuntimeContext,
        arg: APLValue,
        sizeAlongAxis: Int,
        stepLength: Int,
        offset: Int,
        savedStack: StorageStack.StorageStackFrame?,
        functionAxis: APLValue?
    ): APLValue {
        return defaultReduceImpl(this, context, arg, offset, sizeAlongAxis, stepLength, pos, savedStack, functionAxis)
    }
}

fun APLFunction.iterateFunctionTree(fn: (APLFunction) -> Unit) {
    fn(this)
    fns.forEach { childFn ->
        childFn.iterateFunctionTree(fn)
    }
}

fun APLFunction.ensureAxisNull(axis: APLValue?) {
    if (axis != null) {
        throwAPLException(AxisNotSupported(pos))
    }
}

abstract class NoAxisAPLFunction(pos: FunctionInstantiation, fns: List<APLFunction> = emptyList()) : APLFunction(pos, fns) {

    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        ensureAxisNull(axis)
        return eval1Arg(context, a)
    }

    open fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue =
        throwAPLException(Unimplemented1ArgException(pos))

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        ensureAxisNull(axis)
        return eval2Arg(context, a, b)
    }

    open fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue =
        throwAPLException(Unimplemented2ArgException(pos))

    override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        ensureAxisNull(axis)
        return evalInverse1Arg(context, a)
    }

    open fun evalInverse1Arg(context: RuntimeContext, a: APLValue): APLValue =
        throwAPLException(InverseNotAvailable(pos))

    override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        ensureAxisNull(axis)
        return evalInverse2ArgA(context, a, b)
    }

    open fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue): APLValue =
        throwAPLException(InverseNotAvailable(pos))

    override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        ensureAxisNull(axis)
        return evalInverse2ArgB(context, a, b)
    }

    open fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue): APLValue =
        throwAPLException(InverseNotAvailable(pos))

    open fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue): APLValue =
        throwAPLException(StructuralUnderNotSupported(pos))

    override fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        ensureAxisNull(axis)
        return evalWithStructuralUnder1Arg(baseFn, context, a)
    }

    open fun evalWithStructuralUnder2Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, b: APLValue): APLValue =
        throwAPLException(StructuralUnderNotSupported(pos))

    override fun evalWithStructuralUnder2Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        ensureAxisNull(axis)
        return evalWithStructuralUnder2Arg(baseFn, context, a, b)
    }
}

interface SaveStackCapable {
    fun savedStack(context: RuntimeContext) = if (saveStack()) currentStack().currentFrame() else null
    fun saveStack(): Boolean
}

class SaveStackSupport(fn: APLFunction) {
    private var saveStack: Boolean = false

    fun savedStack() = if (saveStack) currentStack().currentFrame() else null

    init {
        computeCapturedEnvs(fn.fns)
        fn.instantiationEnv.markCanEscape()
    }

    private fun computeCapturedEnvs(fns: List<APLFunction>) {
        val capturedEnvs = fns.flatMap(APLFunction::allCapturedEnvironments)
        if (capturedEnvs.isNotEmpty()) {
            saveStack = capturedEnvs.isNotEmpty()
            capturedEnvs.forEach(Environment::markCanEscape)
        }
    }
}


abstract class DelegatedAPLFunctionImpl(pos: FunctionInstantiation, fns: List<APLFunction> = emptyList()) : APLFunction(pos, fns) {
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

    override fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, axis: APLValue?) =
        innerImpl().evalWithStructuralUnder1Arg(baseFn, context, a, axis)

    override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue =
        innerImpl().evalInverse2ArgB(context, a, b, axis)

    override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue =
        innerImpl().evalInverse2ArgA(context, a, b, axis)

    override fun evalWithStructuralUnder2Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?) =
        innerImpl().evalWithStructuralUnder2Arg(baseFn, context, a, b, axis)

    override fun computeClosure(parser: APLParser) =
        innerImpl().computeClosure(parser)

    //    abstract override fun copy(fns: List<APLFunction>): APLFunction

    override fun capturedEnvironments() = innerImpl().capturedEnvironments()

    override fun reduce(
        context: RuntimeContext,
        arg: APLValue,
        sizeAlongAxis: Int,
        stepLength: Int,
        offset: Int,
        savedStack: StorageStack.StorageStackFrame?,
        functionAxis: APLValue?) =
        innerImpl().reduce(context, arg, sizeAlongAxis, stepLength, offset, savedStack, functionAxis)

    @Suppress("LeakingThis")
    override val name1Arg = innerImpl().name1Arg

    @Suppress("LeakingThis")
    override val name2Arg = innerImpl().name2Arg

    abstract fun innerImpl(): APLFunction
}

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
    inner class DeclaredFunctionImpl(pos: FunctionInstantiation) : APLFunction(pos) {
        private val leftArgRef = StackStorageRef(leftArgName)
        private val rightArgRef = StackStorageRef(rightArgName)

        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return withLinkedContext(env, "declaredFunction1arg(${name})", pos) {
                context.setVar(rightArgRef, a)
                instruction.evalWithContext(context)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return withLinkedContext(env, "declaredFunction2arg(${name})", pos) {
                context.setVar(leftArgRef, a)
                context.setVar(rightArgRef, b)
                instruction.evalWithContext(context)
            }
        }

        override fun capturedEnvironments() = listOf(env)

        override val name1Arg: String get() = name
        override val name2Arg: String get() = name
    }

    override fun make(instantiation: FunctionInstantiation) = DeclaredFunctionImpl(instantiation)
}

/**
 * A special declared function which ignores its arguments. Its primary use is inside defsyntax rules
 * where the functions are only used to provide code structure and not directly called by the user.
 */
class DeclaredNonBoundFunction(val instruction: Instruction) : APLFunctionDescriptor {
    inner class DeclaredNonBoundFunctionImpl(pos: FunctionInstantiation) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return instruction.evalWithContext(context)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return instruction.evalWithContext(context)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = DeclaredNonBoundFunctionImpl(instantiation)
}

class LeftAssignedFunction(
    underlying: APLFunction, val leftArgs: Instruction, pos: FunctionInstantiation, val leftArgBindEnv: Environment? = null
) : APLFunction(pos, listOf(underlying)) {

    private val underlying get() = fns[0]

    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        val leftArg = leftArgs.evalWithContext(context)
        return underlying.eval2Arg(context, leftArg, a, axis)
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        throwAPLException(LeftAssigned2ArgException(pos))
    }

    override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        val leftArg = leftArgs.evalWithContext(context)
        return underlying.evalInverse2ArgB(context, leftArg, a, axis)
    }

    override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        throwAPLException(LeftAssigned2ArgException(pos))
    }

    override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        throwAPLException(LeftAssigned2ArgException(pos))
    }

    override fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        val leftArg = leftArgs.evalWithContext(context)
        return underlying.evalWithStructuralUnder2Arg(baseFn, context, leftArg, a, axis)
    }

    override fun evalWithStructuralUnder2Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        throwAPLException(LeftAssigned2ArgException(pos))
    }

    override fun capturedEnvironments(): List<Environment> {
        return if (leftArgBindEnv == null) emptyList() else listOf(leftArgBindEnv)
    }

    override fun computeClosure(parser: APLParser): Pair<APLFunction, List<Instruction>> {
        val sym = parser.tokeniser.engine.createAnonymousSymbol("leftAssignedFunction")
        val binding = parser.currentEnvironment().bindLocal(sym)
        val (innerFn, relatedInstrs) = underlying.computeClosure(parser)
        val ref = StackStorageRef(binding)
        val list = mutableListOf<Instruction>(AssignmentInstruction(ref, leftArgs, pos))
        list.addAll(relatedInstrs)
        val env = parser.currentEnvironment()
        return Pair(
            LeftAssignedFunction(innerFn, VariableRef(sym, ref, pos), FunctionInstantiation(pos, env), env),
            list)
    }

    override val name1Arg get() = underlying.name2Arg
}

class AxisValAssignedFunctionDirect(baseFn: APLFunction, val axis: Instruction, val axisBindEnv: Environment? = null) :
    NoAxisAPLFunction(baseFn.instantiation, listOf(baseFn)) {

    private val baseFn get() = fns[0]

    override fun evalArgsAndCall1Arg(context: RuntimeContext, rightArgs: Instruction): APLValue {
        val rightValue = rightArgs.evalWithContext(context)
        val axisValue = axis.evalWithContext(context)
        return baseFn.eval1Arg(context, rightValue, axisValue)
    }

    override fun evalArgsAndCall2Arg(context: RuntimeContext, leftArgs: Instruction, rightArgs: Instruction): APLValue {
        val rightValue = rightArgs.evalWithContext(context)
        val axisValue = axis.evalWithContext(context)
        val leftValue = leftArgs.evalWithContext(context)
        return baseFn.eval2Arg(context, leftValue, rightValue, axisValue)
    }

    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        return baseFn.eval1Arg(context, a, axis.evalWithContext(context))
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        return baseFn.eval2Arg(context, a, b, axis.evalWithContext(context))
    }

    override fun evalInverse1Arg(context: RuntimeContext, a: APLValue): APLValue {
        return baseFn.evalInverse1Arg(context, a, axis.evalWithContext(context))
    }

    override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        return baseFn.evalInverse2ArgB(context, a, b, axis.evalWithContext(context))
    }

    override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        return baseFn.evalInverse2ArgA(context, a, b, axis.evalWithContext(context))
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun evalWithStructuralUnder1Arg(processingFN: APLFunction, context: RuntimeContext, a: APLValue): APLValue {
        return baseFn.evalWithStructuralUnder1Arg(processingFN, context, a, axis.evalWithContext(context))
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun evalWithStructuralUnder2Arg(processingFn: APLFunction, context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        return baseFn.evalWithStructuralUnder2Arg(processingFn, context, a, b, axis.evalWithContext(context))
    }

    override fun capturedEnvironments(): List<Environment> {
        return if (axisBindEnv == null) emptyList() else listOf(axisBindEnv)
    }

    override fun computeClosure(parser: APLParser): Pair<APLFunction, List<Instruction>> {
        val sym = parser.tokeniser.engine.createAnonymousSymbol("axisFn")
        val binding = parser.currentEnvironment().bindLocal(sym)
        val (innerFn, relatedInstrs) = baseFn.computeClosure(parser)
        val ref = StackStorageRef(binding)
        val list = ArrayList<Instruction>()
        list.addAll(relatedInstrs)
        list.add(AssignmentInstruction(ref, axis, pos))
        val env = parser.currentEnvironment()
        return Pair(
            AxisValAssignedFunctionDirect(innerFn, VariableRef(sym, ref, pos), env),
            list)
    }

    override val name1Arg get() = baseFn.name1Arg
    override val name2Arg get() = baseFn.name2Arg
}
