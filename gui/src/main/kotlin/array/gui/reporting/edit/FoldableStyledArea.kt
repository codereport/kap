package array.gui.reporting.edit

import javafx.scene.Node
import javafx.scene.paint.Color
import javafx.scene.text.TextFlow
import org.fxmisc.richtext.GenericStyledArea
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.model.SegmentOps
import org.fxmisc.richtext.model.StyledSegment
import org.fxmisc.richtext.model.TextOps
import org.fxmisc.richtext.model.TwoDimensional
import org.reactfx.util.Either
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.UnaryOperator

class FoldableStyledArea : GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle>(
    ParStyle.EMPTY,  // default paragraph style
    applyParagraphStyle(),  // paragraph style setter
    initialTextStyle(),  // default segment style
    segOps(),  // segment operations
    nodeFactory()
) {
    fun foldParagraphs(startPar: Int, endPar: Int) {
        foldParagraphs(startPar, endPar, addFoldStyle)
    }

    fun foldSelectedParagraphs() {
        foldSelectedParagraphs(addFoldStyle)
    }

    fun foldText(start: Int, end: Int) {
        fold(start, end, addFoldStyle)
    }

    fun unfoldParagraphs(startingFromPar: Int) {
        unfoldParagraphs(startingFromPar, foldStyleCheck, removeFoldStyle)
    }

    fun unfoldText(startingFromPos: Int) {
        var startingFromPos0 = startingFromPos
        startingFromPos0 = offsetToPosition(startingFromPos0, TwoDimensional.Bias.Backward).major
        unfoldParagraphs(startingFromPos0, foldStyleCheck, removeFoldStyle)
    }

    protected val addFoldStyle get() = UnaryOperator { pstyle: ParStyle -> pstyle.updateFold(true) }
    protected val removeFoldStyle get() = UnaryOperator { pstyle: ParStyle -> pstyle.updateFold(false) }
    protected val foldStyleCheck get() = Predicate<ParStyle> { pstyle -> pstyle.isFolded }

    companion object {
        private val styledTextOps = SegmentOps.styledTextOps<TextStyle?>()

        private val linkedImageOps: LinkedImageOps<TextStyle> = LinkedImageOps()

        private fun createNode(
            seg: StyledSegment<Either<String, LinkedImage>, TextStyle>,
            applyStyle: BiConsumer<in TextExt, TextStyle>): Node {
            return seg.getSegment().unify(
                { text -> StyledTextArea.createStyledTextNode(text, seg.getStyle(), applyStyle) },
                LinkedImage::createNode
            )
        }

        private fun nodeFactory(): Function<StyledSegment<Either<String, LinkedImage>, TextStyle>, Node> {
            return Function<StyledSegment<Either<String, LinkedImage>, TextStyle>, Node> { seg ->
                createNode(seg) { text: TextExt, style: TextStyle ->
                    text.style = style.toCss()
                }
            }
        }

        private fun segOps(): TextOps<Either<String, LinkedImage>, TextStyle> {
            return styledTextOps._or(
                linkedImageOps,
                { _, _ -> Optional.empty() })
        }

        private fun applyParagraphStyle(): BiConsumer<TextFlow, ParStyle> {
            return BiConsumer<TextFlow, ParStyle> { paragraph, style ->
                paragraph.style = style.toCss()
            }
        }

        private fun initialTextStyle(): TextStyle {
            return TextStyle
                .EMPTY
                .updateFontSize(12)
                .updateFontFamily("Serif")
                .updateTextColor(Color.BLACK)
        }
    }
}
