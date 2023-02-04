package array

import array.builtins.*
import array.json.JsonAPLModule
import array.syntax.CustomSyntax
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmInline
import kotlin.jvm.Volatile
import kotlin.reflect.KClass

/**
 * Class that holds information about the context in which a function is materialised in the code.
 * In short, an [APLFunctionDescriptor] describes a function itself. When a function call is
 * expressed in code, the [APLFunctionDescriptor.make] function is called to create an instance
 * of [APLFunction] which represents a particular invocation of the function at a particular
 * place in the code. This place is represented by an instance of this class.
 *
 * @param pos The position in the code where the function was materialised
 * @param env The environment where the function is called
 */
class FunctionInstantiation(val pos: Position, val env: Environment) {
    inline fun updatePos(fn: (Position) -> Position): FunctionInstantiation {
        return FunctionInstantiation(fn(pos), env)
    }
}

interface APLFunctionDescriptor {
    fun make(instantiation: FunctionInstantiation): APLFunction
}

@JvmInline
value class OptimisationFlags(val flags: Int) {
    val is1ALong get() = (flags and OPTIMISATION_FLAG_1ARG_LONG) != 0
    val is1ADouble get() = (flags and OPTIMISATION_FLAG_1ARG_DOUBLE) != 0
    val is2ALongLong get() = (flags and OPTIMISATION_FLAG_2ARG_LONG_LONG) != 0
    val is2ADoubleDouble get() = (flags and OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE) != 0

    fun flagsString(): String {
        val flagMap = listOf(
            OPTIMISATION_FLAG_1ARG_LONG to "1ALong",
            OPTIMISATION_FLAG_1ARG_DOUBLE to "1ADouble",
            OPTIMISATION_FLAG_2ARG_LONG_LONG to "2ALongLong",
            OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE to "2ADoubleDouble")
        val flagsString = flagMap.filter { (value, _) -> (flags and value) != 0 }.joinToString(", ") { it.second }
        return "OptimisationFlags(flags=0x${flags.toString(16)}, values: ${flagsString})"
    }

    fun andWith(other: OptimisationFlags) = OptimisationFlags(flags and other.flags)

    @Suppress("unused")
    fun orWith(other: OptimisationFlags) = OptimisationFlags(flags or other.flags)

    val masked1Arg get() = OptimisationFlags(flags and OPTIMISATION_FLAGS_1ARG_MASK)

    @Suppress("unused")
    val masked2Arg get() = OptimisationFlags(flags and OPTIMISATION_FLAGS_2ARG_MASK)

    override fun toString() = flagsString()

    companion object {
        const val OPTIMISATION_FLAG_1ARG_LONG = 0x1
        const val OPTIMISATION_FLAG_1ARG_DOUBLE = 0x2
        const val OPTIMISATION_FLAG_2ARG_LONG_LONG = 0x4
        const val OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE = 0x8

        const val OPTIMISATION_FLAGS_1ARG_MASK = OPTIMISATION_FLAG_1ARG_LONG or OPTIMISATION_FLAG_1ARG_DOUBLE
        const val OPTIMISATION_FLAGS_2ARG_MASK = OPTIMISATION_FLAG_2ARG_LONG_LONG or OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE
    }
}

private const val CORE_NAMESPACE_NAME = "kap"
private const val KEYWORD_NAMESPACE_NAME = "keyword"
private const val DEFAULT_NAMESPACE_NAME = "default"
private const val ANONYMOUS_SYMBOL_NAMESPACE_NAME = "anonymous"

class StorageStack private constructor() {
    val stack = ArrayList<StorageStackFrame>()

    constructor(env: Environment) : this() {
        stack.add(StorageStackFrame(env, "root", null))
    }

    private constructor(prevStack: List<StorageStackFrame>) : this() {
        stack.addAll(prevStack)
    }

    fun copy() = StorageStack(stack)

