package array

import com.google.gson.Gson
import com.google.gson.stream.JsonToken
import java.io.BufferedReader
import java.io.FileReader
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmIoTest {
    @Test
    fun parseJson() {
        val gson = Gson()
        BufferedReader(FileReader("/home/elias/prog/array/array/test-data/mastodon-format.json", Charsets.UTF_8)).use { r ->
            val p: CharacterProvider = ReaderCharacterProvider(r)
            val reader = gson.newJsonReader(CharacterProviderReaderWrapper(p))
            reader.isLenient = true
            assertEquals(JsonToken.BEGIN_ARRAY, reader.peek())
            reader.beginArray()
            assertEquals(JsonToken.BEGIN_OBJECT, reader.peek())
            reader.beginObject()
            assertEquals("account", reader.nextName())
            assertEquals(JsonToken.BEGIN_OBJECT, reader.peek())
            reader.beginObject()
            assertEquals("note", reader.nextName())
            assertEquals(JsonToken.STRING, reader.peek())
            val s = reader.nextString()
            println(s)
        }
    }

    @Test
    fun readWithOffset() {
        openInputCharFile("test-data/atoz.txt").use { p ->
            val reader = CharacterProviderReaderWrapper(p)
            val buf = CharArray(16)
            assertEquals(16, reader.read(buf, 0, buf.size))
            assertEquals("abcdefghijklmnop", buf.concatToString())
            assertEquals(3, reader.read(buf, 2, 3))
            assertEquals("abqrsfghijklmnop", buf.concatToString())
        }
    }
}
