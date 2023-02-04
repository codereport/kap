package array.options

class InvalidOptionDefinition(message: String) : Exception(message)
open class OptionException(message: String) : Exception(message)
class InvalidOption(name: String) : OptionException("Invalid arg: ${name}")
class OptionSyntaxException(message: String) : OptionException(message)

class Option(val name: String, val requireArg: Boolean = false, val description: String? = null, val shortOption: String? = null)
class OptionResult(val option: Option, val arg: String?)

private val LONG_OPTION_ARG_PATTERN = "^--([a-zA-Z0-9-]+)=(.*)$".toRegex()
private val LONG_OPTION_NO_ARG_PATTERN = "^--([a-zA-Z0-9-]+)$".toRegex()
private val SHORT_OPTION_PATTERN = "^-([a-zA-Z0-9]+)$".toRegex()

class ArgParser(vararg options: Option) {
    val definedOptions: Map<String, Option>

    init {
        definedOptions = HashMap()
        options.forEach { option ->
            if (definedOptions.containsKey(option.name)) {
                throw InvalidOptionDefinition("Duplicated option name found: ${option.name}")
            }
            val s = option.shortOption
            if (s != null) {
                if (definedOptions.values.find { v -> v.shortOption == s } != null) {
                    throw InvalidOptionDefinition("Duplicated short option found: ${option.name}")
                }
            }
            definedOptions[option.name] = option
        }
    }

    fun parse(args: Array<String>): Map<String, String?> {
        val parseResults = HashMap<String, String?>()
        var i = 0
        while (i < args.size) {
            val arg = args[i++]
            val argResult = LONG_OPTION_ARG_PATTERN.matchEntire(arg)
            if (argResult != null) {
                val option = lookup(arg, argResult.groups.get(1)!!.value, argResult.groups.get(2)!!.value)
                parseResults[option.option.name] = option.arg
            } else {
                val longNoArgResult = LONG_OPTION_NO_ARG_PATTERN.matchEntire(arg)
                if (longNoArgResult != null) {
                    val option = lookup(arg, longNoArgResult.groups.get(1)!!.value, null)
                    parseResults[option.option.name] = option.arg
                } else {
                    val shortResult = SHORT_OPTION_PATTERN.matchEntire(arg)
                    if (shortResult != null) {
                        val shortArgString = shortResult.groups.get(1)!!.value
                        shortArgString.forEach { ch ->
                            val option = lookupSingleChar(ch.toString())
                            if (option.requireArg) {
                                if (i >= args.size) {
                                    throw OptionSyntaxException("Missing argument to \"${option.shortOption}\" parameter")
                                }
                                val parameter = args[i++]
                                parseResults[option.name] = parameter
                            } else {
                                parseResults[option.name] = null
                            }
                        }
                    } else {
                        throw InvalidOption(arg)
                    }
                }
            }
//            val option = if (argResult != null) {
//                lookup(arg, argResult.groups.get(1)!!.value, argResult.groups.get(2)!!.value)
//            } else {
//                val longNoArgResult = LONG_OPTION_NO_ARG_PATTERN.matchEntire(arg)
//                if (longNoArgResult != null) {
//                    lookup(arg, longNoArgResult.groups.get(1)!!.value, null)
//                } else {
//                    throw InvalidOption(arg)
//                }
//            }
//            parseResults[option.option.name] = option.arg
        }
        return parseResults
    }

    fun printHelp() {
        println("Options:")
        definedOptions.keys.sorted().forEach { key ->
            val option = definedOptions[key]!!
            val description = option.description
            val buf = StringBuilder()
            buf.append("  --")
            buf.append(option.name)
            if (description != null) {
                buf.append(" ")
                buf.append(description)
            }
            println(buf.toString())
        }
    }

    private fun lookup(originalArg: String, name: String, arg: String?): OptionResult {
        val option = definedOptions[name] ?: throw InvalidOption(originalArg)
        if ((option.requireArg && arg == null) || (!option.requireArg && arg != null)) {
            throw InvalidOption(originalArg)
        }
        return OptionResult(option, arg)
    }

    private fun lookupSingleChar(s: String): Option {
        definedOptions.values.forEach { option ->
            if (option.shortOption != null && option.shortOption == s) {
                return option
            }
        }
        throw InvalidOption(s)
    }
}