    inline fun withStackFrame(environment: Environment, name: String, pos: Position, crossinline fn: (StorageStackFrame) -> APLValue): APLValue {
        val frame = StorageStackFrame(environment, name, pos)
        stack.add(frame)
        var result: APLValue
        try {
            result = fn(frame)
        } catch (retValue: ReturnValue) {
            if (retValue.returnEnvironment === environment) {
                result = retValue.value
            } else {
                throw retValue
            }
        } finally {
            val removed = stack.removeLast()
            assertx(removed === frame) { "Removed frame does not match inserted frame" }
            frame.fireReleaseCallbacks()
        }
        return result
    }

    fun findStorage(storageRef: StackStorageRef): VariableHolder {
        val frame = stack[stack.size - storageRef.frameIndex - 1]
        return frame.storageList[storageRef.storageOffset]
    }

    fun currentFrame() = stack.last()

    inner class StorageStackFrame(val environment: Environment, val name: String, val pos: Position?) {
        var storageList: Array<VariableHolder>
        private var releaseCallbacks: MutableList<() -> Unit>? = null

        init {
            val localStorageSize = environment.storageList.size
            val externalStoageList = environment.externalStorageList
            val externalStorageSize = externalStoageList.size
            storageList = Array(localStorageSize + externalStorageSize) { i ->
                if (i < localStorageSize) {
                    VariableHolder()
                } else {
                    val ref = externalStoageList[i - localStorageSize]
                    // We don't subtract 1 from stackIndex here because at this point the element has not been added to the stack yet
                    val stackIndex = stack.size - ref.frameIndex
                    val frame = stack[stackIndex]
                    frame.storageList[ref.storageOffset]
                }
            }
        }

        fun pushReleaseCallback(callback: () -> Unit) {
            val list = releaseCallbacks
            if (list == null) {
                val updated = ArrayList<() -> Unit>()
                updated.add(callback)
                releaseCallbacks = updated
            } else {
                list.add(callback)
            }
        }

        fun fireReleaseCallbacks() {
            val list = releaseCallbacks
            if (list != null) {
                list.asReversed().forEach { fn ->
                    fn()
                }
            }
        }

        fun makeStackFrameDescription(): StackFrameDescription {
            return StackFrameDescription("name:${name}, envName:${environment.name}", pos)
        }
    }

    data class StackFrameDescription(val name: String, val pos: Position?)
}

