package array

import java.nio.file.Path

val jarDirectories = mutableListOf(Path.of("lib"))

class LibClassLoader : ClassLoader() {

}

private fun initClassloader() {
    val loader = LibClassLoader()
}
