package array.builtins

import array.*

class BooleanAndFunction : APLFunctionDescriptor {
    class BooleanAndFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun evalArgsAndCall2Arg(context: RuntimeContext, leftArgs: Instruction, rightArgs: Instruction): APLValue {
            val a = leftArgs.evalWithContext(context)
            return if (a.asBoolean(pos)) {
                rightArgs.evalWithContext(context)
            } else {
                a
            }
        }

        override fun evalArgsAndCall2ArgDiscardResult(context: RuntimeContext, leftArgs: Instruction, rightArgs: Instruction) {
            val a = leftArgs.evalWithContext(context)
            if (a.asBoolean(pos)) {
                rightArgs.evalWithContextAndDiscardResult(context)
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = BooleanAndFunctionImpl(instantiation)
}

class BooleanOrFunction : APLFunctionDescriptor {
    class BooleanOrFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun evalArgsAndCall2Arg(context: RuntimeContext, leftArgs: Instruction, rightArgs: Instruction): APLValue {
            val a = leftArgs.evalWithContext(context)
            return if (a.asBoolean(pos)) {
                a
            } else {
                rightArgs.evalWithContext(context)
            }
        }

        override fun evalArgsAndCall2ArgDiscardResult(context: RuntimeContext, leftArgs: Instruction, rightArgs: Instruction) {
            val a = leftArgs.evalWithContext(context)
            unless(a.asBoolean(pos)) {
                rightArgs.evalWithContext(context)
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = BooleanOrFunctionImpl(instantiation)
}
