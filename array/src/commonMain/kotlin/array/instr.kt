package array

import array.builtins.StructuralUnderOp
import array.complex.Complex
import com.dhsdevelopments.mpbignum.BigInt

interface LvalueReader {
    fun makeInstruction(rightArgs: Instruction, pos: Position): Instruction
}

abstract class Instruction(val pos: Position) {
    abstract fun evalWithContext(context: RuntimeContext): APLValue

    open fun evalWithContextAndDiscardResult(context: RuntimeContext) {
        evalWithContext(context).collapse(withDiscard = true)
    }

    abstract fun children(): List<Instruction>

    open fun copy(updatedChildList: List<Instruction>): Instruction {
        require(children().isEmpty()) { "copy() not implemented for ${this::class.simpleName}" }
        require(updatedChildList.isEmpty()) { "updated child list must be empty" }
        return this
    }

    open fun deriveLvalueReader(): LvalueReader? = null
}

class DummyInstr(pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        throw IllegalStateException("Attempt to call dummy instruction")
    }

    override fun children(): List<Instruction> = emptyList()
}

class InstructionList(val instructions: List<Instruction>) : Instruction(computePos(instructions)) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        for (i in 0 until instructions.size - 1) {
            val instr = instructions[i]
            instr.evalWithContextAndDiscardResult(context)
        }
        return instructions.last().evalWithContext(context)
    }

    override fun children() = instructions
    override fun copy(updatedChildList: List<Instruction>) = InstructionList(updatedChildList)

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

class ParsedAPLList private constructor(val instructions: List<Instruction>, pos: Position) : Instruction(pos) {
    constructor(instructions: List<Instruction>) : this(instructions, instructions[0].pos)

    override fun evalWithContext(context: RuntimeContext): APLValue {
        val resultList = ArrayList<APLValue>()
        instructions.forEach { instr ->
            resultList.add(instr.evalWithContext(context))
        }
        return APLList(resultList)
    }

    override fun children() = instructions
    override fun copy(updatedChildList: List<Instruction>) = ParsedAPLList(updatedChildList)

    override fun deriveLvalueReader(): LvalueReader {
        if (instructions.any { instr -> instr !is VariableRef }) {
            throw IncompatibleTypeParseException("Destructuring variable list must only contain variable names", pos)
        }
        return object : LvalueReader {
            override fun makeInstruction(rightArgs: Instruction, pos: Position): Instruction {
                return ListAssignmentInstruction(instructions.map { instr -> (instr as VariableRef).storageRef }.toTypedArray(), rightArgs, pos)
            }
        }
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

    override fun evalWithContextAndDiscardResult(context: RuntimeContext) {
        fn.evalArgsAndCall1ArgDiscardResult(context, rightArgs)
    }

    override fun children() = listOf(rightArgs)
    override fun copy(updatedChildList: List<Instruction>) = FunctionCall1Arg(fn, updatedChildList[0], pos)

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

    override fun evalWithContextAndDiscardResult(context: RuntimeContext) {
        fn.evalArgsAndCall2ArgDiscardResult(context, leftArgs, rightArgs)
    }

    override fun children() = listOf(leftArgs, rightArgs)
    override fun copy(updatedChildList: List<Instruction>) = FunctionCall2Arg(fn, updatedChildList[0], updatedChildList[1], pos)

    override fun toString() = "FunctionCall2Arg(fn=${fn}, leftArgs=${leftArgs}, rightArgs=${rightArgs})"
}

class DynamicFunctionDescriptor(val instr: Instruction) : APLFunctionDescriptor {
    class DynamicFunctionImpl(val instr: Instruction, pos: FunctionInstantiation, val bindEnv: Environment? = null) : APLFunction(pos) {
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

        override fun capturedEnvironments(): List<Environment> {
            return if (bindEnv == null) emptyList() else listOf(bindEnv)
        }

