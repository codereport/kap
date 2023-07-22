package array.gui.settings

import array.unless
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

@Serializable
enum class ReturnBehaviour { CLEAR_INPUT, PRESERVE }

@Serializable
data class Settings(
    val recentPath: String? = null,
    val directory: String? = null,
    val fontFamily: String? = null,
    val fontSize: Int? = null,
    val newlineBehaviour: ReturnBehaviour? = null,
    val keyPrefix: String? = null
) {
    fun fontFamilyWithDefault() = fontFamily ?: "Iosevka Fixed"
    fun fontSizeWithDefault() = fontSize ?: 10
    fun newlineBehaviourWithDefault() = newlineBehaviour ?: ReturnBehaviour.CLEAR_INPUT
    fun keyPrefixWithDefault() = keyPrefix ?: "`"
}


private fun findSettingsDirectory(): Path? {
    val homeString = System.getProperty("user.home") ?: return null
    val home = Paths.get(homeString)
    unless(home.exists()) {
        return null
    }
    val configDir = home.resolve(".kap")
    return when {
        !configDir.exists() -> configDir.also { configDir.createDirectory() }
        configDir.isDirectory() -> configDir
        else -> null
    }
}

fun loadSettings(): Settings {
    val settingsDir = findSettingsDirectory() ?: throw IOException("Unable to find settings directory")
    val settingsFile = settingsDir.resolve("kap.conf")
    if (!settingsFile.exists()) {
        return Settings()
    }
    val content = settingsFile.readText()
    return Json.decodeFromString(content)
}

fun saveSettings(settings: Settings) {
    val settingsDir = findSettingsDirectory() ?: throw IOException("Unable to find settings directory")
    val settingsFile = settingsDir.resolve("kap.conf")
    val content = Json.encodeToString(settings)
    settingsFile.writeText(content)
}
