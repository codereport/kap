package array

fun APLParser.processDynamicAssignment(pos: Position, leftArgs: List<Instruction>): ParseResultHolder.InstrParseResult {
    unless(leftArgs.size == 1) {
        throw IncompatibleTypeParseException("Can only assign to a single variable", pos)
    }
    val dest = leftArgs[0]
    if (dest !is VariableRef) {
        throw IncompatibleTypeParseException("Dynamic assignment only works for single variables", pos)
    }
    val (holder, parsedEnv) = withEnvironment("dynamic assignment") { env ->
        Pair(parseValue(), env)
    }
    return when (holder) {
        is ParseResultHolder.InstrParseResult -> makeDynamicAssignInstruction(this, dest, holder, parsedEnv)
        is ParseResultHolder.FnParseResult -> throw IllegalContextForFunction(holder.pos)
        is ParseResultHolder.EmptyParseResult -> throw ParseException(
            "No right-side value in dynamic assignment instruction",
            pos)
    }
}

private fun makeDynamicAssignInstruction(
    parser: APLParser,
    dest: VariableRef,
    holder: ParseResultHolder.InstrParseResult,
    parsedEnv: Environment
): ParseResultHolder.InstrParseResult {

    val env = parser.currentEnvironment()
    env.markCanEscape()
    val freeVariableRefs = parsedEnv.freeVariableRefs()
    val assignmentInstr =
        DynamicAssignmentInstruction.make(dest.storageRef, freeVariableRefs, holder.instr, parsedEnv, holder.pos)
    return ParseResultHolder.InstrParseResult(assignmentInstr, holder.lastToken)
}

private class WeakRefVariableUpdateListener(
    engine: Engine,
    private val debugName: String,
    val storage: VariableHolder,
    inner: VariableUpdateListener
) : VariableUpdateListener {
    private val ref = MPWeakReference.make(inner)

    init {
        storage.registerListener(engine, this)
    }

    override fun updated(newValue: APLValue, oldValue: APLValue?) {
        val l = ref.value
        if (l == null) {
            println("Unregistering listener after GC: name=${debugName}")
            storage.unregisterListener(this)
        } else {
            l.updated(newValue, oldValue)
        }
    }
}

class DynamicAssignmentInstruction private constructor(
    val storageRef: StackStorageRef,
    val vars: List<StackStorageRef>,
    val instr: Instruction,
    val env: Environment,
    pos: Position
) : Instruction(pos) {

    private val lock = MPLock()
    private var tracker: UpdateTracker? = null

    override fun evalWithContext(context: RuntimeContext): APLValue {
        val stack = currentStack()
        val frame = stack.currentFrame()
        val holder = stack.findStorage(storageRef)
        return withLinkedContext(env, "dynamic assignment", pos) {
            val res = instr.evalWithContext(context).collapse()
            lock.withLocked {
                val tr = tracker ?: UpdateTracker(context, vars, instr, holder, env, frame).also { tracker = it }
                tr.makeDynamicValue(res).also { dynamicValue ->
                    holder.updateValue(dynamicValue)
                }
            }
        }
    }

    override fun children() = listOf(instr)
    override fun copy(updatedChildList: List<Instruction>) = DynamicAssignmentInstruction(storageRef, vars, updatedChildList[0], env, pos)

    companion object {
        fun make(
            storageRef: StackStorageRef,
            bindings: List<EnvironmentBinding>,
            instr: Instruction,
            env: Environment,
            pos: Position
        ): DynamicAssignmentInstruction {
            val vars = bindings.map(::StackStorageRef)
            return DynamicAssignmentInstruction(storageRef, vars, instr, env, pos)
        }
    }

    class UpdateTracker(
        val context: RuntimeContext,
        vars: List<StackStorageRef>,
        val instr: Instruction,
        val destinationHolder: VariableHolder,
        val env: Environment,
        val savedFrame: StorageStack.StorageStackFrame
    ) {
        private val listeners: Map<VariableHolder, Pair<WeakRefVariableUpdateListener, VariableUpdateListener>>
        private val destinationListener: Pair<WeakRefVariableUpdateListener, VariableUpdateListener>

        init {
            val listenerMap = HashMap<VariableHolder, Pair<WeakRefVariableUpdateListener, VariableUpdateListener>>()
            vars.forEach { stackRef ->
                val depth = depthOfEnv(stackRef.binding.environment, env)
                val storage = currentStack().findStorageFromFrameIndexAndOffset(
                    if (stackRef.frameIndex == -2) -2 else stackRef.frameIndex - depth,
                    stackRef.storageOffset)
                val innerListener = VariableUpdateListener { newValue, _ ->
                    processUpdate(newValue)
                }
                val listener = WeakRefVariableUpdateListener(context.engine, "TrackedDependency(ref=${stackRef.name})", storage, innerListener)
                listenerMap[storage] = Pair(listener, innerListener)
            }
            listeners = listenerMap

            val innerDestListener = VariableUpdateListener { newValue, _ -> processDestinationUpdated(newValue) }
            destinationListener = Pair(
                WeakRefVariableUpdateListener(context.engine, "UpdateTracker(env=${env.name})", destinationHolder, innerDestListener), innerDestListener)
        }

        private fun processDestinationUpdated(newValue: APLValue) {
            if (newValue !is DynamicValue || newValue.tracker !== this) {
                listeners.forEach { (holder, listener) ->
                    val wasFound = holder.unregisterListener(listener.first)
                    assertx(wasFound) {
                        "Listener was not found when attempting to unregister tracker: value=${newValue}, tracker=${this}"
                    }
                }
                destinationHolder.unregisterListener(destinationListener.first)
            }
        }

        private fun processUpdate(newValue: APLValue) {
            val updateId = computeUpdateId(newValue)
            destinationHolder.updateValue(DynamicValue(context, this, updateId = updateId))
        }

        private fun computeUpdateId(newValue: APLValue): UpdateId {
            if (newValue is DynamicValue) {
                val oldDest = destinationHolder.value()
                if (oldDest is DynamicValue) {
                    val id = oldDest.updateId
                    if (newValue.updateId === id) {
                        destinationHolder.updateValueNoPropagate(APLNullValue.APL_NULL_INSTANCE)
                        throwAPLException(CircularDynamicAssignment(instr.pos))
                    } else {
                        return newValue.updateId
                    }
                }
            }
            return UpdateId()
        }

        fun makeDynamicValue(res: APLValue): DynamicValue {
            return DynamicValue(context, this, res)
        }
    }

    class UpdateId {
        val id = curr++

        override fun toString() = "UpdateId(${id})"

        companion object {
            private var curr = 0
        }
    }

    class DynamicValue(
        val context: RuntimeContext,
        val tracker: UpdateTracker,
        initial: APLValue? = null,
        val updateId: UpdateId = UpdateId()
    ) : AbstractDelegatedValue() {
        private var curr: APLValue? = initial
        private val lock = MPLock()

        override val value: APLValue
            get() = lock.withLocked {
                curr ?: computeValue()
            }

        private fun computeValue(): APLValue {
            val v = withSavedStackFrame(tracker.savedFrame) {
                withLinkedContext(tracker.env, "dynamic assignment", tracker.instr.pos) {
                    tracker.instr.evalWithContext(context)
                }
            }
            curr = v
            return v
        }

        override fun toString() = "DynamicValue(${curr})"
    }
}
