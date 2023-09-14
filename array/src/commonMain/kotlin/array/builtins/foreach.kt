package array.builtins

import array.*

class ForEachResult1Arg(
    val context: RuntimeContext,
    val fn: APLFunction,
    val value: APLValue,
    val axis: APLValue?,
    val pos: Position,
    val savedStack: StorageStack.StorageStackFrame?
) : APLArray() {
    override val dimensions = value.dimensions
    override val rank get() = value.rank
    override fun valueAt(p: Int) = withPossibleSavedStack(savedStack) { fn.eval1Arg(context, value.valueAt(p), axis) }
    override val size get() = value.size

    override fun collapseInt(withDiscard: Boolean): APLValue {
        return if (withDiscard) {
            iterateMembers { v ->
                v.collapseInt(withDiscard = true)
            }
            UnusedResultAPLValue
        } else {
            super.collapseInt(withDiscard = false)
        }
    }
}

class ForEachResult2Arg(
    val context: RuntimeContext,
    val fn: APLFunction,
    val arg1: APLValue,
    val arg2: APLValue,
    val axis: APLValue?,
    val pos: Position,
    val savedStack: StorageStack.StorageStackFrame?
) : APLArray() {
    init {
        unless(arg1.dimensions.compareEquals(arg2.dimensions)) {
            throwAPLException(InvalidDimensionsException("Arguments to foreach does not have the same dimensions", pos))
        }
    }

    override val dimensions get() = arg1.dimensions
    override val rank get() = arg1.rank
    override fun valueAt(p: Int) = withPossibleSavedStack(savedStack) { fn.eval2Arg(context, arg1.valueAt(p), arg2.valueAt(p), axis) }
    override val size get() = arg1.size

    override fun collapseInt(withDiscard: Boolean): APLValue {
        return if (withDiscard) {
            iterateMembers { v ->
                v.collapseInt(withDiscard = true)
            }
            UnusedResultAPLValue
        } else {
            super.collapseInt(withDiscard = false)
        }
    }
}

class ForEachFunctionDescriptor(val fnInner: APLFunction) : APLFunctionDescriptor {
    class ForEachFunctionImpl(pos: FunctionInstantiation, fn: APLFunction) : APLFunction(pos, listOf(fn)), ParallelSupported {

        private val saveStackSupport = SaveStackSupport(this)

        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return if (a.isScalar()) {
                return EnclosedAPLValue.make(fn.eval1Arg(context, a.disclose(), null))
            } else {
                ForEachResult1Arg(context, fn, a, axis, pos, saveStackSupport.savedStack())
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return compute2Arg(context, fn, a, b, axis, pos, saveStackSupport.savedStack())
        }

        override fun computeParallelTasks1Arg(
            context: RuntimeContext, numTasks: Int, a: APLValue, axis: APLValue?
        ): ParallelTaskList {
            val res = eval1Arg(context, a, axis)
            return ParallelCompressTaskList.make(res, numTasks, pos)
        }

        override fun computeParallelTasks2Arg(
            context: RuntimeContext, numTasks: Int, a: APLValue, b: APLValue, axis: APLValue?
        ): ParallelTaskList {
            val res = eval2Arg(context, a, b, axis)
            return ParallelCompressTaskList.make(res, numTasks, pos)
        }

        override fun copy(fns: List<APLFunction>) = ForEachFunctionImpl(instantiation, fns[0])

        val fn = fns[0]
    }

    override fun make(instantiation: FunctionInstantiation): APLFunction {
        return ForEachFunctionImpl(instantiation, fnInner)
    }

    companion object {
        fun compute2Arg(
            context: RuntimeContext,
            fn: APLFunction,
            a: APLValue,
            b: APLValue,
            axis: APLValue?,
            pos: Position,
            savedStack: StorageStack.StorageStackFrame?
        ): APLValue {
            if (a.isScalar() && b.isScalar()) {
                return EnclosedAPLValue.make(fn.eval2Arg(context, a.disclose(), b.disclose(), axis).unwrapDeferredValue())
            }
            val a1 = if (a.isScalar()) {
                ConstantArray(b.dimensions, a.disclose())
            } else {
                a
            }
            val b1 = if (b.isScalar()) {
                ConstantArray(a.dimensions, b.disclose())
            } else {
                b
            }
            return ForEachResult2Arg(context, fn, a1, b1, axis, pos, savedStack)
        }
    }
}

class ForEachOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, pos: FunctionInstantiation) = ForEachFunctionDescriptor(fn)
}
