package array

class MetaModule : KapModule {
    override val name get() = "classloader-metamodule"

    override fun init(engine: Engine) {
        val ns = engine.makeNamespace("mod")
        engine.registerFunction(ns.internAndExport("load"), LoadJVMModuleFunction())
    }
}

class LoadJVMModuleFunction : APLFunctionDescriptor {
    class LoadJVMModuleFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            throwAPLException(APLEvalException("dynamic loading of modules not implemented"))
        }
    }

    override fun make(instantiation: FunctionInstantiation) = LoadJVMModuleFunctionImpl(instantiation)
}
