package array.builtins
//≠∵⌻⍨ ⍳30
import array.*

abstract class BitwiseCombineAPLFunction(pos: FunctionInstantiation) : MathCombineAPLFunction(pos) {
    override val optimisationFlags get() = OptimisationFlags(OptimisationFlags.OPTIMISATION_FLAG_1ARG_LONG or OptimisationFlags.OPTIMISATION_FLAG_2ARG_LONG_LONG)

    override fun combine1Arg(a: APLSingleValue): APLValue = bitwiseCombine1Arg(a.ensureNumber(pos).asLong(pos)).makeAPLNumber()
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue =
        bitwiseCombine2Arg(a.ensureNumber(pos).asLong(), b.ensureNumber(pos).asLong(pos)).makeAPLNumber()

    override fun combine1ArgLong(a: Long) = bitwiseCombine1Arg(a)
    override fun combine2ArgLong(a: Long, b: Long) = bitwiseCombine2Arg(a, b)

    open fun bitwiseCombine1Arg(a: Long): Long = throwAPLException(Unimplemented1ArgException(pos))
    open fun bitwiseCombine2Arg(a: Long, b: Long): Long = throwAPLException(Unimplemented2ArgException(pos))
}

class BitwiseOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: FunctionInstantiation) = fn.deriveBitwise() ?: throw BitwiseNotSupported(fn.pos)
}

class BitwiseAndFunction : APLFunctionDescriptor {
    class BitwiseAndFunctionImpl(pos: FunctionInstantiation) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = a and b
        override val name2Arg get() = "bitwise and"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseAndFunctionImpl(instantiation)
}

class BitwiseOrFunction : APLFunctionDescriptor {
    class BitwiseOrFunctionImpl(pos: FunctionInstantiation) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = a or b
        override val name2Arg get() = "bitwise or"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseOrFunctionImpl(instantiation)
}

class BitwiseXorFunction : APLFunctionDescriptor {
    class BitwiseXorFunctionImpl(pos: FunctionInstantiation) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = a xor b
        override val name2Arg get() = "bitwise xor"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseXorFunctionImpl(instantiation)
}

class BitwiseNotFunction : APLFunctionDescriptor {
    class BitwiseNotFunctionImpl(pos: FunctionInstantiation) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine1Arg(a: Long) = a.inv()
        override val name2Arg get() = "bitwise not"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseNotFunctionImpl(instantiation)
}

class BitwiseNandFunction : APLFunctionDescriptor {
    class BitwiseNandFunctionImpl(pos: FunctionInstantiation) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = (a and b).inv()
        override val name2Arg get() = "bitwise nand"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseNandFunctionImpl(instantiation)
}

class BitwiseNorFunction : APLFunctionDescriptor {
    class BitwiseNorFunctionImpl(pos: FunctionInstantiation) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = (a or b).inv()
        override val name2Arg get() = "bitwise nor"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseNorFunctionImpl(instantiation)
}

// TODO: Need to assign this to the appropriate parent function
class BitwiseCountBitsFunction : APLFunctionDescriptor {
    class BitwiseCountBitsFunctionImpl(pos: FunctionInstantiation) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine1Arg(a: Long): Long {
            var total = 0L
            repeat(64) { i ->
                if (a and (1L shl i) > 0) {
                    total++
                }
            }
            return total
        }

        override val name1Arg get() = "bitwise count bits"
    }

    override fun make(instantiation: FunctionInstantiation) = BitwiseCountBitsFunctionImpl(instantiation)
}
