package array

import kotlin.test.*

class IoTest {
    @Test
    fun testBinaryFile() {
        openInputFile("test-data/plain.txt").use { input ->
            val buf = ByteArray(3)
            val result = input.readBlock(buf)
            assertEquals(3, result)
            assertByteArrayContent("abc".encodeToByteArray(), buf)
        }
    }

    @Test
    fun testPartialBlock() {
        openInputFile("test-data/plain.txt").use { input ->
            val buf = ByteArray(10) { 0 }
            val result = input.readBlock(buf, 2, 3)
            assertEquals(3, result)
            assertByteArrayContent("abc".encodeToByteArray(), buf, 2)
        }
    }

    @Test
    fun testMultipleReads() {
        openInputFile("test-data/plain.txt").use { input ->
            val buf = ByteArray(5) { 0 }
            val result1 = input.readBlock(buf, 2, 3)
            assertEquals(3, result1)
            assertByteArrayContent("abc".encodeToByteArray(), buf, 2)
            val result2 = input.readBlock(buf, 0, 3)
            assertEquals(3, result2)
            assertByteArrayContent("bar".encodeToByteArray(), buf)
        }
    }

    @Test
    fun testCharacterContent() {
        openInputCharFile("test-data/char-tests.txt").use { input ->
            assertEquals(0x61, input.nextCodepoint())
            assertEquals(0x62, input.nextCodepoint())
            assertEquals(0x2283, input.nextCodepoint())
            assertEquals(0x22C6, input.nextCodepoint())
            assertEquals(0x1D49F, input.nextCodepoint())
            assertEquals(0xE01, input.nextCodepoint())
            assertEquals(0xA, input.nextCodepoint())
            assertNull(input.nextCodepoint())
        }
    }

    @Test
    fun testReadline() {
        openInputCharFile("test-data/plain.txt").use { input ->
            assertEquals("abcbar", input.nextLine())
            assertNull(input.nextCodepoint())
        }
    }

    @Test
    fun characterProviderLines() {
        openInputCharFile("test-data/multi.txt").use { input ->
            val expected = listOf("foo", "bar", "test", "abcdef", "testtest", "  testline", "", "aa", "ab", "ac", "ad")
            val res = ArrayList<String>()
            input.lines().forEach { s ->
                res.add(s)
            }
            assertEquals(expected, res)
        }
    }

    @Test
    fun fileNotFoundError() {
        assertFailsWith<MPFileException> {
            openInputCharFile("test-data/this-file-should-not-be-found")

        }
    }

    @Test
    fun stringBuilderOutput() {
        val out = StringBuilderOutput()
        out.writeString("abc")
        out.writeString("efg")
        assertEquals("abcefg", out.buf.toString())
    }

    @Test
    fun resolveDirectoryPathAbsolute() {
        assertEquals("/foo/bar", resolveDirectoryPath("/foo/bar", "/xyz"))
    }

    @Test
    fun resolveDirectoryPathRelative() {
        assertEquals("/xyz/foo/bar", resolveDirectoryPath("foo/bar", "/xyz"))
    }

    @Test
    fun resolveDirectoryPathBlankNameNotAllowed() {
        assertFails {
            resolveDirectoryPath("", "/foo/bar")
        }
    }

    @Test
    fun resolveDirectoryPathWithNull() {
        assertEquals("foo/bar", resolveDirectoryPath("foo/bar", null))
        assertEquals("/foo/bar", resolveDirectoryPath("/foo/bar", null))
    }

    @Test
    fun stringCharacterProvider() {
        val prov = StringCharacterProvider("fooabc")
        assertEquals('f'.code, prov.nextCodepoint())
        assertEquals('o'.code, prov.nextCodepoint())
        assertEquals('o'.code, prov.nextCodepoint())
        assertEquals('a'.code, prov.nextCodepoint())
        assertEquals('b'.code, prov.nextCodepoint())
        assertEquals('c'.code, prov.nextCodepoint())
        assertNull(prov.nextCodepoint())
    }

    @Test
    fun astralPlaneStringCharProv() {
        openInputCharFile("test-data/char-tests.txt").use { input ->
            val s = input.nextLine()
            assertNotNull(s)
            val prov = StringCharacterProvider(s)
            assertEquals(0x61, prov.nextCodepoint())
            assertEquals(0x62, prov.nextCodepoint())
            assertEquals(0x2283, prov.nextCodepoint())
            assertEquals(0x22C6, prov.nextCodepoint())
            assertEquals(0x1D49F, prov.nextCodepoint())
            assertEquals(0xE01, prov.nextCodepoint())
            assertNull(prov.nextCodepoint())
        }
    }

