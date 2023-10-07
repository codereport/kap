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
        if (newInstr is FunctionCall1Arg && newInstr.rightArgs is FunctionCall2Arg) {
            val fn0 = newInstr.fn
            val fn1 = newInstr.rightArgs.fn
            val merged = findMergedArgs2To1(fn0, fn1)
            return merged?.computeReplacementInstr(newInstr.rightArgs.leftArgs, newInstr.rightArgs.rightArgs) ?: newInstr
        } else {
            return newInstr
        }
    }

    private fun findMergedArgs2To1(fn0: APLFunction, fn1: APLFunction): MergeMethod? {
        merged1To2Args.forEach { testF ->
            val mergeMethod = testF(fn0, fn1)
            return mergeMethod
        }
        return null
    }

    override fun optimiseParsedCode(parser: APLParser, instr: Instruction): Instruction {
        return findChildExpressions(instr)
    }

    companion object {
        val merged1To2Args: List<(APLFunction, APLFunction) -> MergeMethod?> = listOf(::divideToFloorMergedFunctions)
    }
}

private fun divideToFloorMergedFunctions(fn0: APLFunction, fn1: APLFunction): MergeMethod? {
    if (fn0 !is MinAPLFunction.MinAPLFunctionImpl || fn1 !is DivAPLFunction.DivAPLFunctionImpl) {
        return null
    }
    return object : MergeMethod {
        override fun computeReplacementInstr(leftArgs: Instruction, rightArgs: Instruction): Instruction {
            val mergedFloorDiv = MergedFloorDivFunction(fn0, fn1, fn0.instantiation)
            return FunctionCall2Arg(mergedFloorDiv, leftArgs, rightArgs, fn0.pos)
        }
    }
}

interface MergeMethod {
    fun computeReplacementInstr(leftArgs: Instruction, rightArgs: Instruction): Instruction
}

class MergedFloorDivFunction(
    fn0: MinAPLFunction.MinAPLFunctionImpl,
    fn1: DivAPLFunction.DivAPLFunctionImpl,
    pos: FunctionInstantiation
) : MathNumericCombineAPLFunction(pos, listOf(fn0, fn1)) {

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