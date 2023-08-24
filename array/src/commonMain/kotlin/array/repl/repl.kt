package array.repl

import array.*
import array.options.ArgParser
import array.options.InvalidOption
import array.options.Option

fun runRepl(args: Array<String>, defaultLibPath: String? = null, keyboardInput: KeyboardInput? = null, init: ((Engine) -> Unit)? = null) {
    val argParser = ArgParser(
        Option("help", false, "Print a summary of options"),
        Option("lib-path", true, "Location of the KAP standard library"),
        Option("load", true, "File to load"),
        Option("no-repl", false, "Don't start the REPL"),
        Option("no-standard-lib", false, "Don't load standard-lib"))
    val argResult = try {
        argParser.parse(args)
    } catch (e: InvalidOption) {
        println("Error: ${e.message}")
        argParser.printHelp()
        return
    }
    if (argResult.containsKey("help")) {
        argParser.printHelp()
        return
    }
    try {
        val engine = Engine()
        if (init != null) {
            init(engine)
        }
        engine.standardOutput = object : CharacterOutput {
            override fun writeString(s: String) {
                print(s)
            }
        }
        val libPath = argResult["lib-path"] ?: defaultLibPath
        if (libPath != null) {
            val libPathType = fileType(libPath)
            if (libPathType != null && libPathType === FileNameType.DIRECTORY) {
                engine.addLibrarySearchPath(libPath)
            } else {
                println("Warning: ${libPath} is not a directory, ignoring")
            }
        }
        if (!argResult.containsKey("no-standard-lib")) {
            try {
                engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"))
            } catch (e: APLGenericException) {
                throw ReplFailedException("Error loading standard library", e)
            }
        }
        argResult["load"]?.let { file ->
            val sourceLocation = if (file == "-") {
                StdinSourceLocation(engine)
            } else {
                val loadFileType = fileType(file)
                when (loadFileType) {
                    null -> throw ReplFailedException("File does not exist: ${file}")
                    FileNameType.FILE -> FileSourceLocation(file)
                    else -> throw ReplFailedException("Not a file: ${file}")
                }
            }
            try {
                engine.parseAndEval(sourceLocation).collapse()
            } catch (e: APLGenericException) {
                throw ReplFailedException("Error loading file: ${file}", e)
            }
        }

        if (!argResult.containsKey("no-repl")) {
            val kb = keyboardInput ?: makeKeyboardInput()
            val prompt = "> "
            while (true) {
                val line = kb.readString(prompt) ?: break
                val stringTrimmed = line.trim()
                if (stringTrimmed != "") {
                    try {
                        engine.clearInterrupted()
                        val (_, result) = engine.parseAndEvalWithFormat(StringSourceLocation(line))
                        for (s in result) {
                            println(s)
                        }
                    } catch (e: APLGenericException) {
                        println(e.formattedError())
                    }
                }
            }
        }
    } catch (e: ReplFailedException) {
        println(e.formatted())
    }
}

class ReplFailedException(message: String, cause: Exception? = null) : Exception(message, cause) {
    fun formatted(): String {
        val buf = StringBuilder()
        buf.append(message)
        cause?.let { c ->
            buf.append(":\n")
            if (c is APLGenericException) {
                buf.append(c.formattedError())
            } else {
                buf.append(c.message)
            }
        }
        return buf.toString()
    }
}

class StdinSourceLocation(val engine: Engine) : SourceLocation {
    override fun sourceText(): String {
        TODO("not implemented")
    }

    override fun open(): CharacterProvider {
        return engine.standardInput
    }
}
