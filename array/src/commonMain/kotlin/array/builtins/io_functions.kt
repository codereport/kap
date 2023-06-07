package array.builtins

import array.*
import array.csv.CsvParseException
import array.csv.readCsv
import array.csv.writeAPLArrayAsCsv

class ReadFunction : APLFunctionDescriptor {
    class ReadFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val file = a.toStringValue(pos)
            val result = ArrayList<APLValue>()
            try {
                openInputCharFile(context.engine.resolvePathName(file)).use { provider ->
                    provider.lines().forEach { s ->
                        result.add(APLString(s))
                    }
                }
                return APLArrayList(dimensionsOfSize(result.size), result)
            } catch (e: MPFileNotFoundException) {
                throwAPLException(
                    TagCatch(
                        APLSymbol(context.engine.internSymbol("fileNotFound", context.engine.keywordNamespace)),
                        APLString(file),
                        "File not found: ${file}",
                        pos))
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ReadFunctionImpl(instantiation)
}

class PrintAPLFunction : APLFunctionDescriptor {
    class PrintAPLFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            printValue(context, a, FormatStyle.PLAIN)
            return a
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val engine = context.engine
            val plainSym = engine.internSymbol("plain", engine.keywordNamespace)
            val prettySym = engine.internSymbol("pretty", engine.keywordNamespace)
            val readSym = engine.internSymbol("read", engine.keywordNamespace)

            val style = when (val styleName = a.ensureSymbol().value) {
                plainSym -> FormatStyle.PLAIN
                prettySym -> FormatStyle.PRETTY
                readSym -> FormatStyle.READABLE
                else -> throwAPLException(
                    APLIllegalArgumentException(
                        "Invalid print style: ${styleName.symbolName}",
                        pos))
            }
            printValue(context, b, style)
            return b
        }

        private fun printValue(context: RuntimeContext, a: APLValue, style: FormatStyle) {
            context.engine.standardOutput.writeString(a.formatted(style))
        }
    }

    override fun make(instantiation: FunctionInstantiation) = PrintAPLFunctionImpl(instantiation)
}

class WriteCsvFunction : APLFunctionDescriptor {
    class WriteCsvFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val fileName = a.toStringValue(pos)
            openOutputCharFile(fileName).use { dest ->
                writeAPLArrayAsCsv(dest, b, pos)
            }
            return b
        }
    }

    override fun make(instantiation: FunctionInstantiation) = WriteCsvFunctionImpl(instantiation)
}

class ReadCsvFunction : APLFunctionDescriptor {
    class ReadCsvFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            openInputCharFile(a.toStringValue(pos)).use { source ->
                try {
                    return readCsv(source)
                } catch (e: CsvParseException) {
                    throwAPLException(APLEvalException("Error while pasing CSV: ${e.message}", pos))
                }
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ReadCsvFunctionImpl(instantiation)
}

class ReadFileFunction : APLFunctionDescriptor {
    class ReadFileFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            openInputCharFile(a.toStringValue(pos)).use { source ->
                val buf = StringBuilder()
                while (true) {
                    val ch = source.nextCodepoint() ?: break
                    buf.addCodepoint(ch)
                }
                return APLString(buf.toString())
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ReadFileFunctionImpl(instantiation)
}


class LoadFunction : APLFunctionDescriptor {
    class LoadFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val requestedFile = a.toStringValue(pos)
            val file = context.engine.resolveLibraryFile(requestedFile) ?: requestedFile
            val engine = context.engine
            return engine.withSavedNamespace {
                withThreadLocalsUnassigned {
                    engine.parseAndEval(FileSourceLocation(file))
                }
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = LoadFunctionImpl(instantiation)
}

class HttpRequestFunction : APLFunctionDescriptor {
    class HttpRequestFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val url = a.toStringValue(pos)
            val result = httpRequest(url)
            return APLString.make(result.content)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = HttpRequestFunctionImpl(instantiation)
}

class HttpPostFunction : APLFunctionDescriptor {
    class HttpPostFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val args = a.listify()
            val url = args.listElement(0, pos).toStringValue(pos)
            val (data, headers) = when (args.listSize()) {
                1 -> Pair(APLString.make(""), emptyMap())
                2 -> Pair(args.listElement(1, pos), emptyMap())
                3 -> Pair(args.listElement(1, pos), ensureHeaderArray(args.listElement(2), pos))
                else -> throwAPLException(
                    APLIllegalArgumentException(
                        "Function requires 1-3 arguments, ${args.listSize()} arguments were passed.",
                        pos))
            }
            val result = httpPost(url, data.asByteArray(pos), headers)
            return APLString.make(result.content)
        }

