package array.builtins

import array.*

class SleepFunction : APLFunctionDescriptor {
    class SleepFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val sleepTimeSeconds = a.ensureNumber(pos).asDouble()
            sleepMillis((sleepTimeSeconds * 1000).toLong())
            return sleepTimeSeconds.makeAPLNumber()
        }
    }

    override fun make(instantiation: FunctionInstantiation) = SleepFunctionImpl(instantiation)
}

class TimeMillisFunction : APLFunctionDescriptor {
    class TimeMillisFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            unless(a.ensureNumber(pos).asInt(pos) == 0) {
                throwAPLException(APLIllegalArgumentException("Argument to timeMillis must be 0", pos))
            }
            return currentTime().makeAPLNumber()
        }

        override val name1Arg get() = "timeMillis"
    }

    override fun make(instantiation: FunctionInstantiation) = TimeMillisFunctionImpl(instantiation)
}

class MakeTimerFunction : APLFunctionDescriptor {
    class MakeTimerFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val delays = b.arrayify().toIntArray(pos)
            val callbacks = a.arrayify().membersSequence().map { v ->
                if (v is LambdaValue) {
                    v
                } else {
                    throwAPLException(APLIllegalArgumentException("Left argument must be a function or a list of functions"))
                }
            }.toList()
            return context.engine.makeTimer(delays, callbacks, pos)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = MakeTimerFunctionImpl(instantiation)
}
