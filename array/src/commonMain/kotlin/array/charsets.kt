package array

expect fun isLetter(codepoint: Int): Boolean
expect fun isDigit(codepoint: Int): Boolean
expect fun isWhitespace(codepoint: Int): Boolean
expect fun charToString(codepoint: Int): String
expect fun nameToCodepoint(name: String): Int?
expect fun codepointToName(codepoint: Int): String?

/**
 * If false, the functions [nameToCodepoint] and [codepointToName] will always return null.
 */
expect val backendSupportsUnicodeNames: Boolean

fun isAlphanumeric(codepoint: Int) = isLetter(codepoint) || isDigit(codepoint)

expect fun StringBuilder.addCodepoint(codepoint: Int): StringBuilder
expect fun String.asCodepointList(): List<Int>
expect fun String.asGraphemeList(): List<String>