        override fun computeClosure(parser: APLParser): Pair<APLFunction, List<Instruction>> {
            val sym = parser.tokeniser.engine.createAnonymousSymbol("leftAssignedFunction")
            val binding = parser.currentEnvironment().bindLocal(sym)
            val ref = StackStorageRef(binding)
            val list = listOf<Instruction>(AssignmentInstruction.make(ref, instr, pos))
            val env = parser.currentEnvironment()
            return Pair(DynamicFunctionImpl(VariableRef(sym, ref, pos), FunctionInstantiation(pos, env), env), list)
        }
    }

    override fun make(instantiation: FunctionInstantiation): APLFunction {
        return DynamicFunctionImpl(instr, instantiation, if (instantiation.env.subtreeHasLocalBindings()) instantiation.env else null)
    }
}

class VariableRef(val name: Symbol, val storageRef: StackStorageRef, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        return currentStack().findStorage(storageRef).value() ?: throwAPLException(VariableNotAssigned(storageRef.name, pos))
    }

    override fun children(): List<Instruction> = emptyList()

    override fun deriveLvalueReader(): LvalueReader {
        return object : LvalueReader {
            override fun makeInstruction(rightArgs: Instruction, pos: Position): Instruction {
                return AssignmentInstruction.make(storageRef, rightArgs, pos)
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

    override fun children() = values
    override fun copy(updatedChildList: List<Instruction>) = Literal1DArray(updatedChildList, pos)

    override fun deriveLvalueReader(): LvalueReader {
        if (values.any { instr -> instr !is VariableRef }) {
            throw IncompatibleTypeParseException("Destructuring variable list must only contain variable names", pos)
        }
        return object : LvalueReader {
            override fun makeInstruction(rightArgs: Instruction, pos: Position): Instruction {
                return ArrayAssignmentInstruction(values.map { instr -> (instr as VariableRef).storageRef }.toTypedArray(), rightArgs, pos)
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
            fun makePos(): Position {
                return if (values.isEmpty()) {
                    pos
                } else {
                    pos.expandToEnd(values.last().pos)
                }
            }

            val result = ArrayList<Long>()
            result.add(firstValue)
            for (i in 1 until values.size) {
                val v = values[i]
                if (v is LiteralInteger) {
                    result.add(v.value)
                } else {
                    return Literal1DArray(values, makePos())
                }
            }
            return LiteralLongArray(result.toLongArray(), makePos())
        }

        private fun collectDoubleValues(firstValue: Double, values: List<Instruction>, pos: Position): Instruction {
            fun makePos(): Position {
                return if (values.isEmpty()) {
                    pos
                } else {
                    pos.expandToEnd(values.last().pos)
                }
            }

            val result = ArrayList<Double>()
            result.add(firstValue)
            for (i in 1 until values.size) {
                val v = values[i]
                if (v is LiteralDouble) {
                    result.add(v.value)
                } else {
                    return Literal1DArray(values, makePos())
                }
            }
            return LiteralDoubleArray(result.toDoubleArray(), makePos())
        }
    }
}

class LiteralInteger(value: Long, pos: Position) : Instruction(pos) {
    private val valueInt = APLLong(value)

    override fun evalWithContext(context: RuntimeContext) = valueInt
    override fun children(): List<Instruction> = emptyList()
    override fun toString() = "LiteralInteger[value=$valueInt]"
    val value get() = valueInt.value
}

class LiteralDouble(value: Double, pos: Position) : Instruction(pos) {
    private val valueInt = APLDouble(value)

    override fun evalWithContext(context: RuntimeContext) = valueInt
    override fun children(): List<Instruction> = emptyList()
    override fun toString() = "LiteralDouble[value=$valueInt]"
    val value get() = valueInt.value
}

class LiteralComplex(value: Complex, pos: Position) : Instruction(pos) {
    private val valueInt = value.makeAPLNumber()

    override fun evalWithContext(context: RuntimeContext) = valueInt
    override fun children(): List<Instruction> = emptyList()
    override fun toString() = "LiteralComplex[value=$valueInt]"
    val value get() = valueInt.asComplex()
}

class LiteralBigInt(value: BigInt, pos: Position) : Instruction(pos) {
    private val valueInt = APLBigInt(value)

    override fun evalWithContext(context: RuntimeContext) = valueInt
    override fun children(): List<Instruction> = emptyList()
    override fun toString() = "LiteralComplex[value=$valueInt]"
    val value get() = valueInt.asBigInt()
}

class LiteralCharacter(value: Int, pos: Position) : Instruction(pos) {
    val valueInt = APLChar(value)

    override fun evalWithContext(context: RuntimeContext) = valueInt
    override fun children(): List<Instruction> = emptyList()
    override fun toString() = "LiteralCharacter[value=$valueInt]"
}

class LiteralSymbol(name: Symbol, pos: Position) : Instruction(pos) {
    private val value = APLSymbol(name)
    override fun evalWithContext(context: RuntimeContext): APLValue = value
    override fun children(): List<Instruction> = emptyList()
}

class LiteralAPLNullValue(pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLNullValue.APL_NULL_INSTANCE
    override fun children(): List<Instruction> = emptyList()
}

class EmptyValueMarker(pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLEmpty()
    override fun children(): List<Instruction> = emptyList()
}

class LiteralStringValue(val s: String, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLString.make(s)
    override fun children(): List<Instruction> = emptyList()
}

class LiteralLongArray(val value: LongArray, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue = APLArrayLong(dimensionsOfSize(value.size), value)
    override fun children(): List<Instruction> = emptyList()
}

class LiteralDoubleArray(val value: DoubleArray, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue = APLArrayDouble(dimensionsOfSize(value.size), value)
    override fun children(): List<Instruction> = emptyList()
}

class AssignmentInstruction private constructor(val variableRef: StackStorageRef, val instr: Instruction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val res = instr.evalWithContext(context).collapse()
        context.setVar(variableRef, res)
        return res
    }

    override fun children() = listOf(instr)
    override fun copy(updatedChildList: List<Instruction>) = AssignmentInstruction(variableRef, updatedChildList[0], pos)

    companion object {
        fun make(variableRef: StackStorageRef, instr: Instruction, pos: Position): AssignmentInstruction {
            if (variableRef.binding.storage.isConst) {
                throw AssignmentToConstantException(variableRef.binding.name, pos)
            }
            return AssignmentInstruction(variableRef, instr, pos)
        }
    }
}

class ArrayAssignmentInstruction(val variableList: Array<StackStorageRef>, val instr: Instruction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val res = instr.evalWithContext(context)
        val v = res.collapse()
        when {
            v.dimensions.size != 1 -> throwAPLException(
                APLEvalException(
                    "Destructuring assignment requires rank-1 value. Argument has dimensions ${v.dimensions}",
                    pos))
            variableList.size != v.size -> throwAPLException(DestructuringAssignmentShapeMismatch(pos))
            else -> {
                variableList.forEachIndexed { i, variableRef ->
                    if (variableRef.binding.storage.isConst) {
                        throw AssignmentToConstantException(variableRef.binding.name, pos)
                    }
                    context.setVar(variableRef, v.valueAt(i))
                }
            }
        }
        return v
    }

    override fun children() = listOf(instr)
    override fun copy(updatedChildList: List<Instruction>) = ArrayAssignmentInstruction(variableList, updatedChildList[0], pos)
}

class ListAssignmentInstruction(val variableList: Array<StackStorageRef>, val instr: Instruction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val res = instr.evalWithContext(context)
        val v = res.collapse()
        if (v !is APLList) {
            throwAPLException(DestructuringAssignmentShapeMismatch(pos))
        }
        if (variableList.size != v.listSize()) {
            throwAPLException(DestructuringAssignmentShapeMismatch(pos))
        }
        variableList.forEachIndexed { i, variableRef ->
            if (variableRef.binding.storage.isConst) {
                throw AssignmentToConstantException(variableRef.binding.name, pos)
            }
            context.setVar(variableRef, v.listElement(i, pos))
        }
        return v
    }

    override fun children() = listOf(instr)
    override fun copy(updatedChildList: List<Instruction>) = ListAssignmentInstruction(variableList, updatedChildList[0], pos)
}

class UserFunction(
    val name: Symbol,
    val leftFnArgs: List<EnvironmentBinding>,
    val rightFnArgs: List<EnvironmentBinding>,
    var instr: Instruction,
    val env: Environment
) : APLFunctionDescriptor {
    inner class UserFunctionImpl(pos: FunctionInstantiation) : APLFunction(pos) {
        private val leftStorageRefs = leftFnArgs.map(::StackStorageRef)
        private val rightStorageRefs = rightFnArgs.map(::StackStorageRef)

        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return withLinkedContext(env, name.nameWithNamespace, pos) {
                context.assignArgs(rightStorageRefs, a, pos)
                instr.evalWithContext(context)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return withLinkedContext(env, name.nameWithNamespace, pos) {
                context.assignArgs(leftStorageRefs, a, pos)
                context.assignArgs(rightStorageRefs, b, pos)
                instr.evalWithContext(context)
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = UserFunctionImpl(instantiation)
}

class EvalLambdaFnx(val fn: APLFunction, pos: Position, val relatedInstructions: List<Instruction> = emptyList()) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        relatedInstructions.asReversed().forEach { instr ->
            instr.evalWithContext(context)
        }
        return LambdaValue(fn, currentStack().currentFrame())
    }

    override fun children() = relatedInstructions
    override fun copy(updatedChildList: List<Instruction>) = EvalLambdaFnx(fn, pos, updatedChildList)
}

sealed class FunctionCallChain(pos: FunctionInstantiation, fns: List<APLFunction>) : APLFunction(pos, fns) {
    class Chain2(pos: FunctionInstantiation, fn0: APLFunction, fn1: APLFunction) :
        FunctionCallChain(pos, listOf(fn0, fn1)) {
        val fn0 get() = fns[0]
        val fn1 get() = fns[1]

        override val optimisationFlags = computeOptimisationFlags()

        private fun computeOptimisationFlags(): OptimisationFlags {
            return OptimisationFlags(0)
        }

        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            ensureAxisNull(axis)
            val res = fn1.eval1Arg(context, a, null)
            return fn0.eval1Arg(context, res, null)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            ensureAxisNull(axis)
            val res = fn1.eval2Arg(context, a, b, null)
            return fn0.eval1Arg(context, res, null)
        }

        override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            ensureAxisNull(axis)
            val res = fn0.evalInverse1Arg(context, a, null)
            return fn1.evalInverse1Arg(context, res, null)
        }

        private val structuralUnderOp = StructuralUnderOp()

        override fun evalWithStructuralUnder1Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            ensureAxisNull(axis)
            val innerFn: APLFunction = structuralUnderOp.combineFunction(baseFn, fn0, fn0.instantiation).make(instantiation)
            val outerFn = structuralUnderOp.combineFunction(innerFn, fn1, fn1.instantiation).make(instantiation)
            return outerFn.eval1Arg(context, a, null)
        }

        override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            ensureAxisNull(axis)
            val res = fn0.evalInverse1Arg(context, b, null)
            return fn1.evalInverse2ArgB(context, a, res, null)
        }

        override fun copy(fns: List<APLFunction>) = Chain2(instantiation, fns[0], fns[1])
    }

    class Chain3(pos: FunctionInstantiation, fn0: APLFunction, fn1: APLFunction, fn2: APLFunction) : FunctionCallChain(pos, listOf(fn0, fn1, fn2)) {
        val fn0 get() = fns[0]
        val fn1 get() = fns[1]
        val fn2 get() = fns[2]

        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            ensureAxisNull(axis)
            val right = fn2.eval1Arg(context, a, null)
            val left = fn0.eval1Arg(context, a, null)
            return fn1.eval2Arg(context, left, right, null)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            ensureAxisNull(axis)
            val right = fn2.eval2Arg(context, a, b, null)
            val left = fn0.eval2Arg(context, a, b, null)
            return fn1.eval2Arg(context, left, right, null)
        }

        override fun copy(fns: List<APLFunction>) = Chain3(instantiation, fns[0], fns[1], fns[2])
    }
}
