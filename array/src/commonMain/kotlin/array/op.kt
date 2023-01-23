package array

import array.builtins.SaveStackCapable
import array.builtins.SaveStackSupport

interface APLOperator {
    fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: Position): APLFunction
}

interface APLOperatorOneArg : APLOperator {
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: Position): APLFunction {
        return combineFunction(currentFn, opPos).make(
            opPos.copy(
                line = currentFn.pos.line,
                col = currentFn.pos.col))
    }

    fun combineFunction(fn: APLFunction, pos: Position): APLFunctionDescriptor
}

/**
 * Parse a single function with strong left-binding. This is used when parsing the right
 * side of a two-argument operator.
 */
fun parseFunctionForOperatorRightArg(parser: APLParser): Either<Pair<APLFunction, Position>, Pair<Token, Position>> {
    val tokenWithPos = parser.tokeniser.nextTokenWithPosition()
    val (token, pos) = tokenWithPos

    fun makeFunctionResult(fn: APLFunction): Either.Left<Pair<APLFunction, Position>> {
        val axis = parser.parseAxis()
        val updated = if (axis != null) {
            AxisValAssignedFunctionDirect(fn, axis)
        } else {
            fn
        }
        return Either.Left(Pair(updated, pos))
    }

    return when (token) {
        is Symbol -> {
            val fn = parser.lookupFunction(token)
            if (fn == null) {
                parser.tokeniser.pushBackToken(tokenWithPos)
                Either.Right(Pair(token, pos))
            } else {
                makeFunctionResult(fn.make(pos.withCallerName(token.symbolName)))
            }
        }
        is OpenFnDef -> {
            makeFunctionResult(parser.parseFnDefinition(pos).make(pos))
        }
        is OpenParen -> {
            val holder = parser.parseExprToplevel(CloseParen)
            if (holder !is ParseResultHolder.FnParseResult) {
                throw ParseException("Expected function", pos)
            }
            makeFunctionResult(holder.fn)
        }
        is ApplyToken -> {
            makeFunctionResult(parser.parseApplyDefinition().make(pos))
        }
        else -> {
            parser.tokeniser.pushBackToken(tokenWithPos)
            Either.Right(Pair(token, pos))
        }
    }
}

interface APLOperatorTwoArg : APLOperator {
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: Position): APLFunction {
        return when (val res = parseFunctionForOperatorRightArg(aplParser)) {
            is Either.Left -> {
                val (fn, pos) = res.value
                val combinedFn = combineFunction(currentFn, fn, opPos)
                combinedFn.make(
                    opPos.copy(
                        endLine = pos.endLine,
                        endCol = pos.endCol))
            }
            is Either.Right -> {
                val (symbol, pos) = res.value
                throw ParseException("Expected function, got: ${symbol}", pos)
            }
        }
    }

    fun combineFunction(fn0: APLFunction, fn1: APLFunction, opPos: Position): APLFunctionDescriptor
}

interface APLOperatorValueRightArg : APLOperator {
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: Position): APLFunction {
        val rightArg = aplParser.parseValue()
        if (rightArg !is ParseResultHolder.InstrParseResult) {
            throw ParseException("Right argument is not a value", rightArg.pos)
        }
        aplParser.tokeniser.pushBackToken(rightArg.lastToken)
        return combineFunction(currentFn, rightArg.instr, opPos)
    }

    fun combineFunction(fn: APLFunction, instr: Instruction, opPos: Position): APLFunction
}

interface APLOperatorCombinedRightArg : APLOperator {
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: Position): APLFunction {
        return when (val rightArg = aplParser.parseValue()) {
            is ParseResultHolder.InstrParseResult -> {
                aplParser.tokeniser.pushBackToken(rightArg.lastToken)
                combineFunctionAndExpr(currentFn, rightArg.instr, opPos).make(opPos)
            }
            is ParseResultHolder.FnParseResult -> {
                aplParser.tokeniser.pushBackToken(rightArg.lastToken)
                combineFunctions(currentFn, rightArg.fn, opPos).make(opPos)
            }
            is ParseResultHolder.EmptyParseResult -> {
                throw ParseException("Expected function or value", rightArg.pos)
            }
        }
    }

    fun combineFunctionAndExpr(fn: APLFunction, instr: Instruction, opPos: Position): APLFunctionDescriptor
    fun combineFunctions(fn1: APLFunction, fn2: APLFunction, opPos: Position): APLFunctionDescriptor
}

