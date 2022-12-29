package array

import array.complex.Complex

interface LvalueReader {
    fun makeInstruction(rightArgs: Instruction, pos: Position): Instruction
}

abstract class Instruction(val pos: Position) {
    abstract fun evalWithContext(context: RuntimeContext): APLValue
    open fun deriveLvalueReader(): LvalueReader? = null
}

class DummyInstr(pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        throw IllegalStateException("Attempt to call dummy instruction")
    }
}

class RootEnvironmentInstruction(val environment: Environment, val instr: Instruction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        throw IllegalStateException("Root environment called with context")
    }

    fun evalWithNewContext(engine: Engine, extraBindings: List<Pair<EnvironmentBinding, APLValue>>?): APLValue {
        val context = RuntimeContext(engine, environment, engine.rootContext)
        extraBindings?.forEach { (binding, value) ->
            context.setVar(binding, value)
        }
        return instr.evalWithContext(context)
    }
}

class InstructionList(val instructions: List<Instruction>) : Instruction(computePos(instructions)) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        for (i in 0 until instructions.size - 1) {
            val instr = instructions[i]
            instr.evalWithContext(context).collapse()
        }
        return instructions.last().evalWithContext(context)
    }

    companion object {
        private fun computePos(l: List<Instruction>): Position {
            return when (l.size) {
                0 -> throw IllegalStateException("Empty instruction list")
                1 -> l[0].pos
                else -> l.last().pos.let { last -> l[0].pos.copy(endLine = last.computedEndLine, endCol = last.computedEndCol) }
            }
        }
    }
}

class ParsedAPLList(val instructions: List<Instruction>) : Instruction(instructions[0].pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val resultList = ArrayList<APLValue>()
        instructions.forEach { instr ->
            resultList.add(instr.evalWithContext(context))
        }
        return APLList(resultList)
    }
}

class FunctionCall1Arg(
    val fn: APLFunction,
    val rightArgs: Instruction,
    pos: Position
) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        return fn.evalArgsAndCall1Arg(context, rightArgs)
    }

    override fun toString() = "FunctionCall1Arg(fn=${fn}, rightArgs=${rightArgs})"
}

class FunctionCall2Arg(
    val fn: APLFunction,
    val leftArgs: Instruction,
    val rightArgs: Instruction,
    pos: Position
) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        return fn.evalArgsAndCall2Arg(context, leftArgs, rightArgs)
    }

    override fun toString() = "FunctionCall2Arg(fn=${fn}, leftArgs=${leftArgs}, rightArgs=${rightArgs})"
}

class DynamicFunctionDescriptor(val instr: Instruction) : APLFunctionDescriptor {
    inner class DynamicFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return resolveFn(context).eval1Arg(context, a, axis)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return resolveFn(context).eval2Arg(context, a, b, axis)
        }

        private fun resolveFn(context: RuntimeContext): APLFunction {
            val result = instr.evalWithContext(context)
            val v = result.unwrapDeferredValue()
            if (v !is LambdaValue) {
                throwAPLException(IncompatibleTypeException("Cannot evaluate values of type: ${v.aplValueType.typeName}", pos))
            }
            return v.makeClosure()
        }
    }

    override fun make(pos: Position): APLFunction {
        return DynamicFunctionImpl(pos)
    }
}

class VariableRef(val name: Symbol, val binding: EnvironmentBinding, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        return context.getVar(binding) ?: throwAPLException(VariableNotAssigned(binding.name, pos))
    }

    override fun deriveLvalueReader(): LvalueReader {
        return object : LvalueReader {
            override fun makeInstruction(rightArgs: Instruction, pos: Position): Instruction {
                return AssignmentInstruction(arrayOf(binding), rightArgs, pos)
            }
        }
    }

    override fun toString() = "Var(${name})"
}

