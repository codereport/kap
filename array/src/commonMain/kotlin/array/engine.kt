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

interface APLFunctionDescriptor {
    fun make(pos: Position): APLFunction
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

class ThreadLocalCallStack(val engine: Engine) {
    val callStack = ArrayList<CallStackElement>()
}

data class CallStackElement(val name: String, val pos: Position?)

val threadLocalCallstackRef = makeMPThreadLocal<ThreadLocalCallStack>()

class ContextStack(toplevel: RuntimeContext) {
    val stack = arrayListOf(toplevel)
}

val threadLocalContextStack = makeMPThreadLocal<ContextStack>()

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

    val rootContext = RuntimeContext(this, Environment(0))
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
        registerNativeFunction("→", ThrowFunction())
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

    private fun parseAndEvalNewContext(source: SourceLocation, extraBindings: Map<Symbol, APLValue>? = null): APLValue {
        withThreadLocalAssigned {
            TokenGenerator(this, source).use { tokeniser ->
                exportedSingleCharFunctions.forEach { token ->
                    tokeniser.registerSingleCharFunction(token)
                }
                val parser = APLParser(tokeniser)
                withSavedNamespace {
                    val (newInstr, mapped) = parser.withEnvironment { env ->
                        val mapped = extraBindings?.map { e ->
                            Pair(env.bindLocal(e.key), e.value)
                        }
                        val instr = parser.parseValueToplevel(EndOfFile)
                        val newInstr = RootEnvironmentInstruction(parser.currentEnvironment(), instr, instr.pos)
                        Pair(newInstr, mapped)
                    }
                    return newInstr.evalWithNewContext(this, mapped)
                }
            }
        }
    }

    private fun parseAndEvalNoContext(source: SourceLocation): APLValue {
        withThreadLocalAssigned {
            TokenGenerator(this, source).use { tokeniser ->
                exportedSingleCharFunctions.forEach { token ->
                    tokeniser.registerSingleCharFunction(token)
                }
                val parser = APLParser(tokeniser)
                val instr = parser.parseValueToplevel(EndOfFile)
                rootContext.reinitRootBindings()
                return instr.evalWithContext(rootContext)
            }
        }
    }

