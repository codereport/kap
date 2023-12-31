package array

import array.complex.Complex
import com.dhsdevelopments.mpbignum.BigInt
import com.dhsdevelopments.mpbignum.compareTo
import com.dhsdevelopments.mpbignum.of
import com.dhsdevelopments.mpbignum.toLong

abstract class Token {
    open fun formatted(): String {
        return this::class.simpleName ?: toString()
    }
}

abstract class ConstantToken : Token() {
    abstract fun parsedValue(): APLValue
}

object Whitespace : Token()
object EndOfFile : Token()
object OpenParen : Token()
object CloseParen : Token()
object OpenFnDef : Token()
object CloseFnDef : Token()
object OpenBracket : Token()
object CloseBracket : Token()
object StatementSeparator : Token()
object LeftArrow : Token()
object FnDefSym : Token()
object FnDefArrow : Token()
object APLNullSym : Token()
object QuotePrefix : Token()
object LambdaToken : Token()
object ApplyToken : Token()
object ListSeparator : Token()
object Newline : Token()
object NamespaceToken : Token()
object ImportToken : Token()
object DefsyntaxSubToken : Token()
object DefsyntaxToken : Token()
object IncludeToken : Token()
object DeclareToken : Token()
object LeftForkToken : Token()
object RightForkToken : Token()
object DynassignToken : Token()
object AndToken : Token()
object OrToken : Token()

class Namespace(val name: String) {
    private val lock = MPLock()
    private val symbols = HashMap<String, NamespaceEntry>()
    private val imports = ArrayList<Namespace>()

    override fun toString() = "Namespace[name=${name}]"

    fun findSymbol(name: String, includePrivate: Boolean = false): Symbol? {
        lock.withLocked {
            val e = symbols[name]
            return when {
                e == null -> null
                includePrivate -> e.symbol
                e.exported -> e.symbol
                else -> null
            }
        }
    }

    fun internSymbol(name: String): Symbol {
        lock.withLocked {
            val e = symbols[name]
            return if (e == null) {
                Symbol(name, this).also { sym -> symbols[name] = NamespaceEntry(sym, false) }
            } else {
                e.symbol
            }
        }
    }

    fun addImport(namespace: Namespace) {
        lock.withLocked {
            if (namespace !== this) {
                imports.add(namespace)
            }
        }
    }

    fun internAndExport(name: String): Symbol {
        lock.withLocked {
            val e = symbols[name]
            val e2 = if (e == null) {
                NamespaceEntry(Symbol(name, this), true).also { symbols[name] = it }
            } else {
                e.exported = true
                e
            }
            return e2.symbol
        }
    }

    /**
     * If [symbol] is interned in this namespace, mark it as exported. Otherwise throw
     * an exception.
     */
    fun exportIfInterned(symbol: Symbol) {
        lock.withLocked {
            val v = symbols[symbol.symbolName]
            if (v == null || v.symbol !== symbol) {
                throw IllegalArgumentException("Symbol is not interned in namespace")
            }
            v.exported = true
        }
    }

    fun findSymbolInImports(name: String): Symbol? {
        lock.withLocked {
            findSymbol(name, true)?.also { sym -> return sym }
            imports.forEach { namespace ->
                namespace.findSymbol(name, false)?.also { sym -> return sym }
            }
            return null
        }
    }

    private class NamespaceEntry(val symbol: Symbol, var exported: Boolean)
}

class Symbol(val symbolName: String, val namespace: Namespace) : Token(), Comparable<Symbol> {
    override fun toString() = "Symbol[name=${nameWithNamespace}]"

    override fun compareTo(other: Symbol): Int {
        return if (namespace.name != other.namespace.name) {
            namespace.name.compareTo(other.namespace.name)
        } else {
            symbolName.compareTo(other.symbolName)
        }
    }

    override fun formatted() = nameWithNamespace

    val nameWithNamespace get() = "${namespace.name}:${symbolName}"
}

class StringToken(val value: String) : ConstantToken() {
    override fun parsedValue() = APLString(value)
    override fun toString() = "StringToken['${value}']"
}

class ParsedLong(val value: Long) : ConstantToken() {
    override fun parsedValue() = value.makeAPLNumber()
    override fun toString() = "ParsedLong[${value}]"
}

class ParsedDouble(val value: Double) : ConstantToken() {
    override fun parsedValue() = value.makeAPLNumber()
    override fun toString() = "ParsedDouble[${value}]"
}

