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
import androidx.compose.ui.util.fastMapNotNull
import dev.drewhamilton.poko.Poko
import me.saket.wysiwyg.internal.MarkdownRendererScope

interface MarkdownNode {
  val range: TextRange

  /**
   * Produces an interim version of this node after [changes], using only the previously parsed AST.
   *
   * Called by [IncrementalMarkdownParser] to keep highlighting flicker-free while the parser
   * computes a fresh AST for the edited text in background. Implementations should shift or resize
   * their cached ranges when the edit preserves the node's syntax, without doing any reparsing.
   *
   * Returns `null` when the edit crosses syntax that this node depends on, meaning the cached node
   * can no longer be trusted and should be dropped from the interim result.
   */
  fun rebased(changes: ChangeListSnapshot): MarkdownNode?

  fun MarkdownRendererScope.render(text: AnnotatedString.Builder)
}

class MarkdownDocument(
  override val range: TextRange,
  val children: List<MarkdownNode>,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder) {
    children.fastForEach { child ->
      with(child) { render(text) }
    }
  }

  override fun rebased(changes: ChangeListSnapshot): MarkdownDocument {
    return MarkdownDocument(
      range = range.rebased(changes) ?: TextRange.Zero,
      children = children.fastMapNotNull { child -> child.rebased(changes) },
    )
  }
}

abstract class DelimitedNode : MarkdownNode {
  abstract override val range: TextRange
  abstract val openingMarker: TextRange
  abstract val closingMarker: TextRange

  final override fun rebased(changes: ChangeListSnapshot): DelimitedNode? {
    if (changes.touchesAny(openingMarker, closingMarker)) return null
    return copy(
      range = range.rebased(changes) ?: return null,
      openingMarker = openingMarker.rebased(changes) ?: return null,
      closingMarker = closingMarker.rebased(changes) ?: return null,
    )
  }

  protected abstract fun copy(
    range: TextRange,
    openingMarker: TextRange,
    closingMarker: TextRange,
  ): DelimitedNode
}

@Poko
class BoldNode(
  override val range: TextRange,
  override val openingMarker: TextRange,
  override val closingMarker: TextRange,
) : DelimitedNode() {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder) {
    text.addStyle(SpanStyle(fontWeight = FontWeight.Bold), range)
    text.addStyle(SpanStyle(color = theme.markerColor), openingMarker)
    text.addStyle(SpanStyle(color = theme.markerColor), closingMarker)
  }

  override fun copy(
    range: TextRange,
    openingMarker: TextRange,
    closingMarker: TextRange,
  ): BoldNode {
    return BoldNode(range, openingMarker, closingMarker)
  }
}

@Poko
class ItalicNode(
  override val range: TextRange,
  override val openingMarker: TextRange,
  override val closingMarker: TextRange,
) : DelimitedNode() {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder) {
    text.addStyle(SpanStyle(fontStyle = FontStyle.Italic), range)
    text.addStyle(SpanStyle(color = theme.markerColor), openingMarker)
    text.addStyle(SpanStyle(color = theme.markerColor), closingMarker)
  }

  override fun copy(
    range: TextRange,
    openingMarker: TextRange,
    closingMarker: TextRange,
  ): ItalicNode {
    return ItalicNode(range, openingMarker, closingMarker)
  }
}

@Poko
class InlineCodeNode(
  override val range: TextRange,
  override val openingMarker: TextRange,
  override val closingMarker: TextRange,
) : DelimitedNode() {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder) {
    text.addStyle(
      style = SpanStyle(
        background = theme.codeBackground,
        fontFamily = FontFamily.Monospace,
      ),
      range = range,
    )
    text.addStyle(SpanStyle(color = theme.markerColor), openingMarker)
    text.addStyle(SpanStyle(color = theme.markerColor), closingMarker)
  }

  override fun copy(
    range: TextRange,
    openingMarker: TextRange,
    closingMarker: TextRange,
  ): InlineCodeNode {
    return InlineCodeNode(range, openingMarker, closingMarker)
  }
}

@Poko
class FencedCodeBlockNode(
  override val range: TextRange,
  override val openingMarker: TextRange,
  override val closingMarker: TextRange,
) : DelimitedNode() {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder) {
    text.addStyle(
      style = SpanStyle(
        background = theme.codeBackground,
        fontFamily = FontFamily.Monospace,
      ),
      range = range,
    )
    text.addStyle(
      style = ParagraphStyle(
        textIndent = TextIndent(
          firstLine = theme.codeBlockLeadingPadding,
          restLine = theme.codeBlockLeadingPadding,
        )
      ),
      range = range,
    )
    text.addStyle(
      style = SpanStyle(color = theme.markerColor),
      range = openingMarker,
    )
    text.addStyle(
      style = SpanStyle(color = theme.markerColor),
      range = closingMarker,
    )
  }

  override fun copy(
    range: TextRange,
    openingMarker: TextRange,
    closingMarker: TextRange,
  ): FencedCodeBlockNode {
    return FencedCodeBlockNode(range, openingMarker, closingMarker)
  }
}

@Poko
class StrikeThroughNode(
  override val range: TextRange,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder) {
    text.addStyle(
      style = SpanStyle(
        textDecoration = TextDecoration.LineThrough,
        color = theme.struckThroughTextColor,
      ),
      range = range,
    )
  }

  override fun rebased(changes: ChangeListSnapshot): StrikeThroughNode? {
    return StrikeThroughNode(
      range = range.rebased(changes) ?: return null,
    )
  }
}