class Literal1DArray private constructor(val values: List<Instruction>, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val size = values.size
        val result = Array<APLValue?>(size) { null }
        for (i in (size - 1) downTo 0) {
            result[i] = values[i].evalWithContext(context)
        }
        return APLArrayImpl.make(dimensionsOfSize(size)) { result[it]!! }
    }

    override fun deriveLvalueReader(): LvalueReader {
        if (values.any { instr -> instr !is VariableRef }) {
            throw IncompatibleTypeParseException("Destructuring variable list must only contain variable names", pos)
        }
        return object : LvalueReader {
            override fun makeInstruction(rightArgs: Instruction, pos: Position): Instruction {
                return AssignmentInstruction(values.map { instr -> (instr as VariableRef).binding }.toTypedArray(), rightArgs, pos)
            }
        }
    }

    override fun toString() = "Literal1DArray(${values})"

    companion object {
        fun make(values: List<Instruction>): Instruction {
            assertx(values.isNotEmpty()) { "attempt to create empty Literal1DArray" }
            return when (val firstElement = values[0]) {
                is LiteralInteger -> {
                    collectLongValues(firstElement.value, values, firstElement.pos)
                }
                is LiteralDouble -> {
                    collectDoubleValues(firstElement.value, values, firstElement.pos)
                }
                else -> Literal1DArray(values, firstElement.pos)
            }
        }

        private fun collectLongValues(firstValue: Long, values: List<Instruction>, pos: Position): Instruction {
            val result = ArrayList<Long>()
            result.add(firstValue)
            for (i in 1 until values.size) {
                val v = values[i]
                if (v is LiteralInteger) {
                    result.add(v.value)
                } else {
                    return Literal1DArray(values, pos)
                }
            }
            return LiteralLongArray(result.toLongArray(), pos)
        }

        private fun collectDoubleValues(firstValue: Double, values: List<Instruction>, pos: Position): Instruction {
            val result = ArrayList<Double>()
            result.add(firstValue)
            for (i in 1 until values.size) {
                val v = values[i]
                if (v is LiteralDouble) {
                    result.add(v.value)
                } else {
                    return Literal1DArray(values, pos)
                }
            }
            return LiteralDoubleArray(result.toDoubleArray(), pos)
        }
    }
}

class LiteralInteger(value: Long, pos: Position) : Instruction(pos) {
    private val valueInt = APLLong(value)

    override fun evalWithContext(context: RuntimeContext) = valueInt
    override fun toString() = "LiteralInteger[value=$valueInt]"
    val value get() = valueInt.value
}

class LiteralDouble(value: Double, pos: Position) : Instruction(pos) {
    private val valueInt = APLDouble(value)

    override fun evalWithContext(context: RuntimeContext) = valueInt
    override fun toString() = "LiteralDouble[value=$valueInt]"
    val value get() = valueInt.value
}

class LiteralComplex(value: Complex, pos: Position) : Instruction(pos) {
    private val valueInt = value.makeAPLNumber()

    override fun evalWithContext(context: RuntimeContext) = valueInt
    override fun toString() = "LiteralComplex[value=$valueInt]"
    val value get() = valueInt.asComplex()
}

class LiteralCharacter(value: Int, pos: Position) : Instruction(pos) {
    val valueInt = APLChar(value)

    override fun evalWithContext(context: RuntimeContext) = valueInt
    override fun toString() = "LiteralCharacter[value=$valueInt]"
}

class LiteralSymbol(name: Symbol, pos: Position) : Instruction(pos) {
    private val value = APLSymbol(name)
    override fun evalWithContext(context: RuntimeContext): APLValue = value
}

class LiteralAPLNullValue(pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLNullValue.APL_NULL_INSTANCE
}

class EmptyValueMarker(pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLEmpty()
}

class LiteralStringValue(val s: String, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLString.make(s)
}

class LiteralLongArray(val value: LongArray, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue = APLArrayLong(dimensionsOfSize(value.size), value)
}

class LiteralDoubleArray(val value: DoubleArray, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue = APLArrayDouble(dimensionsOfSize(value.size), value)
}