class ParsedComplex(val value: Complex) : ConstantToken() {
    override fun parsedValue() = value.makeAPLNumber()
    override fun toString() = "ParsedComplex[${value}]"
}

class ParsedCharacter(val value: Int) : ConstantToken() {
    override fun parsedValue() = value.makeAPLNumber()
    override fun toString() = "ParsedCharacter[${value}]"
}

class ParsedBigInt(val value: BigInt) : ConstantToken() {
    override fun parsedValue() = value.makeAPLNumber()
    override fun toString() = "ParsedBigInt[${value}]"
}

interface SourceLocation {
    fun sourceText(): String
    fun open(): CharacterProvider
    val name: String? get() = null
}

class StringSourceLocation(private val sourceText: String) : SourceLocation {
    override fun sourceText() = sourceText
    override fun open() = StringCharacterProvider(sourceText)
}

class FileSourceLocation(private val file: String) : SourceLocation {
    override fun sourceText(): String {
        TODO("not implemented")
    }

    override fun open() = openInputCharFile(file)

    override val name get() = file
}

data class Position(
    val source: SourceLocation,
    val line: Int,
    val col: Int,
    val callerName: String? = null,
    val endLine: Int? = null,
    val endCol: Int? = null,
) {
    fun withCallerName(s: String) = copy(callerName = s)
    fun expandToEnd(pos: Position) = copy(endLine = pos.computedEndLine, endCol = pos.computedEndCol)
    val computedEndCol get() = endCol ?: (col + 1)
    val computedEndLine get() = endLine ?: line
}

data class TokenWithPosition(val token: Token, val pos: Position)

class TokenGenerator(val engine: Engine, contentArg: SourceLocation) : NativeCloseable {
    private val content = PushBackCharacterProvider(contentArg)
    private val pushBackList = ArrayList<TokenWithPosition>()

    private val charToTokenMap = hashMapOf(
        "(" to OpenParen,
        ")" to CloseParen,
        "{" to OpenFnDef,
        "}" to CloseFnDef,
        "[" to OpenBracket,
        "]" to CloseBracket,
        "←" to LeftArrow,
        "◊" to StatementSeparator,
        "⋄" to StatementSeparator,
        "∇" to FnDefSym,
        "⇐" to FnDefArrow,
        "⍬" to APLNullSym,
        "λ" to LambdaToken,
        "⍞" to ApplyToken,
        ";" to ListSeparator,
        "«" to LeftForkToken,
        "»" to RightForkToken)

    fun peekToken(): Token {
        val res = nextTokenWithPosition()
        pushBackToken(res)
        return res.token
    }

    inline fun <reified T : Token> nextTokenWithType(): T {
        val (token, pos) = nextTokenWithPosition()
        if (token is T) {
            return token
        } else {
            throw UnexpectedToken(token, pos)
        }
    }

    inline fun <reified T : Token> nextTokenAndPosWithType(): Pair<T, Position> {
        val (token, pos) = nextTokenWithPosition()
        if (token is T) {
            return Pair(token, pos)
        } else {
            throw UnexpectedToken(token, pos)
        }
    }

    private var isClosed = false

    override fun close() {
        if (!isClosed) {
            content.close()
            isClosed = true
        }
    }

    fun nextTokenOrSpace(): TokenWithPosition {
        val posBeforeParse = content.pos()
        fun mkpos(token: Token): TokenWithPosition {
            val newPos = content.pos()
            return TokenWithPosition(token, posBeforeParse.copy(endLine = newPos.line, endCol = newPos.col))
        }

        if (pushBackList.isNotEmpty()) {
            return pushBackList.removeLast()
        }

        assertx(!isClosed) { "tokeniser has been closed" }

        val ch = content.nextCodepoint()
        if (ch == null) {
            close()
            return mkpos(EndOfFile)
        }

        charToTokenMap[charToString(ch)]?.also { return mkpos(it) }

        return mkpos(
            when {
                engine.charIsSingleCharExported(charToString(ch)) -> {
                    val name = charToString(ch)
                    engine.currentNamespace.findSymbolInImports(name) ?: engine.internSymbol(name, engine.currentNamespace)
                }
                isNegationSign(ch) || isDigit(ch) -> {
                    content.pushBack()
                    collectNumber()
                }
                isNewline(ch) -> Newline
                isWhitespace(ch) -> Whitespace
                isCharQuote(ch) -> collectChar(posBeforeParse)
                isSymbolStartChar(ch) -> collectSymbolOrKeyword(ch, posBeforeParse)
                isQuoteChar(ch) -> collectString()
                isCommentChar(ch) -> skipUntilNewline()
                isQuotePrefixChar(ch) -> QuotePrefix
                isBackquote(ch) -> skipNextNewline()
                else -> throw UnexpectedSymbol(ch, posBeforeParse)
            }
        )
    }

