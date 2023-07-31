package com.dhsdevelopments.kap.gui2

import array.*
import com.dhsdevelopments.kap.gui2.arrayeditor.ArrayEditor
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.SwingUtilities

class Gui2Module : KapModule {
    override val name: String get() = "gui2"

    override fun init(engine: Engine) {
        val cmdNs = engine.makeNamespace("c")
        engine.registerFunction(cmdNs.internAndExport("edit"), OpenEditor())
    }
}

class OpenEditor : APLFunctionDescriptor {
    class OpenEditorImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val a0 = a.collapse()
            SwingUtilities.invokeLater {
                val frame = JFrame()
                val editor = ArrayEditor(a0)
                frame.contentPane = JScrollPane(editor)
                frame.pack()
                frame.isVisible = true
            }
            return APLNullValue.APL_NULL_INSTANCE
        }
    }

    override fun make(instantiation: FunctionInstantiation) = OpenEditorImpl(instantiation)
}
