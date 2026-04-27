package me.saket.wysiwyg.highlight

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em
import androidx.compose.ui.util.fastForEach
import dev.drewhamilton.poko.Poko
import me.saket.wysiwyg.internal.MarkdownNodeRenderScope

interface MarkdownNode {
  val offsetInParent: Int
  val totalLength: Int

  fun MarkdownNodeRenderScope.render(text: AnnotatedString.Builder)
}

@JvmInline
value class LocalTextRange(
  val textRange: TextRange,
) {
  val start: Int get() = textRange.start
  val end: Int get() = textRange.end

  constructor(startOffset: Int, endOffset: Int) :
      this(TextRange(startOffset, endOffset))

  companion object {
    /**
     * Creates a local range starting at [startOffset] and spanning [length] code units.
     */
    fun span(startOffset: Int, length: Int): LocalTextRange {
      return LocalTextRange(TextRange(startOffset, startOffset + length))
    }
  }
}

@Poko
class MarkdownChildNode(
  val offsetInParent: Int,
  val node: MarkdownNode,
)

@Poko
class MarkdownDocument(
  override val totalLength: Int,
  val children: List<MarkdownChildNode>,
  val changes: TextChangeListSnapshot = TextChangeListSnapshot.Empty,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(text: AnnotatedString.Builder) {
    children.fastForEach { child ->
      child.render(text)
    }
  }

  fun copy(changes: TextChangeListSnapshot): MarkdownDocument {
    return MarkdownDocument(
      totalLength = this.totalLength,
      children = this.children,
      changes = changes,
    )
  }
}

interface DelimitedMarkdownNode : MarkdownNode {
  val textRange: LocalTextRange
  val openingMarkerRange: LocalTextRange
  val closingMarkerRange: LocalTextRange

  fun MarkdownNodeRenderScope.renderText(text: AnnotatedString.Builder, range: TextRange)

  override fun MarkdownNodeRenderScope.render(text: AnnotatedString.Builder) {
    // Note to self: resolve the ranges before adding any style/span objects to avoid
    // allocating objects that aren't needed, and more importantly to avoid adding partial styles.
    val textRange = textRange.resolve() ?: return
    val openingMarkerRange = openingMarkerRange.resolve() ?: return
    val closingMarkerRange = closingMarkerRange.resolve() ?: return

    val markerSpan = SpanStyle(theme.markerColor)
    text.addStyle(markerSpan, openingMarkerRange)
    text.addStyle(markerSpan, closingMarkerRange)
    renderText(text, textRange)
  }
}

@Poko
class BoldNode(
  override val totalLength: Int,
  override val textRange: LocalTextRange,
  override val openingMarkerRange: LocalTextRange,
  override val closingMarkerRange: LocalTextRange,
) : DelimitedMarkdownNode {

  override fun MarkdownNodeRenderScope.renderText(text: AnnotatedString.Builder, range: TextRange) {
    text.addStyle(SpanStyle(fontWeight = FontWeight.Bold), range)
  }
}

@Poko
class ItalicNode(
  override val totalLength: Int,
  override val textRange: LocalTextRange,
  override val openingMarkerRange: LocalTextRange,
  override val closingMarkerRange: LocalTextRange,
) : DelimitedMarkdownNode {

  override fun MarkdownNodeRenderScope.renderText(text: AnnotatedString.Builder, range: TextRange) {
    text.addStyle(SpanStyle(fontStyle = FontStyle.Italic), range)
  }
}

@Poko
class InlineCodeNode(
  override val totalLength: Int,
  override val textRange: LocalTextRange,
  override val openingMarkerRange: LocalTextRange,
  override val closingMarkerRange: LocalTextRange,
) : DelimitedMarkdownNode {

  override fun MarkdownNodeRenderScope.renderText(text: AnnotatedString.Builder, range: TextRange) {
    text.addStyle(
      SpanStyle(background = theme.codeBackground, fontFamily = FontFamily.Monospace),
      range,
    )
  }
}

@Poko
class FencedCodeBlockNode(
  override val totalLength: Int,
  override val textRange: LocalTextRange,
  override val openingMarkerRange: LocalTextRange,
  override val closingMarkerRange: LocalTextRange,
) : DelimitedMarkdownNode {

  override fun MarkdownNodeRenderScope.renderText(text: AnnotatedString.Builder, range: TextRange) {
    val textStyle = SpanStyle(
      background = theme.codeBackground,
      fontFamily = FontFamily.Monospace,
    )
    val paragraphStyle = ParagraphStyle(
      textIndent = TextIndent(
        firstLine = theme.codeBlockLeadingPadding,
        restLine = theme.codeBlockLeadingPadding,
      )
    )
    text.addStyle(textStyle, range)
    text.addStyle(paragraphStyle, range)
  }
}

