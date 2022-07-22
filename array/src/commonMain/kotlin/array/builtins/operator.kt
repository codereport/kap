package array.builtins

import array.*
import kotlin.math.max
import kotlin.math.min

class PowerAPLOperator : APLOperatorCombinedRightArg {
    override fun combineFunctionAndExpr(fn: APLFunction, instr: Instruction, opPos: Position): APLFunctionDescriptor {
        return PowerAPLFunctionWithValueDescriptor(fn, instr)
    }

    override fun combineFunctions(fn1: APLFunction, fn2: APLFunction, opPos: Position): APLFunctionDescriptor {
        return PowerAPLFunctionDescriptor(fn1, fn2)
    }

    class PowerAPLFunctionWithValueDescriptor(
        val fn: APLFunction,
        val instr: Instruction
    ) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            return object : APLFunction(pos) {
                override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                    val iterations = instr.evalWithContext(context)
                    var n = iterations.ensureNumber(pos).asLong(pos)
                    if (n < 0) {
                        throwAPLException(APLIllegalArgumentException("Argument to power is negative: ${n}", pos))
                    }
                    val engine = context.engine
                    var curr = a
                    while (n > 0) {
                        engine.checkInterrupted()
                        curr = fn.eval1Arg(context, curr, null).collapse()
                        n--
                    }
                    return curr
                }
            }
        }
    }

    class PowerAPLFunctionDescriptor(
        val fn1: APLFunction,
        val fn2: APLFunction,
    ) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            return object : APLFunction(pos) {
                override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                    val engine = context.engine
                    var curr = a
                    while (true) {
                        engine.checkInterrupted()
                        val next = fn1.eval1Arg(context, curr, null).collapse()
                        val checkResult = fn2.eval2Arg(context, next, curr, null).collapse()
                        curr = next
                        if (checkResult.asBoolean()) {
                            break
                        }
                        context.engine.checkInterrupted(pos)
                    }
                    return curr
                }
            }
        }
    }
}

/*
Implementation note:

From my presentation at the Dyalog '19 user meeting, (f⍤k)x ←→ ↑f¨⊂[(-k)↑⍳≢⍴x]x. Slides to download here.
For two ranks, you have to find which ranks correspond to which arguments, then use the ⊂[…] thing on each argument separately.
It also assumes a positive rank; to convert a negative rank to positive I think the formula is k←0⌊(≢⍴x)+k.
There's also the reference implementation in BQN, which has the same functionality
as APL (it supports leading axis agreement, but only because Each does)

once you have the monadic case working, the dyadic one is basically free (maybe after extracting some stuff to functions,
and changing laziness behavior)
@dzaima (in fact, {a b c←⌽3⍴⌽⍵⍵ ⋄ ↑(⊂⍤b⊢⍺) ⍺⍺¨ ⊂⍤c⊢⍵} is an impl ofthe dyadic case from
the monadic one (with Dyalog's ↑ meaning))
 */

