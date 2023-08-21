package array

import array.builtins.*
import array.json.JsonAPLModule
import array.syntax.CustomSyntax
import kotlin.concurrent.Volatile
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmInline
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

    fun maybeMarkEscape(vararg fns: APLFunction) {
        if (fns.any { it.allCapturedEnvironments().isNotEmpty() }) {
            env.markCanEscape()
        }
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

    constructor(rootFrame: StorageStackFrame) : this() {
        stack.add(rootFrame)
    }

    constructor(prevStack: List<StorageStackFrame>) : this() {
        stack.addAll(prevStack)
    }

    fun copy() = StorageStack(stack)

    inline fun withStackFrame(environment: Environment, name: String, pos: Position, crossinline fn: (StorageStackFrame) -> APLValue): APLValue {
        val frame = StorageStackFrame(this, environment, name, pos)
        stack.add(frame)
        var result: APLValue
        @Suppress("LiftReturnOrAssignment")
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
        return findStorageFromFrameIndexAndOffset(storageRef.frameIndex, storageRef.storageOffset)
    }

    fun findStorageFromFrameIndexAndOffset(frameIndex: Int, storageOffset: Int): VariableHolder {
        val frame = stack[if (frameIndex == -2) 0 else stack.size - frameIndex - 1]
        return frame.storageList[storageOffset]
    }

    fun currentFrame() = stack.last()

    class StorageStackFrame private constructor(val environment: Environment, val name: String, val pos: Position?, var storageList: Array<VariableHolder>) {
        private var releaseCallbacks: MutableList<() -> Unit>? = null

        constructor(stack: StorageStack, environment: Environment, name: String, pos: Position?)
                : this(environment, name, pos, computeStorageList(stack, environment))

        constructor(environment: Environment)
                : this(environment, "root", null, computeRootFrame(environment))

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

private inline fun computeStorageList(stack: StorageStack, environment: Environment): Array<VariableHolder> {
    val localStorageSize = environment.storageList.size
    val externalStorageList = environment.externalStorageList
    val externalStorageSize = externalStorageList.size
    return Array(localStorageSize + externalStorageSize) { i ->
        if (i < localStorageSize) {
            VariableHolder()
        } else {
            val ref = externalStorageList[i - localStorageSize]
            // We don't subtract 1 from stackIndex here because at this point the element has not been added to the stack yet
            val stackIndex = if (ref.frameIndex == -2) 0 else stack.stack.size - ref.frameIndex
            val frame = stack.stack[stackIndex]
            frame.storageList[ref.storageOffset]
        }
    }
}

private fun computeRootFrame(env: Environment): Array<VariableHolder> {
    assertx(env.externalStorageList.isEmpty()) { "Root frame should not have external refs" }
    return Array(env.storageList.size) { VariableHolder() }
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

interface SystemParameterProvider {
    fun lookupValue(): APLValue
    fun updateValue(newValue: APLValue, pos: Position): Unit = throwAPLException(UnmodifiableSystemParameterException(pos))
}

class ConstantStringSystemParameterProvider(val value: String) : SystemParameterProvider {
    override fun lookupValue() = APLString(value)
}

class ConstantSymbolSystemParameterProvider(val name: Symbol) : SystemParameterProvider {
    override fun lookupValue() = APLSymbol(name)
}

class Engine(numComputeEngines: Int? = null) {
    private val functions = HashMap<Symbol, APLFunctionDescriptor>()
    private val operators = HashMap<Symbol, APLOperator>()
    private val functionDefinitionListeners = ArrayList<FunctionDefinitionListener>()
    private val functionAliases = HashMap<Symbol, Symbol>()
    private val namespaces = HashMap<String, Namespace>()
    private val customSyntaxSubEntries = HashMap<Symbol, CustomSyntax>()
    private val customSyntaxEntries = HashMap<Symbol, CustomSyntax>()
    private val librarySearchPaths = ArrayList<String>()
    val modules = ArrayList<KapModule>()
    private val exportedSingleCharFunctions = initSingleCharFunctionList()

    var customRenderer: LambdaValue? = null
    val classManager = ClassManager(this)

    /**
     * Lock that protects the update listener list for any variable.
     * This is a global lock rather than a lock on the [VariableHolder] itself
     * in order to minimise the amount of work needed to initialise an instance
     * of [VariableHolder]. Since registering listeners on variables is such
     * a rare event, this is acceptable.
     */
    val updateListenerLock = MPLock()

    val rootEnvironment = Environment("root", null)
    val rootStackFrame = StorageStack.StorageStackFrame(rootEnvironment)
    var standardOutput: CharacterOutput = NullCharacterOutput()
    var standardInput: CharacterProvider = NullCharacterProvider()
    private val timerHandler: TimerHandler? = makeTimerHandler(this)

    val coreNamespace = makeNamespace(CORE_NAMESPACE_NAME, overrideDefaultImport = true)
    val keywordNamespace = makeNamespace(KEYWORD_NAMESPACE_NAME, overrideDefaultImport = true)
    val initialNamespace = makeNamespace(DEFAULT_NAMESPACE_NAME)
    val anonymousSymbolNamespace = makeNamespace(ANONYMOUS_SYMBOL_NAMESPACE_NAME, overrideDefaultImport = true)

    var currentNamespace = initialNamespace
    val closableHandlers = HashMap<KClass<out APLValue>, ClosableHandler<*>>()
    val backgroundDispatcher = makeBackgroundDispatcher(numComputeEngines ?: numCores())
    val inComputeThread = makeMPThreadLocal<Boolean>()
    val systemParameters = HashMap<Symbol, SystemParameterProvider>()
    val standardSymbols = StandardSymbols(this)
    val nativeData = makeNativeData()

    @Volatile
    private var breakPending = false

    init {
        // Intern the names of all the types in the core namespace.
        // This ensures that code that refers to the unqualified versions of the names pick up the correct symbol.
        APLValueType.entries.forEach { aplValueType ->
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
        registerNativeFunction("⊇", PickAPLFunction())
        registerNativeFunction("cmp", CompareObjectsFunction())
        registerNativeFunction("→", ReturnFunction())
        registerNativeFunction("floorc", ComplexFloorFunction())
        registerNativeFunction("ceilc", ComplexCeilFunction())

        // hash tables
        registerNativeFunction("map", MapAPLFunction())
        registerNativeFunction("mapGet", MapGetAPLFunction())
        registerNativeFunction("mapPut", MapPutAPLFunction())
        registerNativeFunction("mapRemove", MapRemoveAPLFunction())
        registerNativeFunction("mapToArray", MapKeyValuesFunction())
        registerNativeFunction("mapSize", MapSizeFunction())

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
        registerNativeFunction("timeMillis", TimeMillisFunction(), "time")
        registerNativeFunction("makeTimer", MakeTimerFunction(), "time")
        registerNativeFunction("throw", ThrowFunction())
        registerNativeOperator("catch", CatchOperator())
        registerNativeFunction("labels", LabelsFunction())
        registerNativeFunction("unwindProtect", UnwindProtectAPLFunction(), "int")
        registerNativeOperator("defer", DeferAPLOperator())
        registerNativeFunction("ensureGeneric", EnsureTypeFunction(ArrayMemberType.GENERIC), "int")
        registerNativeFunction("ensureLong", EnsureTypeFunction(ArrayMemberType.LONG), "int")
        registerNativeFunction("ensureDouble", EnsureTypeFunction(ArrayMemberType.DOUBLE), "int")
        registerNativeFunction("asBigint", AsBigintFunction(), "int")
        registerNativeOperator("atLeave", AtLeaveScopeOperator())
        registerNativeFunction("toList", ToListFunction())
        registerNativeFunction("fromList", FromListFunction())
        registerNativeFunction("proto", AssignPrototypeFunction(), "int")
        registerNativeFunction("toBoolean", ToBooleanFunction())

        // maths
        registerNativeFunction("sin", SinAPLFunction(), "math")
        registerNativeFunction("cos", CosAPLFunction(), "math")
        registerNativeFunction("tan", TanAPLFunction(), "math")
        registerNativeFunction("asin", AsinAPLFunction(), "math")
        registerNativeFunction("acos", AcosAPLFunction(), "math")
        registerNativeFunction("atan", AtanAPLFunction(), "math")
        registerNativeFunction("√", SqrtAPLFunction())
        registerNativeFunction("gcd", GcdAPLFunction(), "math")
        registerNativeFunction("lcm", LcmAPLFunction(), "math")
        registerNativeFunction("numerator", NumeratorAPLFunction(), "math")
        registerNativeFunction("denominator", DenominatorAPLFunction(), "math")

        // metafunctions
        registerNativeFunction("typeof", TypeofFunction())
        registerNativeFunction("isLocallyBound", IsLocallyBoundFunction())
        registerNativeFunction("comp", CompFunction())
        registerNativeFunction("sysparam", SystemParameterFunction())
        registerNativeFunction("internalValueInfo", InternalValueInfoFunction(), "int")

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

        classManager.init()

        systemParameters[standardSymbols.renderer] = CustomRendererParameter(this)

        platformInit(this)

        addModule(UnicodeModule())
        addModule(JsonAPLModule())
        addModule(RegexpModule())
    }

    private fun initSingleCharFunctionList(): MutableSet<String> {
        return hashSetOf(
            "!", "#", "%", "&", "*", "+", ",", "-", "/", "<", "=", ">", "?", "^", "|",
            "~", "¨", "×", "÷", "↑", "→", "↓", "∊", "∘", "∧", "∨", "∩", "∪", "∼", "≠", "≡",
            "≢", "≤", "≥", "⊂", "⊃", "⊖", "⊢", "⊣", "⊤", "⊥", "⋆", "⌈", "⌊", "⌶", "⌷", "⌹",
            "⌻", "⌽", "⌿", "⍀", "⍉", "⍋", "⍎", "⍒", "⍕", "⍞", "⍟", "⍠", "⍣", "⍤", "⍥",
            "⍨", "⍪", "⍫", "⍱", "⍲", "⍳", "⍴", "⍵", "⍶", "⍷", "⍸", "⍹", "⍺", "◊",
            "○", "$", "¥", "χ", "\\", ".", "∵", "⍓", "⫽", "⑊", "⊆", "⊇", "⍥", "∥", "⍛", "˝",
            "⍢", "√")
    }

    fun charIsSingleCharExported(ch: String) = exportedSingleCharFunctions.contains(ch)

    fun close() {
        backgroundDispatcher.close()
    }

    fun interruptEvaluation() {
        breakPending = true
    }

    fun checkInterrupted(pos: Position? = null) {
        val pending = breakPending
        if (pending || nativeBreakPending(this)) {
            if (!isInComputeThread) {
                clearInterrupted()
            }
            throw APLEvaluationInterrupted(pos)
        }
    }

    fun clearInterrupted() {
        breakPending = false
        nativeUpdateBreakPending(this, false)
    }

    val isInComputeThread get() = inComputeThread.value == true

    fun addModule(module: KapModule) {
        module.init(this)
        modules.add(module)
    }

    inline fun <reified T : KapModule> findModule(): T? {
        return modules.filterIsInstance<T>().firstOrNull()
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

    fun createAnonymousSymbol(name: String? = null) = Symbol(if (name == null) "<anonymous>" else "<anonymous: ${name}>", anonymousSymbolNamespace)

    fun parse(source: SourceLocation): Instruction {
        TokenGenerator(this, source).use { tokeniser ->
            val parser = APLParser(tokeniser)
            return parser.parseValueToplevel(EndOfFile)
        }
    }

    fun parseAndEvalWithFormat(
        source: SourceLocation,
        extraBindings: Map<Symbol, APLValue>? = null,
    ): Pair<APLValue, List<String>> {
        return parseAndEvalWithPostProcessing(source, extraBindings = extraBindings) { engine, context, result ->
            val collapsed = result.collapse()
            val renderedResult = renderResult(context, collapsed)
            val list = ArrayList<String>()
            formatResult(renderedResult) { s ->
                list.add(s)
            }
            Pair(collapsed, list)
        }
    }

    fun parseAndEval(
        source: SourceLocation,
        extraBindings: Map<Symbol, APLValue>? = null,
        collapseResult: Boolean = true
    ): APLValue {
        return parseAndEvalWithPostProcessing(source, extraBindings = extraBindings) { engine, context, result ->
            if (collapseResult) {
                result.collapse()
            } else {
                result
            }
        }
    }

    fun <T> parseAndEvalWithPostProcessing(
        source: SourceLocation,
        extraBindings: Map<Symbol, APLValue>? = null,
        postProcess: (Engine, RuntimeContext, APLValue) -> T
    ): T {
        if (extraBindings != null) {
            throw IllegalArgumentException("extra bindings is not supported at the moment")
        }
        TokenGenerator(this, source).use { tokeniser ->
            val parser = APLParser(tokeniser)
            val instr = parser.parseValueToplevel(EndOfFile)
            rootEnvironment.escapeAnalysis()

            withThreadLocalAssigned {
                recomputeRootFrame()
                val context = RuntimeContext(this)
                val result = instr.evalWithContext(context)
                return postProcess(tokeniser.engine, context, result)
            }
        }
    }

    fun internSymbol(name: String, namespace: Namespace? = null): Symbol =
        (namespace ?: currentNamespace).internSymbol(name)

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

    @OptIn(ExperimentalContracts::class)
    inline fun <T> withSavedNamespace(fn: () -> T): T {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        val oldNamespace = currentNamespace
        try {
            return fn()
        } finally {
            currentNamespace = oldNamespace
        }
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <T> withCurrentNamespace(namespace: Namespace, fn: () -> T): T {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        withSavedNamespace {
            currentNamespace = namespace
            return fn()
        }
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <T> withThreadLocalAssigned(fn: () -> T): T {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        val oldStack = threadLocalStorageStackRef.value
        assertx(oldStack == null) { "Overriding old stack" }
        threadLocalStorageStackRef.value = StorageStack(rootStackFrame)
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
        val frame = rootStackFrame
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
            closableHandlers[value::class] ?: throwAPLException(APLEvalException("Value cannot be closed: ${value.formatted(FormatStyle.PLAIN)}", pos))
        @Suppress("UNCHECKED_CAST")
        (handler as ClosableHandler<T>).close(value)
    }

    fun resolvePathName(file: String) = resolveDirectoryPath(file, workingDirectory)

    fun makeTimer(delays: IntArray, callbacks: List<LambdaValue>, pos: Position): APLValue {
        val handler = timerHandler ?: throwAPLException(APLEvalException("Backend does not support timers", pos))
        return handler.registerTimer(delays, callbacks)
    }

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
    @Volatile
    private var value: APLValue? = null

    fun value() = value

    fun updateValue(newValue: APLValue?) {
        val oldValue = value
        value = newValue
        if (newValue != null && oldValue !== newValue) {
            fireListeners(newValue, oldValue)
        }
    }

    fun updateValueNoPropagate(newValue: APLValue?) {
        value = newValue
    }

    // The listener registration is thread-safe, but the rest of variable management is not.
    // The lack of thread-safety for variables was intentional, as the responsibility for preventing issues
    // is on the programmer. Listener registrations are outside the direct influence of the programmer, which
    // requires it to be thread-safe.
    private var listeners: MTSafeArrayList<VariableUpdateListener>? = null

    private fun fireListeners(newValue: APLValue, oldValue: APLValue?) {
        listeners?.forEach { listener -> listener.updated(newValue, oldValue) }
    }

    fun registerListener(engine: Engine, listener: VariableUpdateListener) {
        engine.updateListenerLock.withLocked {
            val listenersCopy = listeners
            val list = if (listenersCopy == null) {
                val newListenerList = MTSafeArrayList<VariableUpdateListener>()
                listeners = newListenerList
                newListenerList
            } else {
                listenersCopy
            }
            list.add(listener)
        }
    }

    fun unregisterListener(listener: VariableUpdateListener): Boolean {
        return listeners?.remove(listener) ?: false
    }
}

fun interface VariableUpdateListener {
    fun updated(newValue: APLValue, oldValue: APLValue?)
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
            storage.value() != null
        }
    }

    fun setVar(storageRef: StackStorageRef, value: APLValue) {
        val stack = currentStack()
        val holder = stack.findStorage(storageRef)
        holder.updateValue(value)
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

class StandardSymbols(val engine: Engine) {
    val platform by lazy { engine.internSymbol("platform", engine.coreNamespace) }
    val renderer by lazy { engine.internSymbol("renderer", engine.coreNamespace) }
    val nullKeyword by lazy { engine.internSymbol("null", engine.keywordNamespace) }
}
