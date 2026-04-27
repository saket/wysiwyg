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
import me.saket.wysiwyg.highlight.MarkdownEditOverlay.Companion.overlayed
import me.saket.wysiwyg.internal.MarkdownRendererScope

interface MarkdownNode {
  val offsetInParent: Int
  val totalLength: Int

  fun MarkdownRendererScope.render(text: AnnotatedString.Builder, startOffset: Int)
}

@Poko
class MarkdownDocument(
  override val offsetInParent: Int = 0,
  override val totalLength: Int,
  val children: List<MarkdownNode>,
  val overlay: MarkdownEditOverlay = MarkdownEditOverlay.Empty,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, startOffset: Int) {
    children.renderEach(text, startOffset)
  }

  fun copy(overlay: MarkdownEditOverlay): MarkdownDocument {
    return MarkdownDocument(
      totalLength = this.totalLength,
      children = this.children,
      overlay = overlay,
    )
  }
}

interface DelimitedMarkdownNode : MarkdownNode {
  val openingMarkerLength: Int
  val closingMarkerLength: Int

  fun MarkdownRendererScope.renderText(text: AnnotatedString.Builder, range: TextRange)

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, startOffset: Int) {
    // Note to self: resolve the ranges before adding any style/span objects to avoid
    // allocating objects that aren't needed, and more importantly to avoid adding partial styles.
    val textRange = TextRange.span(startOffset, totalLength).overlayed(editOverlay) ?: return
    val openingMarkerRange = openingMarkerRange(startOffset, editOverlay) ?: return
    val closingMarkerRange = closingMarkerRange(startOffset, editOverlay) ?: return

    val markerSpan = SpanStyle(theme.markerColor)
    text.addStyle(markerSpan, openingMarkerRange)
    text.addStyle(markerSpan, closingMarkerRange)
    renderText(text, textRange)
  }

  companion object {
    @Suppress("NOTHING_TO_INLINE")
    inline fun DelimitedMarkdownNode.openingMarkerRange(
      startOffset: Int,
      overlay: MarkdownEditOverlay
    ): TextRange? {
      return TextRange.span(startOffset, openingMarkerLength).overlayed(overlay)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun DelimitedMarkdownNode.closingMarkerRange(
      startOffset: Int,
      overlay: MarkdownEditOverlay
    ): TextRange? {
      return TextRange(totalLength - closingMarkerLength, totalLength)
        .translated(startOffset)
        .overlayed(overlay)
    }
  }
}

@Poko
class BoldNode(
  override val offsetInParent: Int,
  override val totalLength: Int,
  override val openingMarkerLength: Int,
  override val closingMarkerLength: Int,
) : DelimitedMarkdownNode {

  override fun MarkdownRendererScope.renderText(text: AnnotatedString.Builder, range: TextRange) {
    text.addStyle(SpanStyle(fontWeight = FontWeight.Bold), range)
  }
}

@Poko
class ItalicNode(
  override val offsetInParent: Int,
  override val totalLength: Int,
  override val openingMarkerLength: Int,
  override val closingMarkerLength: Int,
) : DelimitedMarkdownNode {

  override fun MarkdownRendererScope.renderText(text: AnnotatedString.Builder, range: TextRange) {
    text.addStyle(SpanStyle(fontStyle = FontStyle.Italic), range)
  }
}

@Poko
class InlineCodeNode(
  override val offsetInParent: Int,
  override val totalLength: Int,
  override val openingMarkerLength: Int,
  override val closingMarkerLength: Int,
) : DelimitedMarkdownNode {

  override fun MarkdownRendererScope.renderText(text: AnnotatedString.Builder, range: TextRange) {
    text.addStyle(
      SpanStyle(background = theme.codeBackground, fontFamily = FontFamily.Monospace),
      range,
    )
  }
}

@Poko
class FencedCodeBlockNode(
  override val offsetInParent: Int,
  override val totalLength: Int,
  override val openingMarkerLength: Int,
  override val closingMarkerLength: Int,
) : DelimitedMarkdownNode {

  override fun MarkdownRendererScope.renderText(text: AnnotatedString.Builder, range: TextRange) {
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
  override val offsetInParent: Int,
  override val totalLength: Int,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, startOffset: Int) {
    val textRange = TextRange.span(startOffset, totalLength).overlayed(editOverlay) ?: return
    val style = SpanStyle(
      color = theme.struckThroughTextColor,
      textDecoration = TextDecoration.LineThrough,
    )
    text.addStyle(style, textRange)
  }
}

