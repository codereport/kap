package array

actual fun platformInit(engine: Engine) {
    engine.systemParameters[engine.standardSymbols.platform] = ConstantSymbolSystemParameterProvider(engine.internSymbol("linux", engine.coreNamespace))
}
