package me.saket.wysiwyg.parser

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import dev.drewhamilton.poko.Poko
import me.saket.wysiwyg.extendedspans.BlockQuoteSpanPainter
import me.saket.wysiwyg.extendedspans.RoundedCornerSpanPainter
import me.saket.wysiwyg.extendedspans.RoundedCornerSpanPainter.TextPaddingValues
import me.saket.wysiwyg.extendedspans.TaskCheckboxSpanPainter
import me.saket.wysiwyg.extendedspans.ThematicBreakSpanPainter
import me.saket.wysiwyg.internal.MarkdownNodeRenderScope
import me.saket.wysiwyg.internal.MarkdownStyleBuffer

@Poko
class MarkdownDocument(
  override val range: LocalTextRange,
  val children: List<MarkdownChildNode>,
  val changes: List<TextChangeListSnapshot> = emptyList(),
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(buffer: MarkdownStyleBuffer) {
    children.fastForEach { child ->
      child.render(buffer)
    }
  }

  fun copy(changes: List<TextChangeListSnapshot>): MarkdownDocument {
    return MarkdownDocument(
      range = this.range,
      children = this.children,
      changes = changes,
    )
  }
}

interface DelimitedMarkdownNode : MarkdownNode {
  val openingMarkerRange: LocalTextRange
  val closingMarkerRange: LocalTextRange

  fun MarkdownNodeRenderScope.renderText(buffer: MarkdownStyleBuffer, range: TextRange)

  override fun MarkdownNodeRenderScope.render(buffer: MarkdownStyleBuffer) {
    // Note to self: resolve the ranges before adding any style/span objects to avoid
    // allocating objects that aren't needed, and more importantly to avoid adding partial styles.
    val textRange = range.resolve() ?: return
    val openingMarkerRange = openingMarkerRange.resolve(dropOnEdit = true) ?: return
    val closingMarkerRange = closingMarkerRange.resolve(dropOnEdit = true) ?: return

    val markerSpan = SpanStyle(theme.markerColor)
    buffer.addStyle(markerSpan, openingMarkerRange)
    buffer.addStyle(markerSpan, closingMarkerRange)
    renderText(buffer, textRange)
  }
}

@Poko
class BoldNode(
  override val range: LocalTextRange,
  override val openingMarkerRange: LocalTextRange,
  override val closingMarkerRange: LocalTextRange,
) : DelimitedMarkdownNode {

  override fun MarkdownNodeRenderScope.renderText(buffer: MarkdownStyleBuffer, range: TextRange) {
    buffer.addStyle(SpanStyle(fontWeight = FontWeight.Bold), range)
    buffer.addTestTag("b", range)
  }
}

@Poko
class ItalicNode(
  override val range: LocalTextRange,
  override val openingMarkerRange: LocalTextRange,
  override val closingMarkerRange: LocalTextRange,
) : DelimitedMarkdownNode {

  override fun MarkdownNodeRenderScope.renderText(buffer: MarkdownStyleBuffer, range: TextRange) {
    buffer.addStyle(SpanStyle(fontStyle = FontStyle.Italic), range)
  }
}

@Poko
class InlineCodeNode(
  override val range: LocalTextRange,
  override val openingMarkerRange: LocalTextRange,
  override val closingMarkerRange: LocalTextRange,
) : DelimitedMarkdownNode {

  override fun MarkdownNodeRenderScope.renderText(buffer: MarkdownStyleBuffer, range: TextRange) {
    buffer.addStyle(
      SpanStyle(fontFamily = FontFamily.Monospace),
      range,
    )
    buffer.addSpanPainter(
      RoundedCornerSpanPainter(
        range = range,
        backgroundColor = theme.codeBackground,
        cornerRadius = 4.sp,
        padding = TextPaddingValues(2.sp),
        topMargin = 0.sp,
        bottomMargin = 0.sp,
      )
    )
  }
}

