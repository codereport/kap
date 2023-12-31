package array

interface APLOperator {
    fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: FunctionInstantiation): APLFunction
}

interface APLOperatorOneArg : APLOperator {
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: FunctionInstantiation): APLFunction {
        return combineFunction(currentFn, opPos).make(
            FunctionInstantiation(opPos.pos.copy(line = currentFn.pos.line, col = currentFn.pos.col), opPos.env))
    }

    fun combineFunction(fn: APLFunction, pos: FunctionInstantiation): APLFunctionDescriptor
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
            val env = parser.currentEnvironment()
            AxisValAssignedFunctionDirect(fn, axis, if (env.subtreeHasLocalBindings()) env else null)
        } else {
            fn
        }
        return Either.Left(Pair(updated, pos))
    }

    fun makeInstantiation() = FunctionInstantiation(pos, parser.currentEnvironment())

    return when (token) {
        is Symbol -> {
            val fn = parser.lookupFunction(token) { FunctionInstantiation(pos.withCallerName(token.symbolName), parser.currentEnvironment()) }
            if (fn == null) {
                parser.tokeniser.pushBackToken(tokenWithPos)
                Either.Right(Pair(token, pos))
            } else {
                makeFunctionResult(fn)
            }
        }
        is OpenFnDef -> {
            makeFunctionResult(parser.parseFnDefinition().make(makeInstantiation()))
        }
        is OpenParen -> {
            val holder = parser.parseExprToplevel(CloseParen)
            if (holder !is ParseResultHolder.FnParseResult) {
                throw ParseException("Expected function", pos)
            }
            makeFunctionResult(holder.fn)
        }
        is ApplyToken -> {
            makeFunctionResult(parser.parseApplyDefinition().make(makeInstantiation()))
        }
        else -> {
            parser.tokeniser.pushBackToken(tokenWithPos)
            Either.Right(Pair(token, pos))
        }
    }
}

interface APLOperatorTwoArg : APLOperator {
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: FunctionInstantiation): APLFunction {
        return when (val res = parseFunctionForOperatorRightArg(aplParser)) {
            is Either.Left -> {
                val (fn, pos) = res.value
                val combinedFn = combineFunction(currentFn, fn, opPos)
                combinedFn.make(
                    FunctionInstantiation(
                        opPos.pos.copy(
                            endLine = pos.endLine,
                            endCol = pos.endCol), opPos.env))
            }
            is Either.Right -> {
                val (symbol, pos) = res.value
                throw ParseException("Expected function, got: ${symbol}", pos)
            }
        }
    }

    fun combineFunction(fn0: APLFunction, fn1: APLFunction, opPos: FunctionInstantiation): APLFunctionDescriptor
}

interface APLOperatorValueRightArg : APLOperator {
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: FunctionInstantiation): APLFunction {
        val rightArg = aplParser.parseValue()
        if (rightArg !is ParseResultHolder.InstrParseResult) {
            throw ParseException("Right argument is not a value", rightArg.pos)
        }
        aplParser.tokeniser.pushBackToken(rightArg.lastToken)
        return combineFunction(currentFn, rightArg.instr, opPos)
    }

    fun combineFunction(fn: APLFunction, instr: Instruction, opPos: FunctionInstantiation): APLFunction
}

interface APLOperatorCombinedRightArg : APLOperator {
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: FunctionInstantiation): APLFunction {
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

    fun combineFunctionAndExpr(fn: APLFunction, instr: Instruction, opPos: FunctionInstantiation): APLFunctionDescriptor
    fun combineFunctions(fn1: APLFunction, fn2: APLFunction, opPos: FunctionInstantiation): APLFunctionDescriptor
}