@OptIn(ExperimentalContracts::class)
inline fun <T> withPossibleSavedStack(frame: StorageStack.StorageStackFrame?, fn: () -> T): T {
    contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
    return if (frame == null) {
        fn()
    } else {
        withSavedStackFrame(frame) {
            fn()
        }
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T> withSavedStackFrame(frame: StorageStack.StorageStackFrame, fn: () -> T): T {
    contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
    currentStack().stack.add(frame)
    try {
        return fn()
    } finally {
        val removed = currentStack().stack.removeLast()
        assertx(removed === frame) { "Removed frame does not match inserted frame" }
    }
}

/**
 * A handler that is registered by [Engine.registerClosableHandler] which is responsible for implementing
 * the backend for the `close` KAP function.
 */
interface ClosableHandler<T : APLValue> {
    /**
     * Close the underlying object.
     */
    fun close(value: T)
}

val threadLocalStorageStackRef = makeMPThreadLocal<StorageStack>()

@OptIn(ExperimentalContracts::class)
inline fun <T> withThreadLocalsUnassigned(fn: () -> T): T {
    contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
    val oldStack = threadLocalStorageStackRef.value
    threadLocalStorageStackRef.value = null
    try {
        return fn()
    } finally {
        threadLocalStorageStackRef.value = oldStack
    }
}

fun currentStack() = threadLocalStorageStackRef.value ?: throw IllegalStateException("Storage stack is not bound")

class Engine(numComputeEngines: Int? = null) {
    private val functions = HashMap<Symbol, APLFunctionDescriptor>()
    private val operators = HashMap<Symbol, APLOperator>()
    private val functionDefinitionListeners = ArrayList<FunctionDefinitionListener>()
    private val functionAliases = HashMap<Symbol, Symbol>()
    private val namespaces = HashMap<String, Namespace>()
    private val customSyntaxSubEntries = HashMap<Symbol, CustomSyntax>()
    private val customSyntaxEntries = HashMap<Symbol, CustomSyntax>()
    private val librarySearchPaths = ArrayList<String>()
    private val modules = ArrayList<KapModule>()
    private val exportedSingleCharFunctions = HashSet<String>()

    val rootEnvironment = Environment("root", null)
    var standardOutput: CharacterOutput = NullCharacterOutput()
    var standardInput: CharacterProvider = NullCharacterProvider()

    val coreNamespace = makeNamespace(CORE_NAMESPACE_NAME, overrideDefaultImport = true)
    val keywordNamespace = makeNamespace(KEYWORD_NAMESPACE_NAME, overrideDefaultImport = true)
    val initialNamespace = makeNamespace(DEFAULT_NAMESPACE_NAME)
    val anonymousSymbolNamespace = makeNamespace(ANONYMOUS_SYMBOL_NAMESPACE_NAME, overrideDefaultImport = true)

    var currentNamespace = initialNamespace
    val closableHandlers = HashMap<KClass<out APLValue>, ClosableHandler<*>>()
    val backgroundDispatcher = makeBackgroundDispatcher(numComputeEngines ?: numCores())
    val inComputeThread = makeMPThreadLocal<Boolean>()

    @Volatile
    private var breakPending = false

    init {
        // Intern the names of all the types in the core namespace.
        // This ensures that code that refers to the unqualified versions of the names pick up the correct symbol.
        APLValueType.values().forEach { aplValueType ->
            coreNamespace.internAndExport(aplValueType.typeName)
        }

        // Other symbols also needs exporting
        for (name in listOf("⍵", "⍺", "⍹", "⍶")) {
            coreNamespace.internAndExport(name)
        }

        // core functions
        registerNativeFunction("+", AddAPLFunction())
        registerNativeFunction("-", SubAPLFunction())
        registerNativeFunction("×", MulAPLFunction())
        registerNativeFunction("÷", DivAPLFunction())
        registerNativeFunction("⋆", PowerAPLFunction())
        registerNativeFunction("⍟", LogAPLFunction())
        registerNativeFunction("⍳", IotaAPLFunction())
        registerNativeFunction("⍴", RhoAPLFunction())
        registerNativeFunction("⊢", IdentityAPLFunction())
        registerNativeFunction("⊣", HideAPLFunction())
        registerNativeFunction("=", EqualsAPLFunction())
        registerNativeFunction("≠", NotEqualsAPLFunction())
        registerNativeFunction("<", LessThanAPLFunction())
        registerNativeFunction(">", GreaterThanAPLFunction())
        registerNativeFunction("≤", LessThanEqualAPLFunction())
        registerNativeFunction("≥", GreaterThanEqualAPLFunction())
        registerNativeFunction("⌷", AccessFromIndexAPLFunction())
        registerNativeFunction("⊂", EncloseAPLFunction())
        registerNativeFunction("⊃", DiscloseAPLFunction())
        registerNativeFunction("∧", AndAPLFunction())
        registerNativeFunction("∨", OrAPLFunction())
        registerNativeFunction(",", ConcatenateAPLFunctionLastAxis())
        registerNativeFunction("⍪", ConcatenateAPLFunctionFirstAxis())
        registerNativeFunction("↑", TakeAPLFunction())
        registerNativeFunction("?", RandomAPLFunction())
        registerNativeFunction("⌽", RotateHorizFunction())
        registerNativeFunction("⊖", RotateVertFunction())
        registerNativeFunction("↓", DropAPLFunction())
        registerNativeFunction("⍉", TransposeFunction())
        registerNativeFunction("⌊", MinAPLFunction())
        registerNativeFunction("⌈", MaxAPLFunction())
        registerNativeFunction("|", ModAPLFunction())
        registerNativeFunction("∘", NullFunction())
        registerNativeFunction("≡", CompareFunction())
        registerNativeFunction("≢", CompareNotEqualFunction())
        registerNativeFunction("∊", MemberFunction())
        registerNativeFunction("⍋", GradeUpFunction())
        registerNativeFunction("⍒", GradeDownFunction())
        registerNativeFunction("⍷", FindFunction())
        registerNativeFunction("⫽", SelectElementsLastAxisFunction())
        registerNativeFunction("/", SelectElementsLastAxisFunction()) // Temporary alias while migrating code
        registerNativeFunction("⌿", SelectElementsFirstAxisFunction())
        registerNativeFunction("\\", ExpandLastAxisFunction())
        registerNativeFunction("⍀", ExpandFirstAxisFunction())
        registerNativeFunction("∼", NotAPLFunction())
        registerNativeFunction("⍕", FormatAPLFunction())
        registerNativeFunction("⍸", WhereAPLFunction())
        registerNativeFunction("∪", UniqueFunction())
        registerNativeFunction("⍲", NandAPLFunction())
        registerNativeFunction("⍱", NorAPLFunction())
        registerNativeFunction("∩", IntersectionAPLFunction())
        registerNativeFunction("!", BinomialAPLFunction())
        registerNativeFunction("⍎", ParseNumberFunction())
        registerNativeFunction("%", CaseFunction())
        registerNativeFunction("⊆", PartitionedEncloseFunction())
        registerNativeFunction("cmp", CompareObjectsFunction())
        registerNativeFunction("→", ReturnFunction())

        // hash tables
        registerNativeFunction("map", MapAPLFunction())
        registerNativeFunction("mapGet", MapGetAPLFunction())
        registerNativeFunction("mapPut", MapPutAPLFunction())
        registerNativeFunction("mapRemove", MapRemoveAPLFunction())
        registerNativeFunction("mapToArray", MapKeyValuesFunction())

        // io functions
        registerNativeFunction("read", ReadFunction(), "io")
        registerNativeFunction("print", PrintAPLFunction(), "io")
        registerNativeFunction("writeCsv", WriteCsvFunction(), "io")
        registerNativeFunction("readCsv", ReadCsvFunction(), "io")
        registerNativeFunction("readFile", ReadFileFunction(), "io")
        registerNativeFunction("load", LoadFunction(), "io")
        registerNativeFunction("httpRequest", HttpRequestFunction(), "io")
        registerNativeFunction("httpPost", HttpPostFunction(), "io")
        registerNativeFunction("readdir", ReaddirFunction(), "io")
        registerNativeFunction("close", CloseAPLFunction())

        // misc functions
        registerNativeFunction("sleep", SleepFunction(), "time")
        registerNativeFunction("throw", ThrowFunction())
        registerNativeOperator("catch", CatchOperator())
        registerNativeFunction("labels", LabelsFunction())
        registerNativeFunction("timeMillis", TimeMillisFunction(), "time")
        registerNativeFunction("unwindProtect", UnwindProtectAPLFunction(), "int")
        registerNativeOperator("defer", DeferAPLOperator())
        registerNativeFunction("ensureGeneric", EnsureTypeFunction(ArrayMemberType.GENERIC), "int")
        registerNativeFunction("ensureLong", EnsureTypeFunction(ArrayMemberType.LONG), "int")
        registerNativeFunction("ensureDouble", EnsureTypeFunction(ArrayMemberType.DOUBLE), "int")
        registerNativeOperator("atLeave", AtLeaveScopeOperator())
        registerNativeFunction("toList", ToListFunction())
        registerNativeFunction("fromList", FromListFunction())

        // maths
        registerNativeFunction("sin", SinAPLFunction(), "math")
        registerNativeFunction("cos", CosAPLFunction(), "math")
        registerNativeFunction("tan", TanAPLFunction(), "math")
        registerNativeFunction("asin", AsinAPLFunction(), "math")
        registerNativeFunction("acos", AcosAPLFunction(), "math")
        registerNativeFunction("atan", AtanAPLFunction(), "math")
        registerNativeFunction("√", SqrtAPLFunction())

        // metafunctions
        registerNativeFunction("typeof", TypeofFunction())
        registerNativeFunction("isLocallyBound", IsLocallyBoundFunction())
        registerNativeFunction("comp", CompFunction())

        // operators
        registerNativeOperator("¨", ForEachOp())
        registerNativeOperator("/", ReduceOpLastAxis())
        registerNativeOperator("⌿", ReduceOpFirstAxis())
        registerNativeOperator("⌻", OuterJoinOp())
        registerNativeOperator(".", OuterInnerJoinOp())
        registerNativeOperator("⍨", CommuteOp())
        registerNativeOperator("⍣", PowerAPLOperator())
        registerNativeOperator("\\", ScanLastAxisOp())
        registerNativeOperator("⍀", ScanFirstAxisOp())
        registerNativeOperator("⍤", RankOperator())
        registerNativeOperator("∵", BitwiseOp())
        registerNativeOperator("∘", ComposeOp())
        registerNativeOperator("⍥", OverOp())
        registerNativeOperator("parallel", ParallelOp())
        registerNativeOperator("∥", ParallelOp())
        registerNativeOperator("⍛", ReverseComposeOp())
        registerNativeOperator("inverse", InverseFnOp())
        registerNativeOperator("˝", InverseFnOp())
        registerNativeOperator("under", StructuralUnderOp())
        registerNativeOperator("⍢", StructuralUnderOp())

        // function aliases                             
        functionAliases[coreNamespace.internAndExport("*")] = coreNamespace.internAndExport("⋆")
        functionAliases[coreNamespace.internAndExport("~")] = coreNamespace.internAndExport("∼")

        platformInit(this)

        addModule(UnicodeModule())
        addModule(JsonAPLModule())
        addModule(RegexpModule())
    }

    fun close() {
        backgroundDispatcher.close()
    }

    fun interruptEvaluation() {
        breakPending = true
    }

    fun checkInterrupted(pos: Position? = null) {
        val pending = breakPending
        if (pending) {
            if (!isInComputeThread) {
                breakPending = false
            }
            throw APLEvaluationInterrupted(pos)
        }
    }

    fun clearInterrupted() {
        breakPending = false
    }

    val isInComputeThread get() = inComputeThread.value == true

    fun addModule(module: KapModule) {
        module.init(this)
        modules.add(module)
    }

    fun addLibrarySearchPath(path: String) {
        val fixedPath = PathUtils.cleanupPathName(path)
        if (!librarySearchPaths.contains(fixedPath)) {
            librarySearchPaths.add(fixedPath)
        }
    }

    fun resolveLibraryFile(requestedFile: String): String? {
        if (requestedFile.isEmpty()) {
            return null
        }
        if (PathUtils.isAbsolutePath(requestedFile)) {
            return null
        }
        librarySearchPaths.forEach { path ->
            val name = "${path}/${requestedFile}"
            if (fileExists(name)) {
                return name
            }
        }
        return null
    }

    fun addFunctionDefinitionListener(listener: FunctionDefinitionListener) {
        functionDefinitionListeners.add(listener)
    }

    @Suppress("unused")
    fun removeFunctionDefinitionListener(listener: FunctionDefinitionListener) {
        functionDefinitionListeners.remove(listener)
    }

    fun registerFunction(name: Symbol, fn: APLFunctionDescriptor) {
        functions[name] = fn
        functionDefinitionListeners.forEach { it.functionDefined(name, fn) }
    }

    private fun registerNativeFunction(name: String, fn: APLFunctionDescriptor, namespaceName: String? = null) {
        val namespace = if (namespaceName == null) coreNamespace else makeNamespace(namespaceName)
        val sym = namespace.internAndExport(name)
        registerFunction(sym, fn)
    }

    fun registerOperator(name: Symbol, fn: APLOperator) {
        operators[name] = fn
        functionDefinitionListeners.forEach { it.operatorDefined(name, fn) }
    }

    private fun registerNativeOperator(name: String, fn: APLOperator, namespaceName: String? = null) {
        val namespace = if (namespaceName == null) coreNamespace else makeNamespace(namespaceName)
        val sym = namespace.internAndExport(name)
        registerOperator(sym, fn)
    }

    fun getFunctions() = functions.toList()

    fun getFunction(name: Symbol) = functions[resolveAlias(name)]
    fun getOperator(name: Symbol) = operators[resolveAlias(name)]

    fun createAnonymousSymbol() = Symbol("<anonymous>", anonymousSymbolNamespace)

    fun parse(source: SourceLocation): Instruction {
        TokenGenerator(this, source).use { tokeniser ->
            exportedSingleCharFunctions.forEach { token ->
                tokeniser.registerSingleCharFunction(token)
            }
            val parser = APLParser(tokeniser)
            return parser.parseValueToplevel(EndOfFile)
        }
    }

    fun parseAndEval(
        source: SourceLocation,
        extraBindings: Map<Symbol, APLValue>? = null,
        allocateThreadLocals: Boolean = true,
        collapseResult: Boolean = true)
            : APLValue {
        if (extraBindings != null) {
            throw IllegalArgumentException("extra bindings is not supported at the moment")
        }
        TokenGenerator(this, source).use { tokeniser ->
            exportedSingleCharFunctions.forEach { token ->
                tokeniser.registerSingleCharFunction(token)
            }
            val parser = APLParser(tokeniser)
            val instr = parser.parseValueToplevel(EndOfFile)
            rootEnvironment.escapeAnalysis()

            fun evalInstrs(): APLValue {
                val result = instr.evalWithContext(RuntimeContext(this))
                return if (collapseResult) {
                    result.collapse()
                } else {
                    result
                }
            }

            return if (allocateThreadLocals) {
                withThreadLocalAssigned {
                    evalInstrs()
                }
            } else {
                recomputeRootFrame()
                evalInstrs()
            }
        }
    }

    fun internSymbol(name: String, namespace: Namespace? = null): Symbol = (namespace ?: currentNamespace).internSymbol(name)

    fun makeNamespace(name: String, overrideDefaultImport: Boolean = false): Namespace {
        return namespaces.getOrPut(name) {
            val namespace = Namespace(name)
            if (!overrideDefaultImport) {
                namespace.addImport(coreNamespace)
            }
            namespace
        }
    }

    private fun resolveAlias(name: Symbol) = functionAliases[name] ?: name

    fun isSelfEvaluatingSymbol(name: Symbol) = name.namespace === keywordNamespace

    fun registerCustomSyntax(customSyntax: CustomSyntax) {
        customSyntaxEntries[customSyntax.name] = customSyntax
    }

    fun syntaxRulesForSymbol(name: Symbol): CustomSyntax? {
        return customSyntaxEntries[name]
    }

    fun registerCustomSyntaxSub(customSyntax: CustomSyntax) {
        customSyntaxSubEntries[customSyntax.name] = customSyntax
    }

    fun customSyntaxSubRulesForSymbol(name: Symbol): CustomSyntax? {
        return customSyntaxSubEntries[name]
    }

    inline fun <T> withSavedNamespace(fn: () -> T): T {
        val oldNamespace = currentNamespace
        try {
            return fn()
        } finally {
            currentNamespace = oldNamespace
        }
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <T> withThreadLocalAssigned(fn: () -> T): T {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        val oldStack = threadLocalStorageStackRef.value
        assertx(oldStack == null) { "Overriding old stack" }
        threadLocalStorageStackRef.value = StorageStack(rootEnvironment)
        try {
            return fn()
        } finally {
            threadLocalStorageStackRef.value = oldStack
        }
    }

    /**
     * Resizes the root frame to accommodate new variable assignments
     */
    fun recomputeRootFrame() {
        if (currentStack().stack.size != 1) {
            throw IllegalStateException("Attempt to recompute the root frame without an empty stack")
        }
        val frame = currentStack().stack[0]
        if (rootEnvironment.externalStorageList.isNotEmpty()) {
            throw IllegalStateException("External storage list for the root environment is not empty")
        }
        val oldStorageList = frame.storageList
        frame.storageList = Array(rootEnvironment.storageList.size) { i ->
            if (i < oldStorageList.size) {
                frame.storageList[i]
            } else {
                VariableHolder()
            }
        }
    }

    fun registerExportedSingleCharFunction(name: String) {
        exportedSingleCharFunctions.add(name)
    }

    inline fun <reified T : APLValue> registerClosableHandler(handler: ClosableHandler<T>) {
        if (closableHandlers.containsKey(T::class)) {
            throw IllegalStateException("Closable handler for class ${T::class.simpleName} already added")
        }
        closableHandlers[T::class] = handler
    }

    inline fun <reified T : APLValue> callClosableHandler(value: T, pos: Position) {
        val handler =
            closableHandlers[value::class] ?: throw APLEvalException("Value cannot be closed: ${value.formatted(FormatStyle.PLAIN)}", pos)
        @Suppress("UNCHECKED_CAST")
        (handler as ClosableHandler<T>).close(value)
    }

    fun resolvePathName(file: String) = resolveDirectoryPath(file, workingDirectory)

    var workingDirectory: String? = currentDirectory()
        get() = field
        set(s) {
            field = if (s != null && fileType(s) != FileNameType.DIRECTORY) {
                "/"
            } else {
                s
            }
        }
}

class CloseAPLFunction : APLFunctionDescriptor {
    class CloseAPLFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val value = a.collapseFirstLevel()
            context.engine.callClosableHandler(value, pos)
            return value
        }

        override val name1Arg: String get() = "close"
    }

    override fun make(instantiation: FunctionInstantiation) = CloseAPLFunctionImpl(instantiation)
}

fun throwAPLException(ex: APLEvalException): Nothing {
    val stack = threadLocalStorageStackRef.value
    if (stack != null) {
        ex.callStack = stack.stack.map(StorageStack.StorageStackFrame::makeStackFrameDescription)
    }
    throw ex
}

expect fun platformInit(engine: Engine)

class VariableHolder {
    var value: APLValue? = null
}

@OptIn(ExperimentalContracts::class)
inline fun withLinkedContext(env: Environment, name: String, pos: Position, crossinline fn: () -> APLValue): APLValue {
    contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
    return currentStack().withStackFrame(env, name, pos) {
        fn()
    }
}

class RuntimeContext(val engine: Engine) {
    fun isLocallyBound(sym: Symbol): Boolean {
        val b = currentStack().currentFrame().environment.findBinding(sym)
        return if (b == null) {
            false
        } else {
            val storage = currentStack().findStorage(StackStorageRef(b))
            storage.value != null
        }
    }

    fun setVar(storageRef: StackStorageRef, value: APLValue) {
        val stack = currentStack()
        val holder = stack.findStorage(storageRef)
        holder.value = value
    }

    fun assignArgs(args: List<StackStorageRef>, a: APLValue, pos: Position? = null) {
        fun checkLength(expectedLength: Int, actualLength: Int) {
            if (expectedLength != actualLength) {
                throwAPLException(IllegalArgumentNumException(expectedLength, actualLength, pos))
            }
        }

        val v = a.unwrapDeferredValue()
        if (v is APLList) {
            checkLength(args.size, v.listSize())
            for (i in args.indices) {
                setVar(args[i], v.listElement(i))
            }
        } else {
            checkLength(args.size, 1)
            setVar(args[0], v)
        }
    }
}

@Suppress("unused")
interface FunctionDefinitionListener {
    fun functionDefined(name: Symbol, fn: APLFunctionDescriptor) = Unit
    fun functionRemoved(name: Symbol) = Unit
    fun operatorDefined(name: Symbol, fn: APLOperator) = Unit
    fun operatorRemoved(name: Symbol) = Unit
}

interface KapModule {
    /**
     * The name of the module.
     */
    val name: String

    /**
     * Initialise the module.
     */
    fun init(engine: Engine)
}
