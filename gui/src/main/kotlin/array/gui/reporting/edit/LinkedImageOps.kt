package array.gui.reporting.edit

import org.fxmisc.richtext.model.NodeSegmentOpsBase


class LinkedImageOps<S> : NodeSegmentOpsBase<LinkedImage, S>(EmptyLinkedImage()) {
    override fun length(linkedImage: LinkedImage): Int {
        return if (linkedImage.isReal) 1 else 0
    }
}