class UserDefinedOperatorOneArg(
    val name: Symbol,
    val opBinding: EnvironmentBinding,
    val leftArgs: List<EnvironmentBinding>,
    val rightArgs: List<EnvironmentBinding>,
    val instr: Instruction,
    val env: Environment
) : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: Position): APLFunctionDescriptor {
        return object : APLFunctionDescriptor {
            override fun make(pos: Position): APLFunction {
                return UserDefinedOperatorFn(fn, pos)
            }
        }
    }

    inner class UserDefinedOperatorFn(val opFn: APLFunction, pos: Position) : NoAxisAPLFunction(pos) {
        private val operatorRef = StackStorageRef(opBinding)
        private val leftArgsRef = leftArgs.map(::StackStorageRef)
        private val rightArgsRef = rightArgs.map(::StackStorageRef)

        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return context.withLinkedContext(env, name.nameWithNamespace(), pos) { inner ->
                inner.assignArgs(rightArgsRef, a)
                inner.setVar(operatorRef, LambdaValue(opFn, context.stack.currentFrame()))
                instr.evalWithContext(inner)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return context.withLinkedContext(env, name.nameWithNamespace(), pos) { inner ->
                inner.assignArgs(leftArgsRef, a)
                inner.assignArgs(rightArgsRef, b)
                inner.setVar(operatorRef, LambdaValue(opFn, context.stack.currentFrame()))
                instr.evalWithContext(inner)
            }
        }
    }
}

class UserDefinedOperatorTwoArg(
    val name: Symbol,
    val leftOpBinding: EnvironmentBinding,
    val rightOpBinding: EnvironmentBinding,
    val leftArgs: List<EnvironmentBinding>,
    val rightArgs: List<EnvironmentBinding>,
    val instr: Instruction,
    val env: Environment
) : APLOperator {
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: Position): APLFunction {
        return when (val res = parseFunctionForOperatorRightArg(aplParser)) {
            is Either.Left -> {
                val (fn, pos) = res.value
                FnCall(
                    currentFn,
                    fn,
                    opPos.copy(endLine = pos.endLine, endCol = pos.endCol))
            }
            is Either.Right -> {
                val valueArg = aplParser.parseValue()
                aplParser.tokeniser.pushBackToken(valueArg.lastToken)
                when (valueArg) {
                    is ParseResultHolder.FnParseResult -> throw ParseException("Function not allowed", valueArg.pos)
                    is ParseResultHolder.InstrParseResult -> ValueCall(currentFn, valueArg.instr, opPos)
                    is ParseResultHolder.EmptyParseResult -> throw ParseException("No right argument given", opPos)
                }
            }
        }
    }

    abstract inner class APLUserDefinedOperatorFunction(leftFn: APLFunction, extraFns: List<APLFunction>, pos: Position) :
            NoAxisAPLFunction(pos, listOf(leftFn) + extraFns), SaveStackCapable by SaveStackSupport() {
        private val leftOperatorRef = StackStorageRef(leftOpBinding)
        private val rightOperatorRef = StackStorageRef(rightOpBinding)
        private val leftArgsRef = leftArgs.map(::StackStorageRef)
        private val rightArgsRef = rightArgs.map(::StackStorageRef)

        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val frame = context.stack.currentFrame()
            val arg = mkArg(context)
            return context.withLinkedContext(env, name.nameWithNamespace(), pos) { inner ->
                inner.assignArgs(rightArgsRef, a)
                inner.setVar(leftOperatorRef, LambdaValue(leftFn, frame))
                inner.setVar(rightOperatorRef, arg)
                instr.evalWithContext(inner)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val frame = context.stack.currentFrame()
            val arg = mkArg(context)
            return context.withLinkedContext(env, name.nameWithNamespace(), pos) { inner ->
                inner.assignArgs(leftArgsRef, a)
                inner.assignArgs(rightArgsRef, b)
                inner.setVar(leftOperatorRef, LambdaValue(leftFn, frame))
                inner.setVar(rightOperatorRef, arg)
                instr.evalWithContext(inner)
            }
        }

        abstract fun mkArg(context: RuntimeContext): APLValue

        private val leftFn = fns[0]
    }

    inner class FnCall(leftFn: APLFunction, rightFn: APLFunction, pos: Position) :
            APLUserDefinedOperatorFunction(leftFn, listOf(rightFn), pos) {
        init {
            computeCapturedEnvs(leftFn, rightFn)
        }

        override fun mkArg(context: RuntimeContext) = LambdaValue(rightFn, context.stack.currentFrame())
        private val rightFn = fns[1]
    }

    inner class ValueCall(leftFn: APLFunction, val argInstr: Instruction, pos: Position) :
            APLUserDefinedOperatorFunction(leftFn, emptyList(), pos) {
        init {
            computeCapturedEnvs(leftFn)
        }

        override fun mkArg(context: RuntimeContext) = argInstr.evalWithContext(context)
    }
}

class InverseFnFunctionDescriptor(val fn: APLFunction) : APLFunctionDescriptor {
    inner class InverseFn(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return fn.evalInverse1Arg(context, a, axis)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return fn.evalInverse2ArgB(context, a, b, axis)
        }
    }

    override fun make(pos: Position) = InverseFn(pos)
}

class InverseFnOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: Position): APLFunctionDescriptor {
        return InverseFnFunctionDescriptor(fn)
    }
}
