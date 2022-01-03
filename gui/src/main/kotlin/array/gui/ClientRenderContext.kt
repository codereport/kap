package array.gui

import array.Engine

interface ClientRenderContext {
    fun engine(): Engine
    fun extendedInput(): ExtendedCharsKeyboardInput
}