    fun pushBackToken(token: TokenWithPosition) {
        pushBackList.add(token)
    }

    private fun skipUntilNewline(): Newline {
        while (true) {
            val ch = content.nextCodepoint()
            if (ch == null || ch == '\n'.code) {
                break
            }
        }
        return Newline
    }

    private fun skipNextNewline(): Whitespace {
        while (true) {
            val (ch, pos) = content.nextCodepointWithPos()
            when {
                ch == null -> throw ParseException("End of file after continuation character", pos)
                isNewline(ch) -> return Whitespace
                !isWhitespace(ch) -> throw ParseException("Non-whitespace characters after continuation character", pos)
            }
        }
    }

    private fun nextCodepointForCharacterOrError(): Int {
        val (ch, pos) = content.nextCodepointWithPos()
        if (ch == null) {
            throw ParseException("Incomplete character in input", pos)
        }
        return ch
    }

    private fun collectChar(pos: Position): ParsedCharacter {
        val ch = nextCodepointForCharacterOrError()
        return if (ch == '\\'.code) {
            processEscapedChar(pos)
        } else {
            ParsedCharacter(ch)
        }
    }

    private fun processEscapedChar(pos: Position): ParsedCharacter {
        val resultChar = when (val ch = nextCodepointForCharacterOrError()) {
            'n'.code -> '\n'.code
            'r'.code -> '\r'.code
            'e'.code -> 27
            '0'.code -> 0
            's'.code -> ' '.code
            't'.code -> '\t'.code
            '\\'.code -> '\\'.code
            'u'.code -> processUnicodeHexCode(pos)
            in ('A'.code)..('Z'.code) -> processUnicodeName(ch, pos)
            else -> throw ParseException("Invalid character specification", pos)
        }
        return ParsedCharacter(resultChar)
    }

    private fun processUnicodeHexCode(pos: Position): Int {
        val buf = StringBuilder()
        while (true) {
            val ch = content.nextCodepoint() ?: break
            if (!isAlphanumeric(ch)) {
                content.pushBack()
                break
            }
            if (!((ch in '0'.code..'9'.code) || (ch in 'a'.code..'z'.code) || (ch in 'A'.code..'z'.code))) {
                throw ParseException("Invalid character in hex code", pos)
            }
            buf.addCodepoint(ch)
        }
        val s = buf.toString()
        if (s.isEmpty()) {
            throw ParseException("Hex code is blank", pos)
        }
        val code = s.toInt(16)
        if (code < 0 || code > 0x10FFFF) {
            throw ParseException("Invalid hex code", pos)
        }
        return code
    }

    private fun processUnicodeName(firstChar: Int, pos: Position): Int {
        val buf = StringBuilder()
        buf.addCodepoint(firstChar)
        while (true) {
            val ch = content.nextCodepoint() ?: break
            if (!isAlphanumeric(ch) && ch != '_'.code && ch != '-'.code) {
                content.pushBack()
                break
            }
            if (!((ch in 'A'.code..'Z'.code) || ch == '_'.code || ch == '-'.code)) {
                throw ParseException("Invalid character in unicode name", pos)
            }
            buf.addCodepoint(ch)
        }
        val s = buf.toString().replace('_', ' ')
        return nameToCodepoint(s) ?: throw ParseException("Invalid codepoint name: '${s}'", pos)
    }

    private fun collectNumber(): Token {
        val buf = StringBuilder()
        val posStart = content.pos()
        loop@ while (true) {
            val posBeforeParse = content.pos()

            fun throwGarbageAfterNumError(): Nothing = throw IllegalNumberFormat("Garbage after number", posBeforeParse)

            val ch = content.nextCodepoint() ?: break
            when {
                !isNumericConstituent(ch) -> {
                    content.pushBack()
                    break@loop
                }
            }
            buf.addCodepoint(ch)
        }

        val s = buf.toString()
        for (parser in NUMBER_PARSERS) {
            val result = parser.process(s)
            if (result != null) {
                return result
            }
        }
        throw IllegalNumberFormat("Content cannot be parsed as a number: '${s}'", posStart)
    }

