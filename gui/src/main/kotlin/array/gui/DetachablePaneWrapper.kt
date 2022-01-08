package array.gui

import com.panemu.tiwulfx.control.dock.DetachableTabPane

class DetachablePaneWrapper {
    var pane = DetachableTabPane()

    init {
        pane.setOnClosedPassSibling { sibling -> pane = sibling }
    }
}
