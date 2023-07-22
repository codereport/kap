package array

import java.nio.file.Paths

val jarDirectories = mutableListOf(Paths.get("lib"))

class LibClassLoader : ClassLoader() {

}

private fun initClassloader() {
    val loader = LibClassLoader()
}