@Poko
class LinkNode(
  override val offsetInParent: Int,
  override val totalLength: Int,
  val textLength: Int,
  val textOpeningMarkerLength: Int,
  val textClosingMarkerLength: Int,
  val urlLength: Int,
  val linkOpeningMarkerLength: Int,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, startOffset: Int) {
    val textOpeningMarkerRange = TextRange.span(startOffset, textOpeningMarkerLength)
      .overlayed(editOverlay)
      ?: return

    val textRange = TextRange.span(textOpeningMarkerRange.end, textLength)
      .overlayed(editOverlay)
      ?: return

    val textClosingMarkerRange = TextRange.span(textRange.end, textClosingMarkerLength)
      .overlayed(editOverlay)
      ?: return

    val linkOpeningMarkerRange = TextRange.span(textClosingMarkerRange.end, linkOpeningMarkerLength)
      .overlayed(editOverlay)
      ?: return

    val urlRange = TextRange.span(linkOpeningMarkerRange.end, urlLength)
      .overlayed(editOverlay)
      ?: return

    val linkClosingMarkerRange = TextRange(urlRange.end, startOffset + totalLength)
      .overlayed(editOverlay)
      ?: return

    text.addStyle(SpanStyle(theme.linkTextColor), textRange)
    text.addStyle(SpanStyle(theme.linkUrlColor), urlRange)

    val markerStyle = SpanStyle(color = theme.markerColor)
    text.addStyle(markerStyle, textOpeningMarkerRange)
    text.addStyle(markerStyle, textClosingMarkerRange)
    text.addStyle(markerStyle, linkOpeningMarkerRange)
    text.addStyle(markerStyle, linkClosingMarkerRange)
  }
}

@Poko
class BlockQuoteNode(
  override val offsetInParent: Int,
  override val totalLength: Int,
  val markerLength: Int,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, startOffset: Int) {
    val textRange = TextRange.span(startOffset, totalLength).overlayed(editOverlay) ?: return
    val markerRange = TextRange.span(startOffset, markerLength).overlayed(editOverlay) ?: return

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
  override val offsetInParent: Int,
  override val totalLength: Int,
  val children: List<MarkdownNode>,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, startOffset: Int) {
    val paragraphRange = TextRange.span(startOffset, totalLength).overlayed(editOverlay)
    if (paragraphRange != null) {
      val paragraphStyle = ParagraphStyle(
        textIndent = TextIndent(
          firstLine = theme.listBlockLeadingPadding,
          restLine = theme.listBlockLeadingPadding,
        )
      )
      text.addStyle(paragraphStyle, paragraphRange)
    }
    children.renderEach(text, startOffset)
  }
}

@Poko
class ListItemNode(
  override val offsetInParent: Int,
  override val totalLength: Int,
  val markerLength: Int,
  val children: List<MarkdownNode>,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, startOffset: Int) {
    val textRange = TextRange.span(startOffset, markerLength).overlayed(editOverlay) ?: return
    text.addStyle(SpanStyle(theme.markerColor), textRange)

    children.renderEach(text, startOffset)
  }
}

@Poko
class HeadingNode(
  override val offsetInParent: Int,
  override val totalLength: Int,
  val openingMarkerLength: Int,
  val level: Int,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, startOffset: Int) {
    val textRange = TextRange.span(startOffset, totalLength).overlayed(editOverlay) ?: return
    val openingMarkerRange = TextRange
      .span(startOffset, openingMarkerLength)
      .overlayed(editOverlay) ?: return

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
  override val offsetInParent: Int,
  override val totalLength: Int,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, startOffset: Int) {
    val textRange = TextRange.span(startOffset, totalLength).overlayed(editOverlay) ?: return
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

/**
 * Creates a root-relative range starting at [startOffset] and spanning [length] code units.
 *
 * FYI the second parameter is a length, not an end offset.
 */
@PublishedApi
@Suppress("NOTHING_TO_INLINE")
internal inline fun TextRange.Companion.span(startOffset: Int, length: Int): TextRange {
  return TextRange(startOffset, startOffset + length)
}

@PublishedApi
@Suppress("NOTHING_TO_INLINE")
internal inline fun TextRange.translated(offset: Int): TextRange {
  return TextRange(start + offset, end + offset)
}

context(rendererScope: MarkdownRendererScope)
private fun List<MarkdownNode>.renderEach(
  text: AnnotatedString.Builder,
  parentStartOffset: Int,
) {
  fastForEach { child ->
    with(child) {
      rendererScope.render(text, startOffset = parentStartOffset + child.offsetInParent)
    }
  }
}
