package array.builtins

import array.*

class CommuteOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: Position) = CommuteFunctionDescriptor(fn)

    class CommuteFunctionDescriptor(val fn: APLFunction) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            return object : APLFunction(pos) {
                override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                    return fn.eval2Arg(context, a, a, axis)
                }

                override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                    return fn.eval2Arg(context, b, a, axis)
                }

                override fun evalInverse2ArgA(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                    return fn.evalInverse2ArgB(context, b, a, axis)
                }

                override fun evalInverse2ArgB(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                    return fn.evalInverse2ArgA(context, b, a, axis)
                }
            }
        }
    }
}
