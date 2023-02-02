package array.builtins
//≠∵⌻⍨ ⍳30
import array.*

abstract class BitwiseCombineAPLFunction(pos: Position) : MathCombineAPLFunction(pos) {
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
    override fun combineFunction(fn: APLFunction, pos: Position) = fn.deriveBitwise() ?: throw BitwiseNotSupported(fn.pos)
}

class BitwiseAndFunction : APLFunctionDescriptor {
    class BitwiseAndFunctionImpl(pos: Position) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = a and b
        override val name2Arg get() = "bitwise and"
    }

    override fun make(pos: Position) = BitwiseAndFunctionImpl(pos)
}

class BitwiseOrFunction : APLFunctionDescriptor {
    class BitwiseOrFunctionImpl(pos: Position) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = a or b
        override val name2Arg get() = "bitwise or"
    }

    override fun make(pos: Position) = BitwiseOrFunctionImpl(pos)
}

class BitwiseXorFunction : APLFunctionDescriptor {
    class BitwiseXorFunctionImpl(pos: Position) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = a xor b
        override val name2Arg get() = "bitwise xor"
    }

    override fun make(pos: Position) = BitwiseXorFunctionImpl(pos)
}

class BitwiseNotFunction : APLFunctionDescriptor {
    class BitwiseNotFunctionImpl(pos: Position) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine1Arg(a: Long) = a.inv()
        override val name2Arg get() = "bitwise not"
    }

    override fun make(pos: Position) = BitwiseNotFunctionImpl(pos)
}

class BitwiseNandFunction : APLFunctionDescriptor {
    class BitwiseNandFunctionImpl(pos: Position) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = (a and b).inv()
        override val name2Arg get() = "bitwise nand"
    }

    override fun make(pos: Position) = BitwiseNandFunctionImpl(pos)
}

class BitwiseNorFunction : APLFunctionDescriptor {
    class BitwiseNorFunctionImpl(pos: Position) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = (a or b).inv()
        override val name2Arg get() = "bitwise nor"
    }

    override fun make(pos: Position) = BitwiseNorFunctionImpl(pos)
}

// TODO: Need to assign this to the appropriate parent function
class BitwiseCountBitsFunction : APLFunctionDescriptor {
    class BitwiseCountBitsFunctionImpl(pos: Position) : BitwiseCombineAPLFunction(pos) {
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

    override fun make(pos: Position) = BitwiseCountBitsFunctionImpl(pos)
}