@Poko
class LinkNode(
  override val range: TextRange,
  val textRange: TextRange,
  val urlRange: TextRange,
  val textOpeningMarker: TextRange,
  val textClosingMarker: TextRange,
  val linkOpeningMarker: TextRange,
  val linkClosingMarker: TextRange,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder) {
    text.addStyle(SpanStyle(color = theme.linkTextColor), textRange)
    text.addStyle(SpanStyle(color = theme.linkUrlColor), urlRange)
    text.addStyle(SpanStyle(color = theme.markerColor), textOpeningMarker)
    text.addStyle(SpanStyle(color = theme.markerColor), textClosingMarker)
    text.addStyle(SpanStyle(color = theme.markerColor), linkOpeningMarker)
    text.addStyle(SpanStyle(color = theme.markerColor), linkClosingMarker)
  }

  override fun rebased(changes: ChangeListSnapshot): LinkNode? {
    if (changes.touchesAny(
        textOpeningMarker,
        textClosingMarker,
        linkOpeningMarker,
        linkClosingMarker
      )
    ) {
      return null
    }
    return LinkNode(
      range = range.rebased(changes) ?: return null,
      textRange = textRange.rebased(changes) ?: return null,
      urlRange = urlRange.rebased(changes) ?: return null,
      textOpeningMarker = textOpeningMarker.rebased(changes) ?: return null,
      textClosingMarker = textClosingMarker.rebased(changes) ?: return null,
      linkOpeningMarker = linkOpeningMarker.rebased(changes) ?: return null,
      linkClosingMarker = linkClosingMarker.rebased(changes) ?: return null,
    )
  }
}

@Poko
class BlockQuoteNode(
  override val range: TextRange,
  val paragraphRange: TextRange,
  val openingMarker: TextRange,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder) {
    text.addStyle(
      style = SpanStyle(color = theme.blockQuoteText),
      range = range,
    )
    text.addStyle(
      style = ParagraphStyle(
        textIndent = TextIndent(
          firstLine = theme.blockQuoteLeadingPadding,
          restLine = theme.blockQuoteLeadingPadding,
        )
      ),
      range = range,
    )
    text.addStyle(
      style = SpanStyle(color = theme.markerColor),
      range = openingMarker,
    )
    text.addStringAnnotation(
      tag = "blockquote",
      annotation = "ignored",
      start = paragraphRange.start,
      end = paragraphRange.end,
    )
  }

  override fun rebased(changes: ChangeListSnapshot): BlockQuoteNode? {
    if (changes.touches(openingMarker)) return null
    return BlockQuoteNode(
      range = range.rebased(changes) ?: return null,
      paragraphRange = paragraphRange.rebased(changes) ?: return null,
      openingMarker = openingMarker.rebased(changes) ?: return null,
    )
  }
}

@Poko
class ListBlockNode(
  override val range: TextRange,
  val items: List<MarkdownNode>,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder) {
    text.addStyle(
      style = ParagraphStyle(
        textIndent = TextIndent(
          firstLine = theme.listBlockLeadingPadding,
          restLine = theme.listBlockLeadingPadding,
        )
      ),
      range = range,
    )
    items.fastForEach { item ->
      with(item) { render(text) }
    }
  }

  override fun rebased(changes: ChangeListSnapshot): MarkdownNode? {
    return ListBlockNode(
      range = range.rebased(changes) ?: return null,
      items = items
        .fastMapNotNull { item -> item.rebased(changes) }
        .ifEmpty { return null },
    )
  }
}

@Poko
class ListItemNode(
  override val range: TextRange,
  val marker: TextRange,
  val children: List<MarkdownNode>,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder) {
    text.addStyle(
      style = SpanStyle(color = theme.markerColor),
      range = marker,
    )
    children.fastForEach { child ->
      with(child) { render(text) }
    }
  }

  override fun rebased(changes: ChangeListSnapshot): ListItemNode? {
    if (changes.touches(marker)) return null

    return ListItemNode(
      range = range.rebased(changes) ?: return null,
      marker = marker.rebased(changes) ?: return null,
      children = children.fastMapNotNull { child -> child.rebased(changes) },
    )
  }
}

@Poko
class HeadingNode(
  override val range: TextRange,
  val openingMarker: TextRange,
  val level: Int,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder) {
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
      range = range,
    )
    text.addStyle(
      style = SpanStyle(color = theme.markerColor),
      range = openingMarker,
    )
  }

  override fun rebased(changes: ChangeListSnapshot): HeadingNode? {
    if (changes.touches(openingMarker)) return null
    return HeadingNode(
      range = range.rebased(changes) ?: return null,
      openingMarker = openingMarker.rebased(changes) ?: return null,
      level = level,
    )
  }
}

@Poko
class ThematicBreakNode(
  override val range: TextRange,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder) {
    text.addStringAnnotation(
      tag = "thematic_break",
      annotation = "ignored",
      start = range.start,
      end = range.end,
    )
    text.addStyle(
      style = SpanStyle(color = theme.markerColor),
      range = range,
    )
  }

  override fun rebased(changes: ChangeListSnapshot): ThematicBreakNode? {
    if (changes.touches(range)) return null
    return ThematicBreakNode(
      range = range.rebased(changes) ?: return null,
    )
  }
}
