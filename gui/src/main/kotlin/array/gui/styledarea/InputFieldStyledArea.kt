package array.gui.styledarea

import javafx.scene.Node
import javafx.scene.text.TextFlow
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.model.SegmentOps
import org.fxmisc.richtext.model.StyledSegment
import org.fxmisc.richtext.model.TextOps
import java.util.function.BiConsumer
import java.util.function.Function

class InputFieldStyledArea : KAPEditorStyledArea<ParStyle, String, TextStyle>(
    ParStyle(),
    applyParagraphStyle,
    TextStyle(),
    segOps,
    nodeFactory
) {
    companion object {
        val applyParagraphStyle = BiConsumer<TextFlow, ParStyle> { flow, parStyle -> }
        val segOps: TextOps<String, TextStyle> = SegmentOps.styledTextOps()
        val nodeFactory = Function<StyledSegment<String, TextStyle>, Node> { seg ->
            val applyStyle = { a: TextExt, b: TextStyle ->
                b.styleContent(a)
            }
            StyledTextArea.createStyledTextNode(seg.segment, seg.style, applyStyle)
        }
    }
}
