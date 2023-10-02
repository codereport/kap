package array

import array.jvmmod.JvmModule
import array.msofficereader.MsOfficeModule
import array.sql.SQLModule

actual fun platformInit(engine: Engine) {
    engine.systemParameters[engine.standardSymbols.platform] = ConstantSymbolSystemParameterProvider(engine.internSymbol("jvm", engine.coreNamespace))

    engine.addModule(MsOfficeModule())
    engine.addModule(SQLModule())
    engine.addModule(JvmModule())
    engine.addModule(JvmAudioModule())
}
