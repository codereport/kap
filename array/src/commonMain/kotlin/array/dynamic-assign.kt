package array

fun APLParser.processDynamicAssignment(pos: Position, leftArgs: List<Instruction>): ParseResultHolder.InstrParseResult {
    unless(leftArgs.size == 1) {
        throw IncompatibleTypeParseException("Can only assign to a single variable", pos)
    }
    val dest = leftArgs[0]
    if (dest !is VariableRef) {
        throw IncompatibleTypeParseException("Dynamic assignment only works for single variables", pos)
    }
    withEnvironment {
        return when (val holder = parseValue()) {
            is ParseResultHolder.InstrParseResult -> makeDynamicAssignInstruction(this, dest, holder)
            is ParseResultHolder.FnParseResult -> throw IllegalContextForFunction(holder.pos)
            is ParseResultHolder.EmptyParseResult -> throw ParseException("No right-side value in dynamic assignment instruction", pos)
        }
    }
}

private fun makeDynamicAssignInstruction(parser: APLParser, dest: VariableRef, holder: ParseResultHolder.InstrParseResult): ParseResultHolder.InstrParseResult {
    val env = parser.currentEnvironment()
    env.canEscape()
    val freeVariableRefs = env.freeVariableRefs()
    println("free vars = ${freeVariableRefs}")
    val assignmentInstr = DynamicAssignmentInstruction(dest.storageRef, freeVariableRefs, holder.instr, env, holder.pos)
    return ParseResultHolder.InstrParseResult(assignmentInstr, holder.lastToken)
}

class DynamicAssignmentInstruction(
    val storageRef: StackStorageRef,
    bindings: List<EnvironmentBinding>,
    val instr: Instruction,
    val env: Environment,
    pos: Position
) : Instruction(pos) {

    private val vars = bindings.map(::StackStorageRef)
    private val lock = MPLock()
    private var tracker: UpdateTracker? = null

    override fun evalWithContext(context: RuntimeContext): APLValue {
        val holder = currentStack().findStorage(storageRef)
        return withLinkedContext(env, "dynamic assignment", pos) {
            val res = instr.evalWithContext(context).collapse()
            holder.value = res
            lock.withLocked {
                val tr = tracker
                if (tr == null) {
                    tracker = UpdateTracker(context, vars, instr, holder)
                }
            }
            res
        }
    }

    override fun children() = listOf(instr)

    class UpdateTracker(val context: RuntimeContext, vars: List<StackStorageRef>, val instr: Instruction, val destinationHolder: VariableHolder) {
        private val savedFrame: StorageStack.StorageStackFrame = currentStack().currentFrame()
        private val listeners: Map<VariableHolder, VariableUpdateListener>
        private val destinationListener: VariableUpdateListener

        init {
            val listenerMap = HashMap<VariableHolder, VariableUpdateListener>()
            vars.forEach { stackRef ->
                val storage = currentStack().findStorage(stackRef)
                val listener = VariableUpdateListener {
                    println("value updated: ${it}")
                    processUpdate()
                }
                storage.registerListener(listener)
                listenerMap[storage] = listener
            }
            listeners = listenerMap
            destinationListener = VariableUpdateListener { newValue -> processDestinationUpdated(newValue) }
            destinationHolder.registerListener(destinationListener)
        }

        private fun processDestinationUpdated(newValue: APLValue) {
            if (newValue !is DynamicValue || newValue.tracker !== this) {
                listeners.forEach { (holder, listener) ->
                    holder.unregisterListener(listener)
                }
                destinationHolder.unregisterListener(destinationListener)
            }
        }

        private fun processUpdate() {
            destinationHolder.value = DynamicValue(context, instr, savedFrame, this)
        }
    }

    class DynamicValue(val context: RuntimeContext, val instr: Instruction, val savedFrame: StorageStack.StorageStackFrame, val tracker: UpdateTracker) :
            AbstractDelegatedValue() {
        private var curr: APLValue? = null
        private val lock = MPLock()

        override val value: APLValue
            get() = lock.withLocked {
                println("getting dynamic value: ${curr}")
                curr ?: computeValue()
            }

        private fun computeValue(): APLValue {
            println("Recomputing value")
            val v = withSavedStackFrame(savedFrame) {
                instr.evalWithContext(context)
            }
            curr = v
            return v
        }
    }
}