    private fun collectSymbolOrKeyword(firstChar: Int, posBeforeParse: Position): Token {
        val (nsName, symbolName) = collectSymbol(firstChar, content, posBeforeParse)
        val namespace = if (nsName == null) {
            val keyword = stringToKeywordMap[symbolName]
            if (keyword != null) {
                return keyword
            }
            val sym = engine.currentNamespace.findSymbolInImports(symbolName)
            if (sym != null) {
                return sym
            }
            null
        } else {
            engine.makeNamespace(nsName)
        }
        return engine.internSymbol(symbolName, namespace)
    }

    private fun collectString(): Token {
        val buf = StringBuilder()
        loop@ while (true) {
            val ch = content.nextCodepoint() ?: throw ParseException("End of input in the middle of string", content.pos())
            when (ch) {
                '"'.code -> break@loop
                '\\'.code -> {
                    val next = content.nextCodepoint() ?: throw ParseException("End of input in the middle of string", content.pos())
                    when (next) {
                        'n'.code -> buf.addCodepoint('\n'.code)
                        'r'.code -> buf.addCodepoint('\r'.code)
                        else -> buf.addCodepoint(next)
                    }
                }
                else -> buf.addCodepoint(ch)
            }
        }
        return StringToken(buf.toString())
    }

    fun nextToken(): Token {
        return nextTokenWithPosition().token
    }

    fun nextTokenWithPosition(): TokenWithPosition {
        while (true) {
            val tokenAndPos = nextTokenOrSpace()
            if (tokenAndPos.token != Whitespace) {
                return tokenAndPos
            }
        }
    }

    inline fun iterateUntilToken(endToken: Token, fn: (Token, Position) -> Unit) {
        while (true) {
            val (token, pos) = nextTokenWithPosition()
            if (token == endToken) {
                break
            }
            fn(token, pos)
        }
    }

    private class NumberParser(val pattern: Regex, val fn: (MatchResult) -> Token) {
        fun process(s: String): Token? {
            val result = pattern.matchEntire(s)
            return if (result == null) {
                null
            } else {
                fn(result)
            }
        }
    }

