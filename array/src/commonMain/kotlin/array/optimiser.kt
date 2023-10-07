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

object DivideToFloorInstructionOptimiser : InstructionOptimiser {
    override fun attemptOptimise(instr: Instruction): Instruction? {
        if (instr !is FunctionCall1Arg || instr.rightArgs !is FunctionCall2Arg) {
            return null
        }
        val fn0 = instr.fn
        val divInstr = instr.rightArgs
        val fn1 = divInstr.fn
        if (fn0 !is MinAPLFunction.MinAPLFunctionImpl || fn1 !is DivAPLFunction.DivAPLFunctionImpl) {
            return null
        }
        val mergedFloorDiv = MergedFloorDivFunction(fn0, fn1, fn0.instantiation)
        return FunctionCall2Arg(mergedFloorDiv, divInstr.leftArgs, divInstr.rightArgs, fn0.pos)
    }
}

class MergedFloorDivFunction(
    fn0: MinAPLFunction.MinAPLFunctionImpl,
    fn1: DivAPLFunction.DivAPLFunctionImpl,
    pos: FunctionInstantiation
) : MathNumericCombineAPLFunction(pos, listOf(fn0, fn1), resultType = ArrayMemberType.LONG) {

    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        return numericRelationOperation(
            pos,
            a,
            b,
            { x, y -> divFloor(x, y).makeAPLNumber() },
            { x, y ->
                if (y == 0.0) APLDOUBLE_0 else {
                    (x / y).let { result ->
                        if (result <= MIN_INT_DOUBLE || result >= MAX_INT_DOUBLE) {
                            BigInt.fromDoubleFloor(result).makeAPLNumber()
                        } else {
                            floor(result).toLong().makeAPLNumber()
                        }
                    }
                }
            },
            { _, _ -> throwAPLException(APLIncompatibleDomainsException("Floor is not valid for complex values", pos)) },
            fnBigint = { x, y -> divFloor(x, y).makeAPLNumber() },
            fnRational = { x, y -> (x / y).floor().makeAPLNumberWithReduction() })
    }

    override fun combine2ArgLong(a: Long, b: Long) = divFloor(a, b)

    override val optimisationFlags get() = OptimisationFlags(OptimisationFlags.OPTIMISATION_FLAG_2ARG_LONG_LONG)
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
