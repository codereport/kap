package array.gui.styledarea

import javafx.scene.Node
import javafx.scene.text.TextFlow
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.model.SegmentOps
import org.fxmisc.richtext.model.StyledSegment
import java.util.function.BiConsumer
import java.util.function.Function

class InputFieldStyledArea() : KAPEditorStyledArea<InputFieldParStyle, String, InputFieldTextStyle>(
    InputFieldParStyle(),
    applyParagraphStyle,
    InputFieldTextStyle(),
    segOps,
    nodeFactory
) {
    companion object {
        val applyParagraphStyle = BiConsumer<TextFlow, InputFieldParStyle> { flow, parStyle -> }
        val segOps = SegmentOps.styledTextOps<InputFieldTextStyle>()
        val nodeFactory = Function<StyledSegment<String, InputFieldTextStyle>, Node> { seg ->
            val applyStyle = { a: TextExt, b: InputFieldTextStyle ->
            }
            StyledTextArea.createStyledTextNode(seg.segment, seg.style, applyStyle)
        }
    }
}


class InputFieldParStyle
class InputFieldTextStyle
