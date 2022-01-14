package array

import kotlinx.cinterop.*
import platform.posix.*

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
                        Char.toCodePoint(ch, low)
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

class KeyboardInputNative : KeyboardInput {
    override fun readString(prompt: String): String? {
        print(prompt)
        memScoped {
            val bufSize = 10240
            val buf = allocArray<ByteVar>(bufSize)
            return fgets(buf, bufSize, stdin)?.toKString()
        }
    }
}

actual fun makeKeyboardInput(): KeyboardInput {
    return KeyboardInputNative()
}

actual fun openInputCharFile(name: String): CharacterProvider {
    return ByteToCharacterProvider(LinuxByteProvider(openInputWithTranslatedExceptions(name), name))
}

class LinuxCharConsumer(val fd: Int) : CharacterConsumer {
    override fun writeChar(ch: Int) {
        memScoped {
            val utf = charToString(ch).encodeToByteArray()
            val buf = allocArray<ByteVar>(utf.size)
            utf.forEachIndexed { i, value ->
                buf[i] = value
            }
            val result = write(fd, buf, utf.size.toULong())
            if (result == -1L) {
                translateErrno(errno)
            }
        }
    }

    override fun close() {
        val result = close(fd)
        if (result == -1) {
            translateErrno(errno)
        }
    }
}

actual fun openOutputCharFile(name: String): CharacterConsumer {
    val fd = openOutputWithTranslatedExceptions(name)
    return LinuxCharConsumer(fd)
}

@OptIn(ExperimentalUnsignedTypes::class)
class LinuxByteProvider(val fd: Int, val name: String) : ByteProvider {
    override fun sourceName() = name

    override fun readByte(): Byte? {
        val buf = ByteArray(1)
        val result = readBlock(buf, 0, 1)
        return if (result == 0) {
            null
        } else {
            buf[0]
        }
    }

    override fun readBlock(buffer: ByteArray, start: Int?, length: Int?): Int {
        val startPos = start ?: 0
        val lengthInt = length ?: (buffer.size - startPos)
        memScoped {
            val buf = allocArray<ByteVar>(lengthInt)
            val result = read(fd, buf, lengthInt.toULong())
            if (result == -1L) {
                throw MPFileException(nativeErrorString())
            }
            val resultLen = result.toInt()
            for (i in 0 until resultLen) {
                buffer[startPos + i] = buf[i].toByte()
            }
            return resultLen
        }
    }

    override fun close() {
        if (close(fd) == -1) {
            throw MPFileException(nativeErrorString())
        }
    }
}

actual fun openInputFile(name: String): ByteProvider {
    return LinuxByteProvider(openInputWithTranslatedExceptions(name), name)
}

private fun openInputWithTranslatedExceptions(name: String): Int {
    val fd = open(name, O_RDONLY)
    if (fd == -1) {
        translateErrno(errno)
    }
    return fd
}

private fun openOutputWithTranslatedExceptions(name: String): Int {
    val fd = open(name, O_WRONLY or O_CREAT, 438) // rwxrwxrwx
    if (fd == -1) {
        translateErrno(errno)
    }
    return fd
}

private fun translateErrno(err: Int): Nothing {
    if (err == ENOENT) {
        throw MPFileNotFoundException(nativeErrorString())
    } else {
        throw MPFileException(nativeErrorString())
    }
}

private fun nativeErrorString(): String {
    return strerror(errno)?.toKString() ?: "unknown error"
}

@OptIn(ExperimentalUnsignedTypes::class)
actual fun fileType(path: String): FileNameType? {
    memScoped {
        val statInfo = alloc<stat>()
        val result = stat(path, statInfo.ptr)
        if (result != 0) {
            return null
        }
        val m = statInfo.st_mode and S_IFMT.toUInt()
        return when {
            m == S_IFREG.toUInt() -> FileNameType.FILE
            m == S_IFDIR.toUInt() -> FileNameType.DIRECTORY
            else -> FileNameType.UNDEFINED
        }
    }
}

actual fun currentDirectory(): String {
    memScoped {
        var size = 100
        var result: String? = null
        while (result == null) {
            val buf = allocArray<ByteVar>(size)
            val res = getcwd(buf, size.toULong())
            if (res != null) {
                result = res.toKString()
            } else {
                size *= 2
            }
        }
        return result
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
actual fun readDirectoryContent(dirName: String): List<PathEntry> {
    println("Loading directory: ${dirName}")
    val dir = opendir(dirName) ?: throw MPFileException("Can't open directory: ${dirName}")
    try {
        memScoped {
            val result = ArrayList<PathEntry>()
            val statInfo = alloc<stat>()
            while (true) {
                val ent = readdir(dir) ?: break
                val filename = ent.pointed.d_name.toKString()
                if (filename != "." && filename != "..") {
                    val statRes = stat("${dirName}/${filename}", statInfo.ptr)
                    if (statRes == 0) {
                        val fileNameType = when (ent.pointed.d_type.toInt()) {
                            DT_DIR -> FileNameType.DIRECTORY
                            DT_REG -> FileNameType.FILE
                            else -> FileNameType.UNDEFINED
                        }
                        val size = if (fileNameType == FileNameType.FILE) {
                            statInfo.st_size
                        } else {
                            0
                        }
                        result.add(PathEntry(filename, size, fileNameType))
                    }
                }
            }
            return result
        }
    } finally {
        closedir(dir)
    }
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
