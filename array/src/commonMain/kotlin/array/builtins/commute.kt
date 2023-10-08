package array.builtins

import array.*

class CommuteOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: FunctionInstantiation) = CommuteFunctionDescriptor(fn)

    class CommuteFunctionDescriptor(val fn: APLFunction) : APLFunctionDescriptor {
        class CommuteFunctionImpl(pos: FunctionInstantiation, fns: List<APLFunction>) : APLFunction(pos, fns) {
            val fn get() = fns[0]

            override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                val a0 = a.collapse()
                return fn.eval2Arg(context, a0, a0, axis)
            }

            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                return fn.eval2Arg(context, b, a, axis)
            }

            override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                return fn.evalInverse2ArgA(context, b, a, axis)
            }

            override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                return fn.evalInverse2ArgB(context, b, a, axis)
            }

            override fun evalWithStructuralUnder2Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                return inversibleStructuralUnder2Arg(this, baseFn, context, a, b, axis)
            }

            override val optimisationFlags = run {
                val a2 = fn.optimisationFlags.masked2Arg
                a2.orWith(OptimisationFlags(if (a2.is2ALongLong) OptimisationFlags.OPTIMISATION_FLAG_1ARG_LONG else 0))
                    .orWith(OptimisationFlags(if (a2.is2ADoubleDouble) OptimisationFlags.OPTIMISATION_FLAG_1ARG_DOUBLE else 0))
            }

            override fun eval1ArgLong(context: RuntimeContext, a: Long, axis: APLValue?): Long {
                return fn.eval2ArgLongToLongWithAxis(context, a, a, axis)
            }

            override fun eval1ArgDouble(context: RuntimeContext, a: Double, axis: APLValue?): Double {
                return fn.eval2ArgDoubleToDoubleWithAxis(context, a, a, axis)
            }

            override fun eval2ArgLongToLongWithAxis(context: RuntimeContext, a: Long, b: Long, axis: APLValue?): Long {
                return fn.eval2ArgLongToLongWithAxis(context, b, a, axis)
            }

            override fun eval2ArgDoubleToDoubleWithAxis(context: RuntimeContext, a: Double, b: Double, axis: APLValue?): Double {
                return fn.eval2ArgDoubleToDoubleWithAxis(context, b, a, axis)
            }
        }

        override fun make(instantiation: FunctionInstantiation): APLFunction {
            return CommuteFunctionImpl(instantiation, listOf(fn))
        }
    }
}
