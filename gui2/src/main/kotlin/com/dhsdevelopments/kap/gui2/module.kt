package com.dhsdevelopments.kap.gui2

import array.*
import com.dhsdevelopments.kap.gui2.arrayeditor.openInArrayEditor
import com.dhsdevelopments.kap.gui2.arrayeditor.valueFromIndex
import javax.swing.SwingUtilities

class Gui2Module : KapModule {
    override val name: String get() = "gui2"

    override fun init(engine: Engine) {
        val cmdNs = engine.makeNamespace("c")
        engine.registerFunction(cmdNs.internAndExport("edit"), OpenEditor())
        engine.registerFunction(cmdNs.internAndExport("editorValue"), EditorValueFunction())
    }
}

class OpenEditor : APLFunctionDescriptor {
    class OpenEditorImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val a0 = a.collapse()
            SwingUtilities.invokeLater {
                openInArrayEditor(a0)
            }
            return APLNullValue.APL_NULL_INSTANCE
        }
    }

    override fun make(instantiation: FunctionInstantiation) = OpenEditorImpl(instantiation)
}

class EditorValueFunction : APLFunctionDescriptor {
    class EditorValueFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val index = a.ensureNumber(pos).asInt(pos)
            return valueFromIndex(index) ?: throwAPLException(APLEvalException("Editor is not open", pos))
        }
    }

    override fun make(instantiation: FunctionInstantiation) = EditorValueFunctionImpl(instantiation)
}