class AssignmentInstruction(val variableList: Array<EnvironmentBinding>, val instr: Instruction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val res = instr.evalWithContext(context)
        val v = res.collapse()
        when {
            variableList.size == 1 -> context.setVar(variableList[0], v)
            v.dimensions.size != 1 -> throwAPLException(APLEvalException("Destructuring assignment requires rank-1 value", pos))
            variableList.size != v.size -> throwAPLException(
                APLEvalException(
                    "Destructuring assignment expected ${variableList.size} results, got: ${v.size}",
                    pos))
            else -> {
                variableList.forEachIndexed { i, binding ->
                    context.setVar(binding, v.valueAt(i))
                }
            }
        }
        return v
    }
}

class UserFunction(
    private val name: Symbol,
    private var leftFnArgs: List<EnvironmentBinding>,
    private var rightFnArgs: List<EnvironmentBinding>,
    var instr: Instruction,
    private var env: Environment
) : APLFunctionDescriptor {
    inner class UserFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return context.withLinkedContext(env, name.nameWithNamespace(), pos) { inner ->
                inner.assignArgs(rightFnArgs, a, pos)
                instr.evalWithContext(inner)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return context.withLinkedContext(env, name.nameWithNamespace(), pos) { inner ->
                inner.assignArgs(leftFnArgs, a, pos)
                inner.assignArgs(rightFnArgs, b, pos)
                instr.evalWithContext(inner)
            }
        }
    }

    override fun make(pos: Position) = UserFunctionImpl(pos)
}

sealed class FunctionCallChain(pos: Position, fns: List<APLFunction>) : APLFunction(pos, fns) {
    class Chain2(pos: Position, fn0: APLFunction, fn1: APLFunction, val inFunctionChainContext: Boolean) :
        FunctionCallChain(pos, listOf(fn0, fn1)) {
        val fn0 get() = fns[0]
        val fn1 get() = fns[1]

        override val optimisationFlags = computeOptimisationFlags()

        private fun computeOptimisationFlags(): OptimisationFlags {
            return OptimisationFlags(0)
        }

        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            if (axis != null) throw AxisNotSupported(pos)
            val res = fn1.eval1Arg(context, a, null)
            return fn0.eval1Arg(context, res, null)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            if (axis != null) throw AxisNotSupported(pos)
            val res = fn1.eval2Arg(context, a, b, null)
            return fn0.eval1Arg(context, res, null)
        }

        override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            if (axis != null) throw AxisNotSupported(pos)
            val res = fn0.evalInverse1Arg(context, a, null)
            return fn1.evalInverse1Arg(context, res, null)
        }

        override fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue) =
            inversibleStructuralUnder1Arg(this, baseFn, context, a)

        override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            if (axis != null) throw AxisNotSupported(pos)
            val res = fn0.evalInverse1Arg(context, b, null)
            return fn1.evalInverse2ArgB(context, a, res, null)
        }

        override fun copy(fns: List<APLFunction>): APLFunction {
            return Chain2(pos, fns[0], fns[1], inFunctionChainContext)
        }
    }

    class Chain3(pos: Position, fn0: APLFunction, fn1: APLFunction, fn2: APLFunction) : FunctionCallChain(pos, listOf(fn0, fn1, fn2)) {
        val fn0 get() = fns[0]
        val fn1 get() = fns[1]
        val fn2 get() = fns[2]

        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            if (axis != null) throw AxisNotSupported(pos)
            val right = fn2.eval1Arg(context, a, null)
            val left = fn0.eval1Arg(context, a, null)
            return fn1.eval2Arg(context, left, right, null)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            if (axis != null) throw AxisNotSupported(pos)
            val right = fn2.eval2Arg(context, a, b, null)
            val left = fn0.eval2Arg(context, a, b, null)
            return fn1.eval2Arg(context, left, right, null)
        }

        override fun copy(fns: List<APLFunction>): APLFunction {
            return Chain3(pos, fns[0], fns[1], fns[2])
        }
    }

    companion object {
        fun make(pos: Position, fn0: APLFunction, fn1: APLFunction, functionChainContext: Boolean): FunctionCallChain {
            return when {
                fn1 is Chain2 && fn1.inFunctionChainContext -> Chain3(pos, fn0, fn1.fn0, fn1.fn1)
                else -> Chain2(pos, fn0, fn1, functionChainContext)
            }
        }
    }
}
