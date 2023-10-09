package array

import array.builtins.*
import com.dhsdevelopments.mpbignum.*
import kotlin.math.floor

interface Optimiser {
    fun optimiseParsedCode(parser: APLParser, instr: Instruction): Instruction
}

class PlainOptimiser : Optimiser {
    override fun optimiseParsedCode(parser: APLParser, instr: Instruction): Instruction {
        return instr
    }
}

class StandardOptimiser : Optimiser {
    private fun findChildExpressions(instr: Instruction): Instruction {
        val childrenCopy = instr.children().map { childInstr ->
            findChildExpressions(childInstr)
        }
        require(childrenCopy.size == instr.children().size) { "Updated child list size is not the same as original list" }
        val newInstr = instr.copy(childrenCopy)
        return findOptimisedInstruction(newInstr) ?: newInstr
    }

    private fun findOptimisedInstruction(instr: Instruction): Instruction? {
        instructionOptimisers.forEach { opt ->
            val res = opt.attemptOptimise(instr)
            if (res != null) {
                return res
            }
        }
        return null
    }

    override fun optimiseParsedCode(parser: APLParser, instr: Instruction): Instruction {
        return findChildExpressions(instr)
    }

    companion object {
        val instructionOptimisers = listOf(DivideToFloorInstructionOptimiser)
    }
}

interface InstructionOptimiser {
    fun attemptOptimise(instr: Instruction): Instruction?
}

abstract class Scalar2ArgInstructionChainOptimiser : InstructionOptimiser {
    override fun attemptOptimise(instr: Instruction): Instruction? {
        attemptRegularChainedFnCalls(instr)?.let { result -> return result }
        attemptCallChain(instr)?.let { result -> return result }
        return null
    }

    private fun attemptRegularChainedFnCalls(instr: Instruction): Instruction? {
        if (instr !is FunctionCall1Arg || instr.rightArgs !is FunctionCall2Arg) {
            return null
        }
        val fn0 = instr.fn
        val secondInstr = instr.rightArgs
        val fn1 = secondInstr.fn
        val mergedFn = findMergedFunctions(fn0, fn1) ?: return null
        return FunctionCall2Arg(mergedFn, secondInstr.leftArgs, secondInstr.rightArgs, fn0.pos)
    }

    private fun attemptCallChain(instr: Instruction): Instruction? {
        if (instr !is FunctionCall2Arg || instr.fn !is FunctionCallChain.Chain2) {
            return null
        }
        val fn0 = instr.fn.fn0
        val fn1 = instr.fn.fn1
        val mergedFn = findMergedFunctions(fn0, fn1) ?: return null
        return FunctionCall2Arg(mergedFn, instr.leftArgs, instr.rightArgs, fn0.pos)
    }

    abstract fun findMergedFunctions(fn0: APLFunction, fn1: APLFunction): APLFunction?
}

object DivideToFloorInstructionOptimiser : Scalar2ArgInstructionChainOptimiser() {
    override fun findMergedFunctions(fn0: APLFunction, fn1: APLFunction): APLFunction? {
        if (fn0 !is MinAPLFunction.MinAPLFunctionImpl || fn1 !is DivAPLFunction.DivAPLFunctionImpl) {
            return null
        }
        return MergedFloorDivFunction(fn0, fn1, fn0.instantiation)
    }
}

class MergedFloorDivFunction(
    fn0: MinAPLFunction.MinAPLFunctionImpl,
    fn1: DivAPLFunction.DivAPLFunctionImpl,
    pos: FunctionInstantiation
) : MathNumericCombineAPLFunction(pos, listOf(fn0, fn1), resultType2Arg = ArrayMemberType.LONG) {

    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        return opLong(a, b, { x -> x.makeAPLNumber() }, { x -> x.makeAPLNumber() })
    }

    override fun combine2ArgGenericToLong(a: APLSingleValue, b: APLSingleValue): Long {
        return opLong(a, b, { x -> x }, { x -> throw LongExpressionOverflow(x) })
    }

    private inline fun <T> convertOrOverflow(a: BigInt, convFn: (Long) -> T, overflowFn: (BigInt) -> T): T {
        return if (a.rangeInLong()) {
            convFn(a.toLong())
        } else {
            overflowFn(a)
        }
    }

    private inline fun <T> opLong(a: APLSingleValue, b: APLSingleValue, convFn: (Long) -> T, overflowFn: (BigInt) -> T): T {
        numericRelationOperation2(
            pos,
            a,
            b,
            { x, y -> return if (x == Long.MIN_VALUE && y == -1L) overflowFn(BIGINT_MAX_LONG_ADD_1) else convFn(divFloor(x, y)) },
            { x, y ->
                return if (y == 0.0) convFn(0) else {
                    (x / y).let { result ->
                        if (result <= MIN_INT_DOUBLE || result >= MAX_INT_DOUBLE) {
                            overflowFn(BigInt.fromDoubleFloor(result))
                        } else {
                            convFn(floor(result).toLong())
                        }
                    }
                }
            },
            { _, _ -> throwAPLException(IncompatibleTypeException("Floor is not valid for complex values", pos)) },
            fnBigint = { x, y -> return convertOrOverflow(divFloor(x, y), convFn, overflowFn) },
            fnRational = { x, y -> return convertOrOverflow((x / y).floor(), convFn, overflowFn) })
    }

    override fun combine2ArgLongToLong(a: Long, b: Long) = divFloor(a, b)

    override val optimisationFlags get() = OptimisationFlags(OptimisationFlags.OPTIMISATION_FLAG_2ARG_LONG_LONG)

    companion object {
        val BIGINT_MAX_LONG_ADD_1 = BigInt.of("9223372036854775808")
    }
}

private fun divFloor(a: Long, b: Long): Long {
    return a / b - if ((a % b) != 0L && (a xor b) < 0) 1 else 0
}

private fun divFloor(a: BigInt, b: BigInt): BigInt {
    return a / b - if ((a % b) != BigIntConstants.ZERO && (a xor b) < BigIntConstants.ZERO) BigIntConstants.ONE else BigIntConstants.ZERO
}

private fun divCeil(a: Long, b: Long): Long {
    return a / b + if ((a % b) != 0L && (a xor b) > 0) 1 else 0
}