@Poko
class FencedCodeBlockNode(
  override val range: LocalTextRange,
  override val openingMarkerRange: LocalTextRange,
  override val closingMarkerRange: LocalTextRange,
) : DelimitedMarkdownNode {

  override fun MarkdownNodeRenderScope.renderText(buffer: MarkdownStyleBuffer, range: TextRange) {
    val textStyle = SpanStyle(fontFamily = FontFamily.Monospace)
    val paragraphStyle = ParagraphStyle(
      textIndent = TextIndent(
        firstLine = theme.codeBlockLeadingPadding,
        restLine = theme.codeBlockLeadingPadding,
      )
    )
    buffer.addStyle(textStyle, range)
    buffer.addStyle(paragraphStyle, range, trimVerticalPadding = true)
    buffer.addSpanPainter(
      RoundedCornerSpanPainter(
        range = range,
        backgroundColor = theme.codeBackground,
        cornerRadius = 8.sp,
        padding = TextPaddingValues(horizontal = 0.sp, vertical = 2.sp),
        topMargin = 0.sp,
        bottomMargin = 0.sp,
      )
    )
  }
}

@Poko
class StrikeThroughNode(
  override val range: LocalTextRange,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(buffer: MarkdownStyleBuffer) {
    val range = range.resolve() ?: return
    val style = SpanStyle(
      color = theme.struckThroughTextColor,
      textDecoration = TextDecoration.LineThrough,
    )
    buffer.addStyle(style, range)
    buffer.addTestTag("s", range)
  }
}

@Poko
class LinkNode(
  override val range: LocalTextRange,
  val textRange: LocalTextRange,
  val textOpeningMarkerRange: LocalTextRange,
  val textClosingMarkerRange: LocalTextRange,
  val urlRange: LocalTextRange,
  val urlOpeningMarkerRange: LocalTextRange,
  val urlClosingMarkerRange: LocalTextRange,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(buffer: MarkdownStyleBuffer) {
    val range = range.resolve() ?: return
    val textRange = textRange.resolve() ?: return
    val textOpeningMarkerRange = textOpeningMarkerRange.resolve(dropOnEdit = true) ?: return
    val textClosingMarkerRange = textClosingMarkerRange.resolve(dropOnEdit = true) ?: return

    val urlRange = urlRange.resolve() ?: return
    val urlOpeningMarkerRange = urlOpeningMarkerRange.resolve(dropOnEdit = true) ?: return
    val urlClosingMarkerRange = urlClosingMarkerRange.resolve(dropOnEdit = true) ?: return

    buffer.addStyle(SpanStyle(theme.linkTextColor), textRange)
    buffer.addStyle(SpanStyle(theme.linkUrlColor), urlRange)

    val markerStyle = SpanStyle(color = theme.markerColor)
    buffer.addStyle(markerStyle, textOpeningMarkerRange)
    buffer.addStyle(markerStyle, textClosingMarkerRange)
    buffer.addStyle(markerStyle, urlOpeningMarkerRange)
    buffer.addStyle(markerStyle, urlClosingMarkerRange)

    buffer.addTestTag("link", range)
  }
}

@Poko
class BlockQuoteNode(
  override val range: LocalTextRange,
  val markerRange: LocalTextRange,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(buffer: MarkdownStyleBuffer) {
    val range = range.resolve() ?: return
    val markerRange = markerRange.resolve(dropOnEdit = true) ?: return

    val textStyle = SpanStyle(color = theme.blockQuoteText)
    val paragraphStyle = ParagraphStyle(
      textIndent = TextIndent(
        firstLine = theme.blockQuoteLeadingPadding,
        restLine = theme.blockQuoteLeadingPadding,
      )
    )
    buffer.addStyle(SpanStyle(color = theme.markerColor), markerRange)
    buffer.addStyle(textStyle, range)
    buffer.addStyle(paragraphStyle, range, trimVerticalPadding = true)

    buffer.addSpanPainter(BlockQuoteSpanPainter(range = range, markerColor = theme.markerColor))
    buffer.addTestTag("blockquote", range)
  }
}

@Poko
class ListBlockNode(
  override val range: LocalTextRange,
  val children: List<MarkdownChildNode>,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(buffer: MarkdownStyleBuffer) {
    val range = range.resolve()
    if (range != null) {
      val paragraphStyle = ParagraphStyle(
        textIndent = TextIndent(
          firstLine = theme.listBlockLeadingPadding,
          restLine = theme.listBlockLeadingPadding,
        )
      )
      buffer.addStyle(paragraphStyle, range, trimVerticalPadding = true)
      buffer.addTestTag("list", range)
    }
    children.fastForEach { child ->
      child.render(buffer)
    }
  }
}

