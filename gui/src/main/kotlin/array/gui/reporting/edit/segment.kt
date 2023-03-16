package array.gui.reporting.edit

import javafx.scene.Node
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.model.SegmentOps
import org.fxmisc.richtext.model.StyledSegment
import org.fxmisc.richtext.model.TextOps
import java.util.*

sealed class Segment {
    abstract fun length(): Int
    abstract fun charAt(index: Int): Char
    abstract fun getText(): String
    abstract fun subSequence(start: Int, end: Int? = null): Segment
    abstract fun createNode(styledSegment: StyledSegment<Segment, TextStyle>): Node
}

class StringSegment(val value: String) : Segment() {
    override fun length() = styledTextOps.length(value)
    override fun charAt(index: Int) = styledTextOps.charAt(value, index)
    override fun getText() = styledTextOps.getText(value)

    override fun subSequence(start: Int, end: Int?): Segment {
        val res = if (end != null) {
            styledTextOps.subSequence(value, start, end)
        } else {
            styledTextOps.subSequence(value, start)
        }
        return StringSegment(res)
    }

    fun joinWithString(nextSeg: StringSegment): Optional<Segment> {
        val res = styledTextOps.joinSeg(value, nextSeg.value)
        return if (res.isEmpty) {
            Optional.empty()
        } else {
            Optional.of(StringSegment(res.get()))
        }
    }

    override fun createNode(styledSegment: StyledSegment<Segment, TextStyle>): Node {
        return StyledTextArea.createStyledTextNode(value, styledSegment.getStyle()) { text: TextExt, style: TextStyle ->
            text.style = style.toCss()
        }
    }

    companion object {
        val styledTextOps = SegmentOps.styledTextOps<TextStyle?>()
    }
}

class DynamicValueSegment() : Segment() {
    override fun length(): Int {
        TODO("Not yet implemented")
    }

    override fun charAt(index: Int): Char {
        TODO("Not yet implemented")
    }

    override fun getText(): String {
        TODO("Not yet implemented")
    }

    override fun subSequence(start: Int, end: Int?): Segment {
        TODO("Not yet implemented")
    }

    override fun createNode(styledSegment: StyledSegment<Segment, TextStyle>): Node {
        TODO("Not yet implemented")
    }
}

class SegOps : TextOps<Segment, TextStyle> {
    override fun length(seg: Segment) = seg.length()
    override fun charAt(seg: Segment, index: Int) = seg.charAt(index)
    override fun getText(seg: Segment) = seg.getText()
    override fun subSequence(seg: Segment, start: Int, end: Int) = seg.subSequence(start, end)
    override fun subSequence(seg: Segment, start: Int) = seg.subSequence(start)

    override fun joinSeg(currentSeg: Segment, nextSeg: Segment): Optional<Segment> {
        if (currentSeg is StringSegment && nextSeg is StringSegment) {
            return currentSeg.joinWithString(nextSeg)
        } else {
            TODO("fix")
        }
    }

    override fun createEmptySeg() = StringSegment("")
    override fun create(text: String) = StringSegment(text)
}