        private fun ensureHeaderArray(headerArg: APLValue, pos: Position): Map<String, String> {
            if (headerArg.rank != 2 || headerArg.dimensions[1] != 2) {
                throw APLIllegalArgumentException("Headers list should be a rank-2 array with 2 columns")
            }
            val result = HashMap<String, String>()
            for (i in 0 until headerArg.dimensions[0]) {
                val key = headerArg.valueAt(i * 2).toStringValue(pos)
                val value = headerArg.valueAt(i * 2 + 1).toStringValue(pos)
                result[key] = value
            }
            return result
        }
    }

    override fun make(instantiation: FunctionInstantiation) = HttpPostFunctionImpl(instantiation)
}

class ReaddirFunction : APLFunctionDescriptor {
    class ReaddirFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return loadContent(context, a, emptyList())
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return loadContent(context, b, parseOutputTypes(context, a))
        }

        private fun loadContent(context: RuntimeContext, file: APLValue, selectors: List<OutputType>): APLValue {
            val content = readDirectoryContent(file.toStringValue(pos))
            val numCols = 1 + selectors.size
            val d = dimensionsOfSize(content.size, numCols)
            val valueList = Array(d.contentSize()) { i ->
                val row = i / numCols
                val col = i % numCols
                val pathEntry = content[row]
                if (col == 0) {
                    APLString.make(pathEntry.name)
                } else {
                    when (selectors[col - 1]) {
                        OutputType.SIZE -> pathEntry.size.makeAPLNumber()
                        OutputType.TYPE -> pathEntryTypeToAPL(context, pathEntry.type)
                    }
                }
            }
            return APLArrayImpl(d, valueList)
        }

        private fun pathEntryTypeToAPL(context: RuntimeContext, type: FileNameType): APLValue {
            val sym = when (type) {
                FileNameType.FILE -> context.engine.internSymbol("file", context.engine.keywordNamespace)
                FileNameType.DIRECTORY -> context.engine.internSymbol("directory", context.engine.keywordNamespace)
                FileNameType.UNDEFINED -> context.engine.internSymbol("undefined", context.engine.keywordNamespace)
            }
            return APLSymbol(sym)
        }

        private fun parseOutputTypes(context: RuntimeContext, value: APLValue): List<OutputType> {
            val keywordToType =
                OutputType.values().associateBy { outputType ->
                    context.engine.internSymbol(
                        outputType.selector,
                        context.engine.keywordNamespace)
                }

            val result = ArrayList<OutputType>()
            val asArray = value.arrayify()
            if (asArray.dimensions.size != 1) {
                throwAPLException(InvalidDimensionsException("Selector must be a scalar or a rank-1 array", pos))
            }
            asArray.iterateMembers { v ->
                val collapsed = v.collapse()
                if (collapsed !is APLSymbol) {
                    throwAPLException(APLIllegalArgumentException("Selector must be a symbol", pos))
                }
                val found =
                    keywordToType[collapsed.value]
                        ?: throwAPLException(
                            APLIllegalArgumentException(
                                "Illegal selector: ${collapsed.value.nameWithNamespace}",
                                pos))
                result.add(found)
            }
            return result
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ReaddirFunctionImpl(instantiation)

    private enum class OutputType(val selector: String) {
        SIZE("size"),
        TYPE("type")
    }
}
