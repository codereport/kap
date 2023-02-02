package array.builtins

import array.*

class CommuteOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: Position) = CommuteFunctionDescriptor(fn)

    class CommuteFunctionDescriptor(val fn: APLFunction) : APLFunctionDescriptor {
        class CommuteFunctionImpl(pos: Position, fns: List<APLFunction>) : APLFunction(pos, fns) {
            val fn get() = fns[0]

            override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                return fn.eval2Arg(context, a, a, axis)
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

            override fun evalWithStructuralUnder2Arg(baseFn: APLFunction, context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
                return inversibleStructuralUnder2Arg(this, baseFn, context, a, b)
            }
        }

        override fun make(pos: Position): APLFunction {
            return CommuteFunctionImpl(pos, listOf(fn))
        }
    }
}