    companion object {
        private fun withNeg(isNegative: Boolean, s: String) = if (isNegative) "-$s" else s

        fun isValidSymbolName(name: String): Boolean {
            return parseStringToSymbol(name) != null
        }

        fun parseStringToSymbol(string: String): Pair<String?, String>? {
            PushBackCharacterProvider(StringSourceLocation(string)).use { content ->
                val (ch, pos) = content.nextCodepointWithPos()
                if (ch == null || !isSymbolStartChar(ch)) {
                    return null
                }
                try {
                    val (nsName, symbolName) = collectSymbol(ch, content, pos)
                    if (content.nextCodepoint() != null) {
                        return null
                    }
                    return Pair(nsName, symbolName)
                } catch (e: ParseException) {
                    return null
                }
            }
        }

        private fun isNegationSign(ch: Int) = ch == '¯'.code
        private fun isQuoteChar(ch: Int) = ch == '"'.code
        private fun isCommentChar(ch: Int) = ch == '⍝'.code
        private fun isSymbolStartChar(ch: Int) = isLetter(ch) || ch == '_'.code || ch == ':'.code || ch == '∆'.code || ch == '⍙'.code
        private fun isSymbolContinuation(ch: Int) = isSymbolStartChar(ch) || isDigit(ch)
        private fun isNumericConstituent(ch: Int) =
            isDigit(ch) || isNegationSign(ch) || ch == '.'.code || ch in ('a'.code)..('z'.code) || ch in (('A'.code)..('Z'.code))

        private fun isCharQuote(ch: Int) = ch == '@'.code
        private fun isQuotePrefixChar(ch: Int) = ch == '\''.code
        private fun isNewline(ch: Int) = ch == '\n'.code
        private fun isBackquote(ch: Int) = ch == '`'.code

        private val stringToKeywordMap = hashMapOf(
            "namespace" to NamespaceToken,
            "import" to ImportToken,
            "defsyntaxsub" to DefsyntaxSubToken,
            "defsyntax" to DefsyntaxToken,
            "use" to IncludeToken,
            "declare" to DeclareToken,
            "dynamicequal" to DynassignToken,
            "or" to OrToken,
            "and" to AndToken)

        private fun collectSymbol(
            firstChar: Int,
            content: PushBackCharacterProvider,
            posBeforeParse: Position
        ): Pair<String?, String> {
            val buf = StringBuilder()
            buf.addCodepoint(firstChar)
            var foundColon = false
            var prevCharIsColon = false
            while (true) {
                val ch = content.nextCodepoint() ?: break
                when {
                    ch == ':'.code -> {
                        if (foundColon) {
                            throw ParseException("Multiple : characters in symbol")
                        }
                        foundColon = true
                        prevCharIsColon = true
                    }
                    prevCharIsColon -> {
                        prevCharIsColon = false
                        if (!isSymbolStartChar(ch)) {
                            content.pushBack()
                            break
                        }
                    }
                    !isSymbolContinuation(ch) -> {
                        content.pushBack()
                        break
                    }
                }
                buf.addCodepoint(ch)
            }
            val name = buf.toString()
            val keywordResult = "^:([^:]+)$".toRegex().matchEntire(name)
            return if (keywordResult != null) {
                Pair("keyword", keywordResult.groups.get(1)!!.value)
            } else {
                val result =
                    "^(?:([^:]+):)?([^:]+)$".toRegex().matchEntire(name) ?: throw ParseException(
                        "Malformed symbol: '${name}'",
                        posBeforeParse)
                val symbolString = result.groups.get(2)!!.value
                val nsName = result.groups.get(1)
                Pair(nsName?.value, symbolString)
            }
        }

        private val NUMBER_PARSERS = listOf(
            NumberParser("^(¯?)([0-9]+\\.[0-9]*)\$".toRegex()) { result ->
                val groups = result.groups
                val sign = groups.get(1)!!.value
                val s = groups.get(2)!!.value
                ParsedDouble(makeDoubleWithExponent(sign, s, null, null))
            },
            NumberParser("^(¯?)([0-9]+(?:\\.[0-9]*)?)[eE](¯?)([0-9]+)\$".toRegex()) { result ->
                val groups = result.groups
                val sign = groups.get(1)!!.value
                val s = groups.get(2)!!.value
                val exponentSign = groups.get(3)!!.value
                val exponent = groups.get(4)!!.value
                ParsedDouble(makeDoubleWithExponent(sign, s, exponentSign, exponent))
            },
            NumberParser("^(¯?)([0-9]+)$".toRegex()) { result ->
                val groups = result.groups
                val sign = groups.get(1)!!
                val s = groups.get(2)!!
                val v = BigInt.of(withNeg(sign.value != "", s.value))
                if (v >= Long.MIN_VALUE && v <= Long.MAX_VALUE) {
                    ParsedLong(v.toLong())
                } else {
                    ParsedBigInt(v)
                }
            },
            NumberParser("^(¯?)0x([0-9a-fA-F]+)$".toRegex()) { result ->
                val groups = result.groups
                val sign = groups.get(1)!!
                val s = groups.get(2)!!
                val v = BigInt.of(withNeg(sign.value != "", s.value), 16)
                if (v >= Long.MIN_VALUE && v <= Long.MAX_VALUE) {
                    ParsedLong(v.toLong())
                } else {
                    ParsedBigInt(v)
                }
            },
            NumberParser("^(¯?)([0-9]+(?:\\.[0-9]*)?)(?:[eE](¯?)([0-9]+))?[jJ](¯?)([0-9]+(?:\\.[0-9]*)?)(?:[eE](¯?)([0-9]+))?$".toRegex()) { result ->
                val groups = result.groups
                val realSign = groups.get(1)!!.value
                val realS = groups.get(2)!!.value
                val realExpSign = groups.get(3)?.value
                val realExpS = groups.get(4)?.value
                val complexSign = groups.get(5)!!.value
                val complexS = groups.get(6)!!.value
                val complexExpSign = groups.get(7)?.value
                val complexExpS = groups.get(8)?.value
                ParsedComplex(
                    Complex(
                        makeDoubleWithExponent(realSign, realS, realExpSign, realExpS),
                        makeDoubleWithExponent(complexSign, complexS, complexExpSign, complexExpS)))
            }
        )

        private fun makeDoubleWithExponent(sign: String, value: String, exponentSign: String?, exponent: String?): Double {
            val valueWithExponent = if (exponentSign != null && exponent != null) "${value}e${withNeg(exponentSign != "", exponent)}" else value
            return withNeg(sign != "", valueWithExponent).toDouble()
        }
    }
}