@Poko
class StrikeThroughNode(
  override val totalLength: Int,
  val textRange: LocalTextRange,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(text: AnnotatedString.Builder) {
    val textRange = textRange.resolve() ?: return
    val style = SpanStyle(
      color = theme.struckThroughTextColor,
      textDecoration = TextDecoration.LineThrough,
    )
    text.addStyle(style, textRange)
  }
}

@Poko
class LinkNode(
  override val totalLength: Int,
  val textRange: LocalTextRange,
  val textOpeningMarkerRange: LocalTextRange,
  val textClosingMarkerRange: LocalTextRange,
  val urlRange: LocalTextRange,
  val urlOpeningMarkerRange: LocalTextRange,
  val urlClosingMarkerRange: LocalTextRange,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(text: AnnotatedString.Builder) {
    val textRange = textRange.resolve() ?: return
    val textOpeningMarkerRange = textOpeningMarkerRange.resolve() ?: return
    val textClosingMarkerRange = textClosingMarkerRange.resolve() ?: return

    val urlRange = urlRange.resolve() ?: return
    val urlOpeningMarkerRange = urlOpeningMarkerRange.resolve() ?: return
    val urlClosingMarkerRange = urlClosingMarkerRange.resolve() ?: return

    text.addStyle(SpanStyle(theme.linkTextColor), textRange)
    text.addStyle(SpanStyle(theme.linkUrlColor), urlRange)

    val markerStyle = SpanStyle(color = theme.markerColor)
    text.addStyle(markerStyle, textOpeningMarkerRange)
    text.addStyle(markerStyle, textClosingMarkerRange)
    text.addStyle(markerStyle, urlOpeningMarkerRange)
    text.addStyle(markerStyle, urlClosingMarkerRange)
  }
}

@Poko
class BlockQuoteNode(
  override val totalLength: Int,
  val textRange: LocalTextRange,
  val markerRange: LocalTextRange,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(text: AnnotatedString.Builder) {
    val textRange = textRange.resolve() ?: return
    val markerRange = markerRange.resolve() ?: return

    val textStyle = SpanStyle(color = theme.blockQuoteText)
    val paragraphStyle = ParagraphStyle(
      textIndent = TextIndent(
        firstLine = theme.blockQuoteLeadingPadding,
        restLine = theme.blockQuoteLeadingPadding,
      )
    )
    text.addStyle(SpanStyle(color = theme.markerColor), markerRange)
    text.addStyle(textStyle, textRange)
    text.addStyle(paragraphStyle, textRange)

    text.addStringAnnotation(
      tag = "blockquote",
      annotation = "ignored",
      start = textRange.start,
      end = textRange.end,
    )
  }
}

@Poko
class ListBlockNode(
  override val totalLength: Int,
  val paragraphRange: LocalTextRange,
  val children: List<MarkdownChildNode>,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(text: AnnotatedString.Builder) {
    val paragraphRange = paragraphRange.resolve()
    if (paragraphRange != null) {
      val paragraphStyle = ParagraphStyle(
        textIndent = TextIndent(
          firstLine = theme.listBlockLeadingPadding,
          restLine = theme.listBlockLeadingPadding,
        )
      )
      text.addStyle(paragraphStyle, paragraphRange)
    }
    children.fastForEach { child ->
      child.render(text)
    }
  }
}

@Poko
class ListItemNode(
  override val totalLength: Int,
  val markerRange: LocalTextRange,
  val children: List<MarkdownChildNode>,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(text: AnnotatedString.Builder) {
    val markerRange = markerRange.resolve() ?: return
    text.addStyle(SpanStyle(theme.markerColor), markerRange)

    children.fastForEach { child ->
      child.render(text)
    }
  }
}

@Poko
class HeadingNode(
  override val totalLength: Int,
  val textRange: LocalTextRange,
  val openingMarkerRange: LocalTextRange,
  val level: Int,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(text: AnnotatedString.Builder) {
    val textRange = textRange.resolve() ?: return
    val openingMarkerRange = openingMarkerRange.resolve() ?: return

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

    text.addStyle(
      style = SpanStyle(
        fontSize = 1.em * fontSizeMultiplier,
        fontWeight = FontWeight.Bold,
        color = theme.headingColor,
      ),
      range = textRange,
    )
    text.addStyle(
      style = SpanStyle(color = theme.markerColor),
      range = openingMarkerRange,
    )
  }
}

@Poko
class ThematicBreakNode(
  override val totalLength: Int,
  val textRange: LocalTextRange,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(text: AnnotatedString.Builder) {
    val textRange = textRange.resolve() ?: return
    text.addStringAnnotation(
      tag = "thematic_break",
      annotation = "ignored",
      start = textRange.start,
      end = textRange.end,
    )
    text.addStyle(
      SpanStyle(color = theme.markerColor),
      textRange,
    )
  }
}
