package array

import kotlin.math.max

actual class StringCharacterProvider actual constructor(private val s: String) : CharacterProvider {
    private var pos = 0

    override fun sourceName(): String? = null
    override fun nextCodepoint(): Int? {
        if (pos >= s.length) {
            return null
        }

        val ch = s[pos++]
        return when {
            ch.isHighSurrogate() -> {
                if (pos < s.length) {
                    val low = s[pos++]
                    if (low.isLowSurrogate()) {
                        makeCharFromSurrogatePair(ch, low)
                    } else {
                        throw IllegalStateException("A high surrogate should be followed by a low surrogate")
                    }
                } else {
                    throw IllegalStateException("End of string when low surrogate was expected")
                }
            }
            ch.isLowSurrogate() -> throw IllegalStateException("Unexpected low surrogate")
            else -> ch.code
        }
    }

    override fun close() {}
}

class ByteArrayByteProvider(val content: ByteArray, val name: String? = null) : ByteProvider {
    override fun sourceName() = name

    var pos = 0

    override fun readByte(): Byte? {
        return if (pos >= content.size) null else content[pos++]
    }

    override fun close() {
    }
}

class FileEntryDataByteConsumer(private val entry: FileEntryData) : ByteConsumer {
    override fun writeByte(value: Byte) {
        entry.append(value)
    }

    override fun writeBlock(buffer: ByteArray) {
        buffer.forEach { value -> entry.append(value) }
    }

    override fun close() {
    }
}

actual fun makeKeyboardInput(): KeyboardInput {
    TODO("Not implemented")
}

class FileEntryData(initialData: ByteArray? = null) {
    private var data: ByteArray
    private var sizeInt: Int

    val size get() = sizeInt

    init {
        if (initialData == null) {
            data = ByteArray(BLOCK_SIZE) { 0.toByte() }
            sizeInt = 0
        } else {
            data = initialData.copyOf()
            sizeInt = initialData.size
        }
    }

    operator fun get(index: Int): Byte {
        checkIndex(index)
        return data[index]
    }

    operator fun set(index: Int, value: Byte) {
        checkIndex(index)
        data[index] = value
    }

    fun append(value: Byte) {
        if (sizeInt >= data.size) {
            val newSize = max(data.size * 2, BLOCK_SIZE)
            val newArray = data.copyOf(newSize)
            data = newArray
            sizeInt = newSize
        }
        data[sizeInt++] = value
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= sizeInt) {
            throw IllegalArgumentException("Attempt to read byte outside of array")
        }
    }

    fun toByteArray() = data.copyOf(sizeInt)

    companion object {
        const val BLOCK_SIZE = 1024
    }
}

sealed class RegisteredEntry(val name: String) {
    class File(name: String, val content: FileEntryData) : RegisteredEntry(name)

    class Directory(name: String) : RegisteredEntry(name) {
        val files = HashMap<String, RegisteredEntry>()

        fun find(path: String): RegisteredEntry? {
            val parts = splitName(path)
            return findPathElement(parts, false)
        }

        fun registerFile(path: String, content: ByteArray): FileEntryData {
            val parts = splitName(path)
            val dir = findPathElement(parts.subList(0, parts.size - 1), createDirs = true, lastElementIsDir = true)
            if (dir !is Directory) {
                throw MPFileException("Parent path does not represent a directory")
            }
            val namepart = parts.last()
            val data = FileEntryData(content)
            dir.files[namepart] = File(namepart, data)
            return data
        }

        private fun splitName(path: String) = path.split("/").filter { s -> s.isNotEmpty() }

        private fun findPathElement(parts: List<String>, createDirs: Boolean = false, lastElementIsDir: Boolean = false): RegisteredEntry? {
            if (parts.isEmpty()) {
                return this
            }
            var curr = this
            for (i in 0 until parts.size - 1) {
                val s = parts[i]
                var p = curr.files[s]
                if (p == null) {
                    if (createDirs) {
                        p = Directory(s)
                        curr.files[s] = p
                    } else {
                        return null
                    }
                } else if (p !is Directory) {
                    return null
                }
                curr = p
            }
            val s = parts.last()
            val f = curr.files[s]
            return when {
                f != null -> f
                createDirs && lastElementIsDir -> curr.createDirectory(s)
                else -> null
            }
        }

        fun createDirectory(name: String, errorIfExists: Boolean = true): Directory {
            if (errorIfExists && files.containsKey(name)) {
                throw MPFileException("Directory already exists: ${name}")
            }
            val dir = Directory(name)
            files[name] = dir
            return dir
        }
    }
}

val registeredFilesRoot = RegisteredEntry.Directory("/")

actual fun openInputFile(name: String): ByteProvider {
    val found = registeredFilesRoot.find(name) ?: throw MPFileNotFoundException("File not found: ${name}")
    if (found !is RegisteredEntry.File) {
        throw MPFileException("Pathname is not a file file: ${name}")
    }
    return ByteArrayByteProvider(found.content.toByteArray(), name)
}

actual fun openInputCharFile(name: String): CharacterProvider {
    return ByteToCharacterProvider(openInputFile(name))
}

actual fun openOutputCharFile(name: String): CharacterConsumer {
    val fileData = when (val entry = registeredFilesRoot.find(name)) {
        null -> registeredFilesRoot.registerFile(name, ByteArray(0))
        is RegisteredEntry.File -> entry.content
        is RegisteredEntry.Directory -> throw MPFileException("Name is a directory: ${name}")
    }
    return CharacterToByteConsumer(FileEntryDataByteConsumer(fileData))
}

actual fun fileType(path: String): FileNameType? {
    val found = registeredFilesRoot.find(path)
    return when {
        found == null -> null
        found is RegisteredEntry.File -> FileNameType.FILE
        found is RegisteredEntry.Directory -> FileNameType.DIRECTORY
        else -> FileNameType.UNDEFINED
    }
}

actual fun currentDirectory(): String {
    return "/"
}

actual fun readDirectoryContent(dirName: String): List<PathEntry> {
    val dir = registeredFilesRoot.find(dirName) ?: throw MPFileException("Path not found: ${dirName}")
    if (dir !is RegisteredEntry.Directory) throw MPFileException("Path does not indicate a directory name: ${dirName}")
    val result = ArrayList<PathEntry>()
    dir.files.values.forEach { file ->
        val e = when (file) {
            is RegisteredEntry.File -> PathEntry(file.name, file.content.size.toLong(), FileNameType.FILE)
            is RegisteredEntry.Directory -> PathEntry(file.name, 0, FileNameType.DIRECTORY)
        }
        result.add(e)
    }
    return result
}

actual fun resolveDirectoryPathInt(fileName: String, workingDirectory: String): String {
    return if (fileName.startsWith("/")) {
        fileName
    } else {
        var i = workingDirectory.length
        while (i > 0 && workingDirectory[i - 1] == '/') {
            i--
        }
        val fixedDirName = workingDirectory.substring(0, i)
        "${fixedDirName}/${fileName}"
    }
}
