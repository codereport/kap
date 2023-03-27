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