    @Test
    fun readMastodonContent() {
        openInputCharFile("test-data/mastodon-format.json").use { p ->
            val res = ArrayList<Int>()
            while (true) {
                val cp = p.nextCodepoint() ?: break
                assertTrue(cp in 1..127)
                res.add(cp)
            }
            assertEquals(3008, res.size)
            val buf = StringBuilder()
            res.forEach { code ->
                buf.append(code.toChar())
            }
            val expected = """
            |[
            |  {
            |    "account": {
            |      "note": "\u003cp\u003eLisp, Emacs, APL and a bunch of other stuff.\u003c/p\u003e\u003cp\u003eFrom Sweden, living in Singapore.\u003c/p\u003e\u003cp\u003eI always work on a bunch of projects. My current major ones are:\u003c/p\u003e\u003cp\u003eA graphical frontend to Maxima: \u003ca href=\"https://github.com/lokedhs/maxima-client\" rel=\"nofollow noopener noreferrer\" target=\"_blank\"\u003e\u003cspan class=\"invisible\"\u003ehttps://\u003c/span\u003e\u003cspan class=\"ellipsis\"\u003egithub.com/lokedhs/maxima-clie\u003c/span\u003e\u003cspan class=\"invisible\"\u003ent\u003c/span\u003e\u003c/a\u003e\u003c/p\u003e\u003cp\u003eKAP: An APL-based programming language: \u003ca href=\"https://codeberg.org/loke/array\" rel=\"nofollow noopener noreferrer\" target=\"_blank\"\u003e\u003cspan class=\"invisible\"\u003ehttps://\u003c/span\u003e\u003cspan class=\"\"\u003ecodeberg.org/loke/array\u003c/span\u003e\u003cspan class=\"invisible\"\u003e\u003c/span\u003e\u003c/a\u003e\u003c/p\u003e\u003cp\u003e\u003ca href=\"https://functional.cafe/tags/lisp\" class=\"mention hashtag\" rel=\"nofollow noopener noreferrer\" target=\"_blank\"\u003e#\u003cspan\u003elisp\u003c/span\u003e\u003c/a\u003e \u003ca href=\"https://functional.cafe/tags/commonlisp\" class=\"mention hashtag\" rel=\"nofollow noopener noreferrer\" target=\"_blank\"\u003e#\u003cspan\u003ecommonlisp\u003c/span\u003e\u003c/a\u003e \u003ca href=\"https://functional.cafe/tags/apl\" class=\"mention hashtag\" rel=\"nofollow noopener noreferrer\" target=\"_blank\"\u003e#\u003cspan\u003eapl\u003c/span\u003e\u003c/a\u003e \u003ca href=\"https://functional.cafe/tags/retrocomputing\" class=\"mention hashtag\" rel=\"nofollow noopener noreferrer\" target=\"_blank\"\u003e#\u003cspan\u003eretrocomputing\u003c/span\u003e\u003c/a\u003e \u003ca href=\"https://functional.cafe/tags/linux\" class=\"mention hashtag\" rel=\"nofollow noopener noreferrer\" target=\"_blank\"\u003e#\u003cspan\u003elinux\u003c/span\u003e\u003c/a\u003e \u003ca href=\"https://functional.cafe/tags/kap\" class=\"mention hashtag\" rel=\"nofollow noopener noreferrer\" target=\"_blank\"\u003e#\u003cspan\u003ekap\u003c/span\u003e\u003c/a\u003e \u003ca href=\"https://functional.cafe/tags/climaxima\" class=\"mention hashtag\" rel=\"nofollow noopener noreferrer\" target=\"_blank\"\u003e#\u003cspan\u003eclimaxima\u003c/span\u003e\u003c/a\u003e \u003ca href=\"https://functional.cafe/tags/emacs\" class=\"mention hashtag\" rel=\"nofollow noopener noreferrer\" target=\"_blank\"\u003e#\u003cspan\u003eemacs\u003c/span\u003e\u003c/a\u003e \u003ca href=\"https://functional.cafe/tags/atari\" class=\"mention hashtag\" rel=\"nofollow noopener noreferrer\" target=\"_blank\"\u003e#\u003cspan\u003eatari\u003c/span\u003e\u003c/a\u003e \u003ca href=\"https://functional.cafe/tags/fedi22\" class=\"mention hashtag\" rel=\"nofollow noopener noreferrer\" target=\"_blank\"\u003e#\u003cspan\u003efedi22\u003c/span\u003e\u003c/a\u003e\u003c/p\u003e"
            |    }
            |  }
            |]
            |
        """.trimMargin()
            assertEquals(expected, buf.toString())
        }
    }

    private fun assertByteArrayContent(expected: ByteArray, content: ByteArray, start: Int? = null) {
        val startPos = start ?: 0
        for (i in expected.indices) {
            assertEquals(expected[i], content[startPos + i])
        }
    }
}