class UserDefinedOperatorOneArg(
    val name: Symbol,
    val opBinding: EnvironmentBinding,
    val leftArgs: List<EnvironmentBinding>,
    val rightArgs: List<EnvironmentBinding>,
    val instr: Instruction,
    val env: Environment
) : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: FunctionInstantiation): APLFunctionDescriptor {
        return object : APLFunctionDescriptor {
            override fun make(instantiation: FunctionInstantiation): APLFunction {
                return UserDefinedOperatorFn(fn, instantiation)
            }
        }
    }

    inner class UserDefinedOperatorFn(val opFn: APLFunction, pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        private val operatorRef = StackStorageRef(opBinding)
        private val leftArgsRef = leftArgs.map(::StackStorageRef)
        private val rightArgsRef = rightArgs.map(::StackStorageRef)

        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val frame = currentStack().currentFrame()
            return withLinkedContext(env, name.nameWithNamespace, pos) {
                context.assignArgs(rightArgsRef, a)
                context.setVar(operatorRef, LambdaValue(opFn, frame))
                instr.evalWithContext(context)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val frame = currentStack().currentFrame()
            return withLinkedContext(env, name.nameWithNamespace, pos) {
                context.assignArgs(leftArgsRef, a)
                context.assignArgs(rightArgsRef, b)
                context.setVar(operatorRef, LambdaValue(opFn, frame))
                instr.evalWithContext(context)
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
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: FunctionInstantiation): APLFunction {
        return when (val res = parseFunctionForOperatorRightArg(aplParser)) {
            is Either.Left -> {
                val (fn, pos) = res.value
                FnCall(
                    currentFn,
                    fn,
                    FunctionInstantiation(opPos.pos.copy(endLine = pos.endLine, endCol = pos.endCol), opPos.env))
            }
            is Either.Right -> {
                val valueArg = aplParser.parseValue()
                aplParser.tokeniser.pushBackToken(valueArg.lastToken)
                when (valueArg) {
                    is ParseResultHolder.FnParseResult -> throw ParseException("Function not allowed", valueArg.pos)
                    is ParseResultHolder.InstrParseResult -> ValueCall(currentFn, valueArg.instr, opPos)
                    is ParseResultHolder.EmptyParseResult -> throw ParseException("No right argument given", opPos.pos)
                }
            }
        }
    }

    abstract inner class APLUserDefinedOperatorFunction(leftFn: APLFunction, extraFns: List<APLFunction>, pos: FunctionInstantiation) :
        NoAxisAPLFunction(pos, listOf(leftFn) + extraFns) {
        private val leftOperatorRef = StackStorageRef(leftOpBinding)
        private val rightOperatorRef = StackStorageRef(rightOpBinding)
        private val leftArgsRef = leftArgs.map(::StackStorageRef)
        private val rightArgsRef = rightArgs.map(::StackStorageRef)

        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val frame = currentStack().currentFrame()
            val arg = mkArg(context)
            return withLinkedContext(env, name.nameWithNamespace, pos) {
                context.assignArgs(rightArgsRef, a)
                context.setVar(leftOperatorRef, LambdaValue(leftFn, frame))
                context.setVar(rightOperatorRef, arg)
                instr.evalWithContext(context)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val frame = currentStack().currentFrame()
            val arg = mkArg(context)
            return withLinkedContext(env, name.nameWithNamespace, pos) {
                context.assignArgs(leftArgsRef, a)
                context.assignArgs(rightArgsRef, b)
                context.setVar(leftOperatorRef, LambdaValue(leftFn, frame))
                context.setVar(rightOperatorRef, arg)
                instr.evalWithContext(context)
            }
        }

        abstract fun mkArg(context: RuntimeContext): APLValue

        private val leftFn = fns[0]
    }

    inner class FnCall(leftFn: APLFunction, rightFn: APLFunction, pos: FunctionInstantiation) : APLUserDefinedOperatorFunction(leftFn, listOf(rightFn), pos) {
        override fun mkArg(context: RuntimeContext) = LambdaValue(rightFn, currentStack().currentFrame())
        private val rightFn = fns[1]

        init {
            SaveStackSupport(this)
        }
    }

    inner class ValueCall(leftFn: APLFunction, val argInstr: Instruction, pos: FunctionInstantiation) :
        APLUserDefinedOperatorFunction(leftFn, emptyList(), pos) {
        override fun mkArg(context: RuntimeContext) = argInstr.evalWithContext(context)

        init {
            SaveStackSupport(this)
        }
    }
}

class InverseFnFunctionDescriptor(val fn: APLFunction) : APLFunctionDescriptor {
    inner class InverseFn(pos: FunctionInstantiation) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return fn.evalInverse1Arg(context, a, axis)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return fn.evalInverse2ArgB(context, a, b, axis)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = InverseFn(instantiation)
}

class InverseFnOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: FunctionInstantiation): APLFunctionDescriptor {
        return InverseFnFunctionDescriptor(fn)
    }
}