class RankOperator : APLOperatorValueRightArg {
    override fun combineFunction(fn: APLFunction, instr: Instruction, opPos: Position): APLFunction {
        return object : APLFunction(opPos) {
            override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                val aReduced = a.collapseFirstLevel()
                val aDimensions = aReduced.dimensions
                val index = computeRankFromOpArg(context)
                val k = max(0, if (index < 0) aDimensions.size + index else index)
                val enclosedResult = AxisMultiDimensionEnclosedValue(aReduced, k)
                val applyRes = ForEachResult1Arg(context, fn, enclosedResult, null, pos)
                return DiscloseAPLFunction.discloseValue(applyRes)
            }

            private fun raiseArgumentException(): Nothing {
                throwAPLException(APLIllegalArgumentException("Operator argument must be scalar or an array of 1 to 3 elements", pos))
            }

            private fun computeRankFromOpArg(context: RuntimeContext): Int {
                val opArg = instr.evalWithContext(context)

                val d = opArg.dimensions
                val res = when (d.size) {
                    0 -> {
                        opArg
                    }
                    1 -> {
                        when (d[0]) {
                            1 -> opArg.valueAt(0)
                            2 -> opArg.valueAt(1)
                            3 -> opArg.valueAt(0)
                            else -> raiseArgumentException()
                        }
                    }
                    else -> {
                        raiseArgumentException()
                    }
                }
                return res.ensureNumber(pos).asInt(pos)
            }

            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                val aReduced = a.collapseFirstLevel()
                val aDimensions = aReduced.dimensions

                val bReduced = b.collapseFirstLevel()
                val bDimensions = bReduced.dimensions

                val opArg = instr.evalWithContext(context)
                val d = opArg.dimensions
                val index0: Int
                val index1: Int
                when (d.size) {
                    0 -> {
                        opArg.ensureNumber(pos).asInt(pos).let { v ->
                            index0 = v
                            index1 = v
                        }
                    }
                    1 ->
                        when (d[0]) {
                            1 -> opArg.valueAtInt(0, pos).let { v ->
                                index0 = v
                                index1 = v
                            }
                            2 -> {
                                index0 = opArg.valueAtInt(0, pos)
                                index1 = opArg.valueAtInt(1, pos)
                            }
                            3 -> {
                                index0 = opArg.valueAtInt(1, pos)
                                index1 = opArg.valueAtInt(2, pos)
                            }
                            else -> raiseArgumentException()
                        }
                    else -> raiseArgumentException()
                }
                val k0 = min(max(0, if (index0 < 0) aDimensions.size + index0 else index0), aDimensions.size)
                val enclosedResult0 = AxisMultiDimensionEnclosedValue(aReduced, k0)

                val k1 = min(max(0, if (index1 < 0) bDimensions.size + index1 else index1), bDimensions.size)
                val enclosedResult1 = AxisMultiDimensionEnclosedValue(bReduced, k1)

                val applyRes = ForEachFunctionDescriptor.compute2Arg(context, fn, enclosedResult0, enclosedResult1, null, pos)
                return DiscloseAPLFunction.discloseValue(applyRes)
            }

        }
    }
}

class ComposedFunctionDescriptor(val fn0: APLFunction, val fn1: APLFunction) : APLFunctionDescriptor {
    inner class ComposedFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override val optimisationFlags = computeOptimisationFlags()

        private fun computeOptimisationFlags(): OptimisationFlags {
            val fn1Flags = fn0.optimisationFlags
            val fn2Flags = fn1.optimisationFlags
            val flags1Arg = fn1Flags.masked1Arg.andWith(fn2Flags.masked1Arg)
            var resFlags = 0
            if (fn2Flags.is1ALong && fn1Flags.is2ALongLong) resFlags = resFlags or OptimisationFlags.OPTIMISATION_FLAG_2ARG_LONG_LONG
            if (fn2Flags.is1ADouble && fn1Flags.is2ADoubleDouble) resFlags =
                resFlags or OptimisationFlags.OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE
            return OptimisationFlags(flags1Arg.flags or resFlags)
        }

        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val res = fn1.eval1Arg(context, a, null)
            return fn0.eval2Arg(context, a, res, null)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val res = fn1.eval1Arg(context, b, null)
            return fn0.eval2Arg(context, a, res, null)
        }

        override fun eval1ArgLong(context: RuntimeContext, a: Long, axis: APLValue?): Long {
            val res = fn1.eval1ArgLong(context, a, null)
            return fn0.eval1ArgLong(context, res, null)
        }

        override fun eval1ArgDouble(context: RuntimeContext, a: Double, axis: APLValue?): Double {
            val res = fn1.eval1ArgDouble(context, a, null)
            return fn0.eval1ArgDouble(context, res, null)
        }

        override fun eval2ArgLongLong(context: RuntimeContext, a: Long, b: Long, axis: APLValue?): Long {
            val res = fn1.eval1ArgLong(context, b, null)
            return fn0.eval2ArgLongLong(context, a, res, null)
        }

        override fun eval2ArgDoubleDouble(context: RuntimeContext, a: Double, b: Double, axis: APLValue?): Double {
            val res = fn1.eval1ArgDouble(context, b, null)
            return fn0.eval2ArgDoubleDouble(context, a, res, null)
        }

        override fun evalInverse1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            val res = fn0.evalInverse1Arg(context, a, null)
            return fn1.evalInverse1Arg(context, res, null)
        }

        override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val res = fn0.evalInverse2ArgA(context, a, b, null)
            return fn1.evalInverse1Arg(context, res, null)
        }

        override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val res = fn1.evalInverse1Arg(context, b, null)
            return fn0.evalInverse2ArgB(context, a, res, null)
        }

        fun fn1() = fn0
        fun fn2() = fn1

        override val name1Arg = "compose [${fn0.name1Arg}, ${fn1.name1Arg}]"
        override val name2Arg = "compose [${fn0.name2Arg}, ${fn1.name1Arg}]"
    }

    override fun make(pos: Position) = ComposedFunctionImpl(pos)
}

