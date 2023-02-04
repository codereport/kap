package array

import array.FunctionCallChain.Chain2
import array.syntax.processCustomSyntax
import array.syntax.processDefsyntax
import array.syntax.processDefsyntaxSub
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed class ParseResultHolder(val lastToken: TokenWithPosition) {
    val pos: Position get() = lastToken.pos

    class InstrParseResult(val instr: Instruction, lastToken: TokenWithPosition) : ParseResultHolder(lastToken)
    class FnParseResult(val fn: APLFunction, lastToken: TokenWithPosition) : ParseResultHolder(lastToken)
    class EmptyParseResult(lastToken: TokenWithPosition) : ParseResultHolder(lastToken)
}

class StackStorageDescriptor(val env: Environment, val index: Int, val comment: String) {
    override fun toString() = "StackStorageDescriptor[comment=${comment}]"
}

class StackStorageRef(val binding: EnvironmentBinding) {
    val name get() = binding.name
    val frameIndex get() = binding.frameIndex
    val storageOffset get() = binding.storage.index
}

class ExternalStorageRef(var frameIndex: Int, var storageOffset: Int)

class EnvironmentBinding(val environment: Environment, val name: Symbol, storage: StackStorageDescriptor) {
    private var storageInt: StackStorageDescriptor

    val storage get() = storageInt

    var frameIndex: Int = -1

    init {
        storageInt = storage
        recomputeStorageIndex()
    }

    fun updateStorage(storage: StackStorageDescriptor) {
        storageInt = storage
        recomputeStorageIndex()
    }

    private fun recomputeStorageIndex() {
        var i = 0
        var curr: Environment? = environment
        while (curr != null) {
            if (curr === storageInt.env) {
                frameIndex = i
                break
            }
            i++
            curr = curr.parent
        }
        if (frameIndex == -1) {
            throw IllegalStateException("storage descriptor not found in parent environments")
        }
    }

    override fun toString(): String {
        return "EnvironmentBinding[environment=${environment}, name=${name}, key=${hashCode().toString(16)}, storage=${storage}]"
    }
}

/**
 * Class describing a lexical scope during parsing.
 */
class Environment(
    /** The name of the environment, only needed for debugging */
    val name: String,
    /** The parent environment, or null if this is the root environment */
    val parent: Environment?,
    /** If true, do not search parent environments when looking up bindings */
    val closed: Boolean = false,
    /** If true, it is possible to return from a frame allocated using this environment */
    val returnTarget: Boolean = false
) {
    /** The level of this environment relative to the root */
    val index: Int = if (parent == null) 0 else parent.index + 1

    /** A list of objects stored in the stack frame associated with this environment */
    val storageList = ArrayList<StackStorageDescriptor>()

    /** A list of objects needs to be copied from ancestor stack frames */
    val externalStorageList = ArrayList<ExternalStorageRef>()

    /** A list of all environments that has this environment as parent */
    val childEnvironments = ArrayList<Environment>()

    /** If true, this environment can escape the dynamic scope of its parent */
    private var canEscape: Boolean = false

    val bindings = ArrayList<EnvironmentBinding>()
    private val localFunctions = HashMap<Symbol, APLFunctionDescriptor>()

    init {
        parent?.let { it.childEnvironments += this }
    }

    /** Returns true if this environment can escape the dynamic scope of its parent */
    fun canEscape() = canEscape

    fun markCanEscape() {
        canEscape = true
    }

    fun findBinding(sym: Symbol) = bindings.find { b -> b.name == sym }

    fun bindLocal(sym: Symbol): EnvironmentBinding {
        if (findBinding(sym) != null) {
            throw IllegalStateException("Symbol ${sym} is already bound in environment")
        }
        val storage = StackStorageDescriptor(this, storageList.size, "local binding: ${sym.nameWithNamespace} in ${name}")
        storageList.add(storage)
        val newBinding = EnvironmentBinding(this, sym, storage)
        bindings.add(newBinding)
        return newBinding
    }

    fun bindRemote(sym: Symbol, binding: EnvironmentBinding): EnvironmentBinding {
        assertx(findBinding(sym) == null) { "Local binding found when creating remote binding: ${binding}" }
        val newBinding = EnvironmentBinding(this, sym, binding.storage)
        bindings.add(newBinding)
        return newBinding
    }

    fun localBindings(): List<EnvironmentBinding> {
        return bindings
    }

    fun registerLocalFunction(name: Symbol, userFn: APLFunctionDescriptor) {
        localFunctions[name] = userFn
    }

    fun findLocalFunction(name: Symbol): APLFunctionDescriptor? {
        return localFunctions[name]
    }

    override fun toString() = "Environment[name=${name}, numBindings=${bindings.size}]"
}


