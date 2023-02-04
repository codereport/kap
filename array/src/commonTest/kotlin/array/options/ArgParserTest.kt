package array.options

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ArgParserTest {
    @Test
    fun simpleTest() {
        val parser = ArgParser(Option("foo", true), Option("bar", false))
        val result = parser.parse(arrayOf("--foo=a", "--bar"))
        assertEquals(2, result.size)
        assertEquals("a", result["foo"])
        assertTrue(result.containsKey("bar"))
        assertEquals(null, result["bar"])
    }

    @Test
    fun noArgOptionWithArgShouldFail() {
        assertFailsWith<InvalidOption> {
            ArgParser(Option("foo", false)).parse(arrayOf("--foo=a"))
        }
    }

    @Test
    fun argOptionWithoutArgShouldFail() {
        assertFailsWith<InvalidOption> {
            ArgParser(Option("foo", true)).parse(arrayOf("--foo"))
        }
    }

    @Test
    fun noArguments() {
        val result = ArgParser(Option("foo", true), Option("bar", false)).parse(emptyArray())
        assertEquals(0, result.size)
    }

    @Test
    fun invalidOptionFormat0() {
        assertFailsWith<InvalidOption> {
            ArgParser(Option("foo", true), Option("bar", false)).parse(arrayOf("-foo=a"))
        }
    }

    @Test
    fun invalidOptionFormat1() {
        assertFailsWith<InvalidOption> {
            ArgParser(Option("foo", true), Option("bar", false)).parse(arrayOf("-bar"))
        }
    }

    @Test
    fun invalidOptionFormat2() {
        assertFailsWith<InvalidOption> {
            ArgParser(Option("foo", true), Option("bar", false)).parse(arrayOf(" --foo=a"))
        }
    }

    @Test
    fun shortOptionOneArg() {
        val result = ArgParser(Option("foo", shortOption = "f")).parse(arrayOf("-f"))
        assertEquals(1, result.size)
        assertTrue(result.containsKey("foo"))
        assertEquals(null, result["foo"])
    }

    @Test
    fun shortOptionTwoArg() {
        val result = ArgParser(Option("foo", shortOption = "f"), Option("xyz", shortOption = "x")).parse(arrayOf("-fx"))
        assertEquals(2, result.size)
        assertTrue(result.containsKey("foo"))
        assertEquals(null, result["foo"])
        assertTrue(result.containsKey("xyz"))
        assertEquals(null, result["xyz"])
    }

    @Test
    fun shortOptionTwoArgMultiEntry() {
        val result = ArgParser(Option("foo", shortOption = "f"), Option("xyz", shortOption = "x")).parse(arrayOf("-f", "-x"))
        assertEquals(2, result.size)
        assertTrue(result.containsKey("foo"))
        assertEquals(null, result["foo"])
        assertTrue(result.containsKey("xyz"))
        assertEquals(null, result["xyz"])
    }

    @Test
    fun shortOptionTwoArgMixLong() {
        val result = ArgParser(Option("foo", shortOption = "f"), Option("xyz", shortOption = "x")).parse(arrayOf("-f", "--xyz"))
        assertEquals(2, result.size)
        assertTrue(result.containsKey("foo"))
        assertEquals(null, result["foo"])
        assertTrue(result.containsKey("xyz"))
        assertEquals(null, result["xyz"])
    }

    @Test
    fun shortOptionTwoArgMixLongWithArg() {
        val result = ArgParser(
            Option("foo", shortOption = "f"),
            Option("xyz", requireArg = true, shortOption = "x"))
            .parse(arrayOf("-f", "--xyz=a"))
        assertEquals(2, result.size)
        assertTrue(result.containsKey("foo"))
        assertEquals(null, result["foo"])
        assertTrue(result.containsKey("xyz"))
        assertEquals("a", result["xyz"])
    }

    @Test
    fun shortOptionArgWithParam() {
        val result = ArgParser(
            Option("foo", shortOption = "f"),
            Option("xyz", requireArg = true, shortOption = "x"))
            .parse(arrayOf("-x", "a"))
        assertEquals(1, result.size)
        assertTrue(result.containsKey("xyz"))
        assertEquals("a", result["xyz"])
    }

    @Test
    fun invalidShortArg() {
        assertFailsWith<InvalidOption> {
            ArgParser(Option("foo", shortOption = "f")).parse(arrayOf("-x"))
        }
    }

    @Test
    fun duplicateArgNames0() {
        assertFailsWith<InvalidOptionDefinition> {
            ArgParser(Option("foo", shortOption = "z"), Option("bar", shortOption = "z"))
        }
    }

    @Test
    fun duplicateArgNames1() {
        assertFailsWith<InvalidOptionDefinition> {
            ArgParser(Option("foo", shortOption = "a"), Option("foo", shortOption = "b"))
        }
    }

    @Test
    fun multipleCopiesOfSameArg0() {
        val result = ArgParser(Option("foo", shortOption = "f"), Option("xyz", shortOption = "x")).parse(arrayOf("-f", "-f"))
        assertEquals(1, result.size)
        assertTrue(result.containsKey("foo"))
        assertEquals(null, result["foo"])
    }

    @Test
    fun multipleCopiesOfSameArg1() {
        val result = ArgParser(Option("foo", shortOption = "f"), Option("xyz", shortOption = "x")).parse(arrayOf("--foo", "--foo"))
        assertEquals(1, result.size)
        assertTrue(result.containsKey("foo"))
        assertEquals(null, result["foo"])
    }

    @Test
    fun multipleCopiesOfSameArg2() {
        val result = ArgParser(
            Option("foo", requireArg = true, shortOption = "f"),
            Option("xyz", shortOption = "x"))
            .parse(arrayOf("--foo=a", "--foo=b"))
        assertEquals(1, result.size)
        assertEquals("b", result["foo"])
    }

    @Test
    fun multipleCopiesOfSameArg3() {
        val result =
            ArgParser(Option("foo", requireArg = true, shortOption = "f"), Option("xyz", shortOption = "x"))
                .parse(arrayOf("-f", "a", "--foo=b"))
        assertEquals(1, result.size)
        assertEquals("b", result["foo"])
    }
}