class ComposeOp : APLOperatorTwoArg {
    override fun combineFunction(fn1: APLFunction, fn2: APLFunction, operatorAxis: Instruction?, opPos: Position): APLFunctionDescriptor {
        if (operatorAxis != null) {
            throw AxisNotSupported(opPos)
        }
        return ComposedFunctionDescriptor(fn1, fn2)
    }
}

class ReverseComposeFunctionDescriptor(val fn1: APLFunction, val fn2: APLFunction) : APLFunctionDescriptor {
    inner class ReverseComposeFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val res = fn1.eval1Arg(context, a, null)
            return fn2.eval2Arg(context, res, a, null)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val res = fn1.eval1Arg(context, a, null)
            return fn2.eval2Arg(context, res, b, null)
        }

        override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val res = fn1.evalInverse1Arg(context, a, null)
            return fn2.evalInverse2ArgA(context, res, b, null)
        }

        override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val res = fn2.evalInverse2ArgB(context, a, b, null)
            return fn1.evalInverse1Arg(context, res, null)
        }

        fun fn1() = fn1
        fun fn2() = fn2
    }

    override fun make(pos: Position) = ReverseComposeFunctionImpl(pos)
}

class ReverseComposeOp : APLOperatorTwoArg {
    override fun combineFunction(fn1: APLFunction, fn2: APLFunction, operatorAxis: Instruction?, opPos: Position): APLFunctionDescriptor {
        if (operatorAxis != null) {
            throw AxisNotSupported(opPos)
        }
        return ReverseComposeFunctionDescriptor(fn1, fn2)
    }
}

class OverDerivedFunctionDescriptor(val fn1: APLFunction, val fn2: APLFunction) : APLFunctionDescriptor {
    inner class OverDerivedFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val result0 = fn2.eval1Arg(context, a, null)
            return fn1.eval1Arg(context, result0, null)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val result0 = fn2.eval1Arg(context, b, null)
            val result1 = fn2.eval1Arg(context, a, null)
            return fn1.eval2Arg(context, result1, result0, null)
        }

        fun fn1() = fn1
        fun fn2() = fn2

        override val name1Arg = "over [${fn1.name1Arg}, ${fn2.name1Arg}]"
        override val name2Arg = "over [${fn1.name2Arg}, ${fn2.name1Arg}]"
    }

    override fun make(pos: Position) = OverDerivedFunctionImpl(pos)
}


class OverOp : APLOperatorTwoArg {
    override fun combineFunction(fn1: APLFunction, fn2: APLFunction, operatorAxis: Instruction?, opPos: Position): APLFunctionDescriptor {
        if (operatorAxis != null) {
            throw AxisNotSupported(opPos)
        }
        return OverDerivedFunctionDescriptor(fn1, fn2)
    }
}