private fun makeResultList(leftArgs: List<Instruction>): Instruction? {
    return when {
        leftArgs.isEmpty() -> null
        leftArgs.size == 1 -> leftArgs[0]
        else -> Literal1DArray.make(leftArgs)
    }
}

class APLParser(val tokeniser: TokenGenerator) {
    private var environments = mutableListOf(tokeniser.engine.rootEnvironment)

    fun currentEnvironment() = environments.last()

    fun pushEnvironment(name: String?, closed: Boolean = false, returnTarget: Boolean = false): Environment {
        val env = Environment(name ?: "<unnamed>", parent = currentEnvironment(), closed = closed, returnTarget = returnTarget)
        environments.add(env)
        return env
    }

    fun popEnvironment(): Environment {
        val env = environments.removeLast()
        assertx(environments.size > 0) { "attempt to pop environment when environment list is empty" }
        return env
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <T> withEnvironment(name: String? = null, closed: Boolean = false, returnTarget: Boolean = false, fn: (Environment) -> T): T {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        val env = pushEnvironment(name, closed = closed, returnTarget = returnTarget)
        try {
            return fn(env)
        } finally {
            popEnvironment()
        }
    }

    private fun findEnvironmentBinding(sym: Symbol): EnvironmentBinding {
        val curr = currentEnvironment()
        // If the symbol is already bound in the current environment, return that binding
        curr.findBinding(sym).let { binding ->
            if (binding != null) {
                return binding
            }
        }
        // If the symbol is bound in a parent environment, bind it in the local environment
        // and return the new binding
        if (!curr.closed) {
            for (env in environments.asReversed().rest()) {
                val binding = env.findBinding(sym)
                if (binding != null) {
                    return curr.bindRemote(sym, binding)
                }
                if (env.closed) {
                    break
                }
            }
        }
        // If there is no existing binding, create a new binding with storage in the current environment
        return curr.bindLocal(sym)
    }

    fun parseValueToplevelWithPosition(endToken: Token): Pair<Instruction, Position> {
        return when (val result = parseExprToplevel(endToken)) {
            is ParseResultHolder.EmptyParseResult -> Pair(EmptyValueMarker(result.pos), result.pos)
            is ParseResultHolder.InstrParseResult -> Pair(result.instr, result.pos)
            is ParseResultHolder.FnParseResult -> throw ParseException("Function expression not allowed", result.pos)
        }
    }

    fun parseValueToplevel(endToken: Token): Instruction {
        return parseValueToplevelWithPosition(endToken).first
    }

    fun parseExprToplevel(endToken: Token): ParseResultHolder {
        val firstExpr = parseList()
        if (firstExpr.lastToken.token == endToken) {
            return firstExpr
        }

        fun throwIfInvalidToken(holder: ParseResultHolder) {
            if (holder.lastToken.token != StatementSeparator && holder.lastToken.token != Newline) {
                throw UnexpectedToken(holder.lastToken.token, holder.pos)
            }
        }

        throwIfInvalidToken(firstExpr)

        val statementList = ArrayList<Instruction>()

        fun addInstr(holder: ParseResultHolder) {
            if (holder is ParseResultHolder.InstrParseResult) {
                statementList.add(holder.instr)
            } else if (holder !is ParseResultHolder.EmptyParseResult) {
                throw IllegalContextForFunction(holder.pos)
            }
        }

        addInstr(firstExpr)

        while (true) {
            val holder = parseList()
            addInstr(holder)
            if (holder.lastToken.token == endToken) {
                return when (statementList.size) {
                    0 -> ParseResultHolder.InstrParseResult(EmptyValueMarker(holder.pos), holder.lastToken)
                    1 -> ParseResultHolder.InstrParseResult(statementList[0], holder.lastToken)
                    else -> ParseResultHolder.InstrParseResult(InstructionList(statementList), holder.lastToken)
                }
            } else {
                throwIfInvalidToken(holder)
            }
        }
    }

    private fun parseList(): ParseResultHolder {
        val firstValue = parseValue()
        if (firstValue.lastToken.token == ListSeparator) {
            if (firstValue is ParseResultHolder.FnParseResult) {
                throw ParseException("Function expressions can't be part of a list", firstValue.pos)
            }

            fun mkInstr(v: ParseResultHolder): Instruction {
                return when (v) {
                    is ParseResultHolder.EmptyParseResult -> EmptyValueMarker(v.pos)
                    is ParseResultHolder.InstrParseResult -> v.instr
                    is ParseResultHolder.FnParseResult -> throw ParseException("Function expressions can't be part of a list", v.pos)
                }
            }

            val statementList = ArrayList<Instruction>()
            statementList.add(mkInstr(firstValue))
            while (true) {
                val holder = parseValue()
                statementList.add(mkInstr(holder))
                if (holder.lastToken.token != ListSeparator) {
                    return ParseResultHolder.InstrParseResult(ParsedAPLList(statementList), holder.lastToken)
                }
            }
        } else {
            return firstValue
        }
    }

    private fun processFn(
        fn: APLFunction,
        leftArgs: List<Instruction>
    ): ParseResultHolder {
        val parsedFn = parseOperator(fn)
        return when (val holder = parseValue()) {
            is ParseResultHolder.EmptyParseResult -> {
                if (leftArgs.isEmpty()) {
                    ParseResultHolder.FnParseResult(parsedFn, holder.lastToken)
                } else {
                    makeLeftBindFunctionParseResult(leftArgs, parsedFn, holder.lastToken)
                }
            }
            is ParseResultHolder.InstrParseResult -> {
                if (leftArgs.isEmpty()) {
                    ParseResultHolder.InstrParseResult(
                        FunctionCall1Arg(parsedFn, holder.instr, holder.instr.pos),
                        holder.lastToken)
                } else {
                    val leftArgsChecked = makeResultList(leftArgs) ?: throw ParseException("Left args is empty", holder.pos)
                    ParseResultHolder.InstrParseResult(
                        FunctionCall2Arg(parsedFn, leftArgsChecked, holder.instr, leftArgsChecked.pos.expandToEnd(holder.instr.pos)),
                        holder.lastToken)
                }
            }
            is ParseResultHolder.FnParseResult -> {
                fun baseFn() = Chain2(parsedFn.instantiation, parsedFn, holder.fn)
                when {
                    leftArgs.isEmpty() -> {
                        ParseResultHolder.FnParseResult(baseFn(), holder.lastToken)
                    }
                    holder.fn is LeftAssignedFunction || holder.fn is MergedLeftArgFunction -> {
                        val left = LeftAssignedFunction(parsedFn, InstructionList(leftArgs), parsedFn.instantiation)
                        val right = holder.fn
                        val mergedFn = MergedLeftArgFunction(left, right)
                        ParseResultHolder.FnParseResult(mergedFn, holder.lastToken)
//                        makeLeftBindFunctionParseResult(
//                            leftArgs,
//                            MergedLeftArgsFunction(parsedFn, holder.fn),
//                            holder.lastToken)
                    }
                    else -> {
                        makeLeftBindFunctionParseResult(leftArgs, baseFn(), holder.lastToken)
                    }
                }
            }
        }
    }

    private fun makeLeftBindFunctionParseResult(
        leftArgs: List<Instruction>,
        baseFn: APLFunction,
        lastToken: TokenWithPosition
    ): ParseResultHolder.FnParseResult {
        val firstArgPos = leftArgs[0].pos
        val resultList = makeResultList(leftArgs) ?: throw IllegalStateException("Result list is null")
        val fn = LeftAssignedFunction(baseFn, resultList, baseFn.instantiation.updatePos { it.copy(line = firstArgPos.line, col = firstArgPos.col) })
        return ParseResultHolder.FnParseResult(fn, lastToken)
    }

    private fun processAssignment(pos: Position, leftArgs: List<Instruction>): ParseResultHolder.InstrParseResult {
        unless(leftArgs.size == 1) {
            throw IncompatibleTypeParseException("Can only assign to a single variable", pos)
        }
        val dest = leftArgs[0]
        val lvalueReader = dest.deriveLvalueReader() ?: throw IncompatibleTypeParseException("Cannot assign to value", pos)
        return when (val holder = parseValue()) {
            is ParseResultHolder.InstrParseResult -> ParseResultHolder.InstrParseResult(
                lvalueReader.makeInstruction(holder.instr, holder.pos), holder.lastToken)
            is ParseResultHolder.FnParseResult -> throw IllegalContextForFunction(holder.pos)
            is ParseResultHolder.EmptyParseResult -> throw ParseException("No right-side value in assignment instruction", pos)
        }
    }

    class RelocalisedFunctionDescriptor(val fn: APLFunction) : APLFunctionDescriptor {
        class RelocalisedFunctionImpl(fn: APLFunction, pos: FunctionInstantiation) : DelegatedAPLFunctionImpl(pos, listOf(fn)) {
            override fun innerImpl() = fns[0]
            override fun copy(fns: List<APLFunction>) = RelocalisedFunctionImpl(fns[0], instantiation)
        }

        override fun make(instantiation: FunctionInstantiation): APLFunction {
            return RelocalisedFunctionImpl(fn, instantiation)
        }
    }

    data class DefinedUserFunction(val fn: APLFunctionDescriptor, val name: Symbol, val pos: Position)

    class UpdateableFunction(var innerFnDescriptor: APLFunctionDescriptor) : APLFunctionDescriptor {
        inner class UpdateableFunctionImpl(pos: FunctionInstantiation) : DelegatedAPLFunctionImpl(pos) {
            private var prevDescriptor: APLFunctionDescriptor = innerFnDescriptor
            private var inner: APLFunction = prevDescriptor.make(pos)

            override fun innerImpl(): APLFunction {
                if (prevDescriptor !== innerFnDescriptor) {
                    inner = innerFnDescriptor.make(instantiation)
                    prevDescriptor = innerFnDescriptor
                }
                return inner
            }
        }

        override fun make(instantiation: FunctionInstantiation): APLFunction {
            return UpdateableFunctionImpl(instantiation)
        }

        fun replaceFunctionDefinition(newFn: APLFunctionDescriptor) {
            innerFnDescriptor = newFn
        }
    }

    private fun processFunctionDefinition(pos: Position, leftArgs: List<Instruction>): Instruction {
        if (leftArgs.isNotEmpty()) {
            throw ParseException("Function definition with non-null left argument", pos)
        }
        parseAndDefineUserDefinedFn(pos)
        return LiteralAPLNullValue(pos)
    }

    private fun processShortFormFn(pos: Position, sym: Symbol): ParseResultHolder {
        val holder = parseValue()
        if (holder !is ParseResultHolder.FnParseResult) {
            throw ParseException("Right side of the arrow must be a function", pos)
        }
        val (closureFn, instructions) = holder.fn.computeClosure(this)
        currentEnvironment().registerLocalFunction(sym, RelocalisedFunctionDescriptor(closureFn))
        val resultInstrs = instructions.asReversed() + LiteralAPLNullValue(pos)
        return ParseResultHolder.InstrParseResult(InstructionList(resultInstrs), holder.lastToken)
    }

    private fun registerDefinedUserFunction(definedUserFunction: DefinedUserFunction) {
        val engine = tokeniser.engine
        when (val oldDefinition = engine.getFunction(definedUserFunction.name)) {
            null -> engine.registerFunction(definedUserFunction.name, UpdateableFunction(definedUserFunction.fn))
            is UpdateableFunction -> oldDefinition.replaceFunctionDefinition(definedUserFunction.fn)
            else -> throw InvalidFunctionRedefinition(definedUserFunction.name, definedUserFunction.pos)
        }
    }

    private fun parseAndDefineUserDefinedFn(pos: Position) {
        class FnArgComponent(val symbols: List<Symbol>, val semicolonSeparated: Boolean)

        fun collectTokenList(list: MutableList<Symbol>) {
            while (true) {
                val (token, innerPos) = tokeniser.nextTokenWithPosition()
                when (token) {
                    is Symbol -> list.add(token)
                    is CloseParen -> return
                    else -> throw UnexpectedToken(token, innerPos)
                }
            }
        }

        fun collectSemicolonSeparatedList(list: MutableList<Symbol>) {
            while (true) {
                list.add(tokeniser.nextTokenWithType())
                val (token, innerPos) = tokeniser.nextTokenWithPosition()
                when (token) {
                    is CloseParen -> return
                    !is ListSeparator -> throw UnexpectedToken(token, innerPos)
                }
            }
        }

        fun parseSymbolList(): FnArgComponent {
            val list = ArrayList<Symbol>()
            list.add(tokeniser.nextTokenWithType())
            val (token, innerPos) = tokeniser.nextTokenWithPosition()
            return when (token) {
                is ListSeparator -> {
                    collectSemicolonSeparatedList(list)
                    FnArgComponent(list, true)
                }
                is Symbol -> {
                    list.add(token)
                    collectTokenList(list)
                    FnArgComponent(list, false)
                }
                is CloseParen -> FnArgComponent(list, false)
                else -> throw UnexpectedToken(token, innerPos)
            }
        }

        fun mkArg(args: FnArgComponent?): List<Symbol> {
            return when {
                args == null -> emptyList()
                args.symbols.size > 1 && !args.semicolonSeparated -> throw ParseException(
                    "Argument list element must be separated by semicolons", pos)
                else -> args.symbols
            }
        }

        fun processFnWithArg(nameComponent: FnArgComponent, leftArgsComponent: FnArgComponent?, rightArgsComponent: FnArgComponent?) {
            val engine = tokeniser.engine

            val name = nameComponent.symbols[0]
            val leftAndRightArgsIsEmpty = leftArgsComponent == null && rightArgsComponent == null
            val (leftArgs, rightArgs) = if (leftAndRightArgsIsEmpty) {
                Pair(listOf(engine.internSymbol("⍺", engine.coreNamespace)), listOf(engine.internSymbol("⍵", engine.coreNamespace)))
            } else {
                Pair(mkArg(leftArgsComponent), mkArg(rightArgsComponent))
            }
            if (rightArgs.isEmpty()) {
                throw ParseException("Right argument list is empty", pos)
            }
            val combined = leftArgs + rightArgs
            val duplicated = combined.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
            if (duplicated.isNotEmpty()) {
                throw ParseException("Symbols in multiple position: ${duplicated.joinToString(separator = " ") { it.symbolName }}", pos)
            }

            val nameSymbols = nameComponent.symbols
            when (nameSymbols.size) {
                1 -> {
                    // Parse like a normal function definition
                    val definedUserFunction = withEnvironment("function0: ${name.nameWithNamespace}", closed = true, returnTarget = true) { env ->
                        val leftFnBindings = leftArgs.map { sym -> env.bindLocal(sym) }
                        val rightFnBindings = rightArgs.map { sym -> env.bindLocal(sym) }
                        val inProcessUserFunction =
                            UserFunction(name, leftFnBindings, rightFnBindings, DummyInstr(pos), env)
                        env.registerLocalFunction(name, inProcessUserFunction)
                        val instr = parseValueToplevel(CloseFnDef)
                        inProcessUserFunction.instr = instr
                        DefinedUserFunction(inProcessUserFunction, name, pos)
                    }
                    registerDefinedUserFunction(definedUserFunction)
                }
                2 -> {
                    if (nameComponent.semicolonSeparated) throw ParseException("Invalid function name", pos)
                    val op = withEnvironment("function1: ${name.nameWithNamespace}", closed = true, returnTarget = true) { env ->
                        val leftFnBindings = leftArgs.map { sym -> env.bindLocal(sym) }
                        val rightFnBindings = rightArgs.map { sym -> env.bindLocal(sym) }
                        val opBinding = env.bindLocal(nameSymbols[0])
                        val instr = parseValueToplevel(CloseFnDef)
                        UserDefinedOperatorOneArg(nameSymbols[1], opBinding, leftFnBindings, rightFnBindings, instr, env)
                    }
                    engine.registerOperator(op.name, op)
                }
                3 -> {
                    if (nameComponent.semicolonSeparated) throw ParseException("Invalid function name", pos)
                    val op = withEnvironment("function2: ${name.nameWithNamespace}", closed = true, returnTarget = true) { env ->
                        val leftFnBindings = leftArgs.map { sym -> env.bindLocal(sym) }
                        val rightFnBindings = rightArgs.map { sym -> env.bindLocal(sym) }
                        val leftOpBinding = env.bindLocal(nameSymbols[0])
                        val rightOpBinding = env.bindLocal(nameSymbols[2])
                        val instr = parseValueToplevel(CloseFnDef)
                        UserDefinedOperatorTwoArg(
                            nameSymbols[1],
                            leftOpBinding,
                            rightOpBinding,
                            leftFnBindings,
                            rightFnBindings,
                            instr,
                            env)
                    }
                    engine.registerOperator(op.name, op)
                }
                else -> throw ParseException("Invalid name specifier", pos)
            }
        }

        val componentList = ArrayList<FnArgComponent>()
        while (true) {
            val (token, innerPos) = tokeniser.nextTokenWithPosition()
            when (token) {
                is OpenFnDef -> break
                is Symbol -> componentList += FnArgComponent(listOf(token), false)
                is OpenParen -> componentList += parseSymbolList()
                else -> throw UnexpectedToken(token, innerPos)
            }
        }

        when {
            componentList.isEmpty() -> throw ParseException("No function name specified", pos)
            componentList.size == 1 -> {
                processFnWithArg(componentList[0], null, null)
            }
            componentList.size == 2 -> {
                processFnWithArg(componentList[0], null, componentList[1])
            }
            componentList.size == 3 -> {
                processFnWithArg(componentList[1], componentList[0], componentList[2])
            }
            else -> throw ParseException("Invalid function definition format", pos)
        }
    }

    fun lookupFunction(name: Symbol): APLFunctionDescriptor? {
        environments.asReversed().forEach { env ->
            val function = env.findLocalFunction(name)
            if (function != null) {
                return function
            }
        }
        return tokeniser.engine.getFunction(name)
    }

    fun parseValue(): ParseResultHolder {
        val leftArgs = ArrayList<Instruction>()

        fun processIndex(pos: Position) {
            if (leftArgs.isEmpty()) {
                throw ParseException("Index referencing without argument", pos)
            }
            val element = leftArgs.removeLast()
            val (index, lastTokenPos) = parseValueToplevelWithPosition(CloseBracket)
            leftArgs.add(ArrayIndex(element, index, pos.copy(endLine = lastTokenPos.computedEndLine, endCol = lastTokenPos.computedEndCol)))
        }

        while (true) {
            val tokenWithPosition = tokeniser.nextTokenWithPosition()
            val (token, pos) = tokenWithPosition
            if (END_EXPR_TOKEN_LIST.contains(tokenWithPosition.token)) {
                val resultList = makeResultList(leftArgs)
                return if (resultList == null) {
                    ParseResultHolder.EmptyParseResult(tokenWithPosition)
                } else {
                    ParseResultHolder.InstrParseResult(resultList, tokenWithPosition)
                }
            }

            when (token) {
                is Symbol -> {
                    val customSyntax = tokeniser.engine.syntaxRulesForSymbol(token)
                    if (customSyntax != null) {
                        leftArgs.add(processCustomSyntax(this, customSyntax))
                    } else {
                        val fnDefTokenWithPosition = tokeniser.nextTokenWithPosition()
                        // If the next  symbol is a function assignment, it needs to be treated specially
                        if (fnDefTokenWithPosition.token is FnDefArrow) {
                            if (leftArgs.size != 0) {
                                throw ParseException("Left side of the arrow must be a single symbol", fnDefTokenWithPosition.pos)
                            }
                            return processShortFormFn(fnDefTokenWithPosition.pos, token)
                        }
                        tokeniser.pushBackToken(fnDefTokenWithPosition)
                        val fn = lookupFunction(token)
                        if (fn != null) {
                            return processFn(
                                fn.make(FunctionInstantiation(pos.withCallerName(token.symbolName), currentEnvironment())),
                                leftArgs)
                        } else {
                            leftArgs.add(makeVariableRef(token, pos))
                        }
                    }
                }
                is OpenParen -> when (val expr = parseExprToplevel(CloseParen)) {
                    is ParseResultHolder.InstrParseResult -> leftArgs.add(expr.instr)
                    is ParseResultHolder.FnParseResult -> return processFn(
                        expr.fn,
                        leftArgs)
                    is ParseResultHolder.EmptyParseResult -> throw ParseException("Empty expression", pos)
                }
                is OpenFnDef -> return processFn(parseFnDefinition(pos).make(FunctionInstantiation(pos, currentEnvironment())), leftArgs)
                is ParsedLong -> leftArgs.add(LiteralInteger(token.value, pos))
                is ParsedDouble -> leftArgs.add(LiteralDouble(token.value, pos))
                is ParsedComplex -> leftArgs.add(LiteralComplex(token.value, pos))
                is ParsedCharacter -> leftArgs.add(LiteralCharacter(token.value, pos))
                is LeftArrow -> return processAssignment(pos, leftArgs)
                is FnDefSym -> leftArgs.add(processFunctionDefinition(pos, leftArgs))
                is APLNullSym -> leftArgs.add(LiteralAPLNullValue(pos))
                is StringToken -> leftArgs.add(LiteralStringValue(token.value, pos))
                is QuotePrefix -> leftArgs.add(LiteralSymbol(tokeniser.nextTokenWithType(), pos))
                is LambdaToken -> leftArgs.add(processLambda(pos))
                is ApplyToken -> return processFn(parseApplyDefinition().make(FunctionInstantiation(pos, currentEnvironment())), leftArgs)
                is NamespaceToken -> processNamespace()
                is ImportToken -> processImport()
                is DefsyntaxSubToken -> processDefsyntaxSub(this, pos)
                is DefsyntaxToken -> leftArgs.add(processDefsyntax(this, pos))
                is IncludeToken -> leftArgs.add(processInclude(pos))
                is DeclareToken -> processDeclare()
                is OpenBracket -> processIndex(pos)
                else -> throw UnexpectedToken(token, pos)
            }
        }
    }

    private fun makeVariableRef(symbol: Symbol, pos: Position): Instruction {
        return if (tokeniser.engine.isSelfEvaluatingSymbol(symbol)) {
            LiteralSymbol(symbol, pos)
        } else {
            VariableRef(symbol, StackStorageRef(findEnvironmentBinding(symbol)), pos)
        }
    }

    private fun processInclude(pos: Position): Instruction {
        val engine = tokeniser.engine
        tokeniser.nextTokenWithType<OpenParen>()
        val filename = tokeniser.nextTokenWithType<StringToken>()
        tokeniser.nextTokenWithType<CloseParen>()
        val resolved = engine.resolveLibraryFile(filename.value) ?: filename.value
        try {
            val innerParser = APLParser(TokenGenerator(engine, FileSourceLocation(resolved)))
            engine.withSavedNamespace {
                return innerParser.parseValueToplevel(EndOfFile)
            }
        } catch (e: MPFileException) {
            throw ParseException("Error loading file: ${resolved}: ${e.message}", pos)
        }
    }

    private fun processNamespace() {
        tokeniser.nextTokenWithType<OpenParen>()
        val namespaceName = tokeniser.nextTokenWithType<StringToken>()
        tokeniser.nextTokenWithType<CloseParen>()
        val namespace = tokeniser.engine.makeNamespace(namespaceName.value)
        tokeniser.engine.currentNamespace = namespace
    }

    private fun processImport() {
        tokeniser.nextTokenWithType<OpenParen>()
        val namespaceName = tokeniser.nextTokenWithType<StringToken>()
        tokeniser.nextTokenWithType<CloseParen>()
        val namespace = tokeniser.engine.makeNamespace(namespaceName.value)
        tokeniser.engine.currentNamespace.addImport(namespace)
    }

    private fun parseSymbolOrSymbolList(fn: (Symbol) -> Unit) {
        val (firstToken, firstTokenPos) = tokeniser.nextTokenWithPosition()
        when (firstToken) {
            is Symbol -> fn(firstToken)
            is OpenParen -> {
                while (true) {
                    val (token, pos) = tokeniser.nextTokenWithPosition()
                    when (token) {
                        is Symbol -> fn(token)
                        is CloseParen -> break
                        else -> throw UnexpectedToken(token, pos)
                    }
                }
            }
            !is CloseParen -> throw UnexpectedToken(firstToken, firstTokenPos)
        }
    }

    private fun processExport() {
        parseSymbolOrSymbolList { sym ->
            exportSymbolIfInterned(sym)
        }
    }


    private fun processLocal() {
        parseSymbolOrSymbolList { sym ->
            currentEnvironment().bindLocal(sym)
        }
    }

    private fun processSingleCharDeclaration(exported: Boolean) {
        val (stringToken, stringPos) = tokeniser.nextTokenAndPosWithType<StringToken>()
        val codepointList = stringToken.value.asCodepointList()
        if (codepointList.size != 1) {
            throw IllegalDeclaration("singleChar declaration argument must be a string of length 1", stringPos)
        }
        tokeniser.registerSingleCharFunction(stringToken.value)
        if (exported) {
            tokeniser.engine.registerExportedSingleCharFunction(stringToken.value)
        }
    }

    private fun processDeclare() {
        val engine = tokeniser.engine
        tokeniser.nextTokenWithType<OpenParen>()
        val (sym, symPosition) = tokeniser.nextTokenAndPosWithType<Symbol>()
        unless(sym.namespace === engine.keywordNamespace) {
            throw IllegalDeclaration("Declaration name must be a keyword", symPosition)
        }
        when (sym.symbolName) {
            "singleChar" -> processSingleCharDeclaration(false)
            "singleCharExported" -> processSingleCharDeclaration(true)
            "export" -> processExport()
            "local" -> processLocal()
            else -> throw IllegalDeclaration("Unknown declaration name: ${sym.nameWithNamespace}")
        }
        tokeniser.nextTokenWithType<CloseParen>()
    }

    private fun exportSymbolIfInterned(symbol: Symbol) {
        symbol.namespace.exportIfInterned(symbol)
    }

    fun parseApplyDefinition(): APLFunctionDescriptor {
        val (token, firstPos) = tokeniser.nextTokenWithPosition()
        val ref = when (token) {
            is Symbol -> makeVariableRef(token, firstPos)
            is OpenParen -> parseValueToplevel(CloseParen)
            else -> throw UnexpectedToken(token, firstPos)
        }
        return DynamicFunctionDescriptor(ref)
    }

    private fun processLambda(pos: Position): EvalLambdaFnx {
        val (token, pos2) = tokeniser.nextTokenWithPosition()
        val fn = when (token) {
            is OpenFnDef -> {
                parseFnDefinition(pos).make(FunctionInstantiation(pos, currentEnvironment()))
            }
            is Symbol -> {
                val fnDefinition = lookupFunction(token) ?: throw ParseException("Symbol is not a valid function", pos)
                fnDefinition.make(FunctionInstantiation(pos, currentEnvironment()))
            }
            is OpenParen -> {
                val holder = parseExprToplevel(CloseParen)
                if (holder !is ParseResultHolder.FnParseResult) {
                    throw ParseException("Argument is not a function", pos)
                }
                holder.fn
            }
            else -> throw UnexpectedToken(token, pos2)
        }
        val (closureFn, relatedInstructions) = fn.computeClosure(this)
        currentEnvironment().markCanEscape()
        return EvalLambdaFnx(closureFn, pos, relatedInstructions)
    }

    fun parseFnDefinition(pos: Position, endToken: Token = CloseFnDef, allocateEnvironment: Boolean = true): APLFunctionDescriptor {
        return if (allocateEnvironment) {
            parseFnDefinitionNewEnvironment(endToken)
        } else {
            parseFnDefinitionSameEnvironment(endToken)
        }
    }

    fun parseFnDefinitionNewEnvironment(endToken: Token = CloseFnDef, name: String = "declared function", returnTarget: Boolean = true): DeclaredFunction {
        val engine = tokeniser.engine
        withEnvironment(name, returnTarget = returnTarget) { env ->
            val leftBinding = env.bindLocal(engine.internSymbol("⍺", engine.coreNamespace))
            val rightBinding = env.bindLocal(engine.internSymbol("⍵", engine.coreNamespace))
            val instruction = parseValueToplevel(endToken)
            return DeclaredFunction("<unnamed>", instruction, leftBinding, rightBinding, env)
        }
    }

    fun parseFnDefinitionSameEnvironment(endToken: Token = CloseFnDef): DeclaredNonBoundFunction {
        val instruction = parseValueToplevel(endToken)
        return DeclaredNonBoundFunction(instruction)
    }

    fun parseOperator(fn: APLFunction): APLFunction {
        var currentFn = fn
        var currToken: TokenWithPosition
        while (true) {
            val axis = parseAxis()
            if (axis != null) {
                currentFn = AxisValAssignedFunctionDirect(currentFn, axis)
            }
            val readToken = tokeniser.nextTokenWithPosition()
            currToken = readToken
            when (val token = currToken.token) {
                is Symbol -> {
                    val op = tokeniser.engine.getOperator(token) ?: break
                    currentFn = op.parseAndCombineFunctions(
                        this,
                        currentFn,
                        FunctionInstantiation(
                            fn.pos.copy(callerName = token.symbolName, endLine = currToken.pos.endLine, endCol = currToken.pos.endCol),
                            currentEnvironment()))
                }
                is LeftForkToken -> {
                    val midExpr = parseExprToplevel(RightForkToken)
                    if (midExpr !is ParseResultHolder.FnParseResult) {
                        throw ParseException("Value in a fork was not a function", currToken.pos)
                    }
                    when (val res = parseFunctionForOperatorRightArg(this)) {
                        is Either.Left -> {
                            currentFn =
                                FunctionCallChain.Chain3(
                                    FunctionInstantiation(currToken.pos.expandToEnd(res.value.second), currentEnvironment()),
                                    currentFn,
                                    midExpr.fn,
                                    res.value.first)
                        }
                        is Either.Right -> {
                            throw ParseException("Right argument is not a function", res.value.second)
                        }
                    }
                }
                else -> break
            }
        }
        tokeniser.pushBackToken(currToken)
        return currentFn
    }

    fun parseAxis(): Instruction? {
        val token = tokeniser.nextTokenWithPosition()
        if (token.token != OpenBracket) {
            tokeniser.pushBackToken(token)
            return null
        }

        return parseValueToplevel(CloseBracket)
    }

    companion object {
        const val OUTER_CALL_SYMBOL = "⍓"

        val END_EXPR_TOKEN_LIST = listOf(
            CloseParen,
            EndOfFile,
            StatementSeparator,
            CloseFnDef,
            CloseBracket,
            ListSeparator,
            Newline,
            RightForkToken)
    }
}