@Poko
class ListItemNode(
  override val range: LocalTextRange,
  val markerRange: LocalTextRange,
  val children: List<MarkdownChildNode>,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(buffer: MarkdownStyleBuffer) {
    val markerRange = markerRange.resolve(dropOnEdit = true) ?: return
    buffer.addStyle(SpanStyle(theme.markerColor), markerRange)

    children.fastForEach { child ->
      child.render(buffer)
    }
  }
}

@Poko
class TaskListItemNode(
  override val range: LocalTextRange,
  val listItemMarkerRange: LocalTextRange,
  val taskMarkerRange: LocalTextRange,
  val textRange: LocalTextRange,
  val isChecked: Boolean,
  val children: List<MarkdownChildNode>,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(buffer: MarkdownStyleBuffer) {
    val listItemMarkerRange = listItemMarkerRange.resolve(dropOnEdit = true) ?: return
    val taskMarkerRange = taskMarkerRange.resolve(dropOnEdit = true) ?: return

    val markerColor = if (isChecked) theme.struckThroughTextColor else theme.markerColor
    buffer.addStyle(
      SpanStyle(markerColor),
      listItemMarkerRange
    )
    buffer.addStyle(
      SpanStyle(color = markerColor, fontFamily = FontFamily.Monospace),
      taskMarkerRange,
    )
    if (isChecked) {
      val textRange = textRange.resolve()
      if (textRange != null) {
        buffer.addStyle(
          SpanStyle(
            color = theme.struckThroughTextColor,
            textDecoration = TextDecoration.LineThrough,
          ),
          textRange,
        )
      }
    }

    buffer.addSpanPainter(
      TaskCheckboxSpanPainter(range = taskMarkerRange, isChecked = isChecked),
    )

    children.fastForEach { child ->
      child.render(buffer)
    }
  }
}

@Poko
class HeadingNode(
  override val range: LocalTextRange,
  val openingMarkerRange: LocalTextRange,
  val level: Int,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(buffer: MarkdownStyleBuffer) {
    val range = range.resolve() ?: return
    val openingMarkerRange = openingMarkerRange.resolve() ?: return

    // The `#`s are repeatable, so adding or removing them moves between heading levels
    // (h1 to h2) without breaking syntax. But replacing one with a non-`#` or deleting
    // the separator does break it, so the cached node must drop in those cases.
    //
    // Other nodes use `resolve(dropOnEdit = true)` because their markers are fixed-shape:
    // any edit overlapping `**`, `[`, `> `, or `- ` invalidates the syntax. Heading markers
    // aren't fixed-shape, their `#`s are repeatable, so adding or removing them moves
    // between heading levels (h1 to h2) without breaking syntax.
    if (!buffer.unstyledText.isAtxHeadingMarker(openingMarkerRange)) return

    val fontSizeMultiplier = with(theme.headingFontSizes) {
      when (level) {
        1 -> h1
        2 -> h2
        3 -> h3
        4 -> h4
        5 -> h5
        6 -> h6
        else -> error("invalid level: $level")
      }
    }

    buffer.addStyle(
      style = SpanStyle(
        fontSize = 1.em * fontSizeMultiplier,
        fontWeight = FontWeight.Bold,
        color = theme.headingColor,
      ),
      range = range,
    )
    buffer.addStyle(
      style = SpanStyle(color = theme.markerColor),
      range = openingMarkerRange,
    )
    buffer.addTestTag("h$level", range)
  }
}

@Poko
class ThematicBreakNode(
  override val range: LocalTextRange,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(buffer: MarkdownStyleBuffer) {
    val range = range.resolve() ?: return
    buffer.addStyle(
      SpanStyle(color = theme.markerColor),
      range,
    )
    buffer.addSpanPainter(
      ThematicBreakSpanPainter(range = range, markerColor = theme.markerColor)
    )
  }
}

private fun CharSequence.isAtxHeadingMarker(range: TextRange): Boolean {
  return range.end <= length &&
      range.length >= 2 &&
      this[range.end - 1].isWhitespace() &&
      (range.start..<range.end - 1).all { this[it] == '#' }
}
