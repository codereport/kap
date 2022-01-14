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

    private fun assertByteArrayContent(expected: ByteArray, content: ByteArray, start: Int? = null) {
        val startPos = start ?: 0
        for (i in expected.indices) {
            assertEquals(expected[i], content[startPos + i])
        }
    }
}