    fun parseAndEval(source: SourceLocation, newContext: Boolean = true, extraBindings: Map<Symbol, APLValue>? = null): APLValue {
        return if (newContext) {
            parseAndEvalNewContext(source, extraBindings)
        } else {
            if (extraBindings != null) {
                throw IllegalArgumentException("newContext is required when specifying bindings")
            }
            parseAndEvalNoContext(source)
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
    inline fun <T> withCallStackElement(name: String, pos: Position, fn: (CallStackElement) -> T): T {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        checkInterrupted(pos)
        val threadLocalCallStack = threadLocalCallstackRef.value
        assertx(threadLocalCallStack != null) { "threadLocalCallStack is null" }
        val callStack = threadLocalCallStack.callStack
        if (callStack.size >= 100) {
            throwAPLException(APLEvalException("Stack overflow", pos))
        }
        val callStackElement = CallStackElement(name, pos)
        callStack.add(callStackElement)
        val prevSize = callStack.size

        try {
            return fn(callStackElement)
        } finally {
            assertx(prevSize == callStack.size) { "previous size is not the same as the callstack size" }
            val removedElement = callStack.removeLast()
            assertx(removedElement === callStackElement)
        }
    }

    inline fun <T> withThreadLocalAssigned(fn: () -> T): T {
        val oldThreadLocal = threadLocalCallstackRef.value
        threadLocalCallstackRef.value = ThreadLocalCallStack(this)
        val oldContextStack = threadLocalContextStack.value
        threadLocalContextStack.value = ContextStack(rootContext)
        try {
            return fn()
        } finally {
            threadLocalContextStack.value = oldContextStack
            threadLocalCallstackRef.value = oldThreadLocal
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
    class CloseAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val value = a.collapseFirstLevel()
            context.engine.callClosableHandler(value, pos)
            return value
        }

        override val name1Arg: String get() = "close"
    }

    override fun make(pos: Position) = CloseAPLFunctionImpl(pos)
}

fun throwAPLException(ex: APLEvalException): Nothing {
    val threadLocalCallStack = threadLocalCallstackRef.value
    if (threadLocalCallStack != null) {
        ex.callStack = threadLocalCallStack.callStack.map { e -> e.copy() }
    }
    throw ex
}

expect fun platformInit(engine: Engine)

class VariableHolder {
    var value: APLValue? = null
}

class RuntimeContext(val engine: Engine, val environment: Environment) {
    private val localVariables = HashMap<EnvironmentBinding, VariableHolder>()
    private var releaseCallbacks: MutableList<() -> Unit>? = null

    init {
        initBindings()
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

    private fun initBindings() {
        val localBindings = environment.localBindings()
        if (localBindings.isNotEmpty()) {
            val contextStack = lookupContextStack()
            environment.localBindings().forEach { b ->
                val holder = if (b.environment === environment) {
                    VariableHolder()
                } else {
//                fun recurse(c: RuntimeContext?): VariableHolder {
//                    if (c == null) {
//                        throw IllegalStateException("Can't find binding in parents")
//                    }
//                    return c.localVariables[b] ?: recurse(c.parent)
//                }
//                recurse(parent)
                    val holder = contextStack.stack[b.environment.index].localVariables[b]
                    if (holder == null) {
                        throw IllegalStateException("Variable ${b.name.nameWithNamespace()} was not found in stack")
                    }
                    holder
                }
                localVariables[b] = holder
            }
        }
    }

    fun reinitRootBindings() {
        environment.localBindings().forEach { b ->
            if (localVariables[b] == null) {
                localVariables[b] = VariableHolder()
            }
        }
    }

    fun isLocallyBound(sym: Symbol): Boolean {
        // TODO: This hack is needed for the KAP function isLocallyBound to work. A better strategy is needed.
        val holder = localVariables.entries.find { it.key.name === sym } ?: return false
        return holder.value.value != null
    }

    fun findVariables() = localVariables.map { (k, v) -> k.name to v.value }.toList()

    private fun findOrThrow(name: EnvironmentBinding): VariableHolder {
        return localVariables[name]
            ?: throw IllegalStateException("Attempt to set the value of a nonexistent binding: ${name}")
    }

    fun setVar(name: EnvironmentBinding, value: APLValue) {
        this.findOrThrow(name).value = value
    }

    fun getVar(binding: EnvironmentBinding): APLValue? = findOrThrow(binding).value

    @OptIn(ExperimentalContracts::class)
    inline fun <T> withLinkedContext(env: Environment, name: String, pos: Position, fn: (RuntimeContext) -> T): T {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        val newContext = RuntimeContext(engine, env)

        val contextStack = lookupContextStack()
        contextStack.stack.add(newContext)
        val prevContextStackSize = contextStack.stack.size

        return withCallStackElement(name, pos) {
            try {
                fn(newContext)
            } finally {
                newContext.fireReleaseCallbacks()
                contextStack.stack.size.let { updatedSize ->
                    if (updatedSize != prevContextStackSize) {
                        throw IllegalStateException("Context stack was changed. Prev size = ${prevContextStackSize}, new size = $updatedSize")
                    }
                }
                contextStack.stack.removeLast()
            }
        }
    }

    fun lookupContextStack(): ContextStack {
        val contextStack = threadLocalContextStack.value
        assertx(contextStack != null) { "context stack is null" }
        return contextStack
    }

    fun assignArgs(args: List<EnvironmentBinding>, a: APLValue, pos: Position? = null) {
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

    @OptIn(ExperimentalContracts::class)
    inline fun <T> withCallStackElement(name: String, pos: Position, fn: (CallStackElement) -> T): T {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        return engine.withCallStackElement(name, pos, fn)
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
