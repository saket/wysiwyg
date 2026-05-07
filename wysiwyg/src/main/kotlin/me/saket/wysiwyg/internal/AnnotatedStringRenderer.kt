package me.saket.wysiwyg.internal

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
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.extendedspans.BlockQuoteSpanPainter
import me.saket.wysiwyg.extendedspans.RoundedCornerSpanPainter
import me.saket.wysiwyg.extendedspans.RoundedCornerSpanPainter.TextPaddingValues
import me.saket.wysiwyg.extendedspans.ThematicBreakSpanPainter
import me.saket.wysiwyg.parser.BlockQuoteNode
import me.saket.wysiwyg.parser.BoldNode
import me.saket.wysiwyg.parser.DelimitedMarkdownNode
import me.saket.wysiwyg.parser.FencedCodeBlockNode
import me.saket.wysiwyg.parser.HeadingNode
import me.saket.wysiwyg.parser.InlineCodeNode
import me.saket.wysiwyg.parser.ItalicNode
import me.saket.wysiwyg.parser.LinkNode
import me.saket.wysiwyg.parser.ListBlockNode
import me.saket.wysiwyg.parser.ListItemNode
import me.saket.wysiwyg.parser.MarkdownDocument
import me.saket.wysiwyg.parser.MarkdownNode
import me.saket.wysiwyg.parser.MarkdownRenderScope
import me.saket.wysiwyg.parser.MarkdownRenderer
import me.saket.wysiwyg.parser.StrikeThroughNode
import me.saket.wysiwyg.parser.ThematicBreakNode
import me.saket.wysiwyg.parser.descendInto

internal class AnnotatedStringRenderer(
  var theme: WysiwygTheme,
) : MarkdownRenderer {

  var buffer: MarkdownStyleBuffer = MarkdownStyleBuffer.Empty

  override fun render(node: MarkdownNode, scope: MarkdownRenderScope) {
    if (node is MarkdownDocument) {
      scope.offsetInRoot = 0
      scope.changes = node.changes
    }
    when (node) {
      is BoldNode -> scope.renderDelimited(node) { range ->
        buffer.addStyle(SpanStyle(fontWeight = FontWeight.Bold), range)
      }
      is ItalicNode -> scope.renderDelimited(node) { range ->
        buffer.addStyle(SpanStyle(fontStyle = FontStyle.Italic), range)
      }
      is InlineCodeNode -> scope.renderDelimited(node) { range ->
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
      is FencedCodeBlockNode -> scope.renderDelimited(node) { range ->
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
      is StrikeThroughNode -> scope.renderStrikeThrough(node)
      is LinkNode -> scope.renderLink(node)
      is BlockQuoteNode -> scope.renderBlockQuote(node)
      is ListBlockNode -> {
        scope.renderListBlock(node)
        descendInto(node, scope)
      }
      is ListItemNode -> {
        if (scope.renderListItem(node)) {
          descendInto(node, scope)
        }
      }
      is HeadingNode -> scope.renderHeading(node)
      is ThematicBreakNode -> scope.renderThematicBreak(node)
      is MarkdownDocument -> descendInto(node, scope)
      else -> descendInto(node, scope)
    }
  }

  private inline fun MarkdownRenderScope.renderDelimited(
    node: DelimitedMarkdownNode,
    renderText: (TextRange) -> Unit,
  ) {
    // Note to self: resolve the ranges before adding any style/span objects to avoid
    // allocating objects that aren't needed, and more importantly to avoid adding partial styles.
    val textRange = node.range.resolve() ?: return
    val openingMarkerRange = node.openingMarkerRange.resolve(dropOnEdit = true) ?: return
    val closingMarkerRange = node.closingMarkerRange.resolve(dropOnEdit = true) ?: return

    val markerSpan = SpanStyle(theme.markerColor)
    buffer.addStyle(markerSpan, openingMarkerRange)
    buffer.addStyle(markerSpan, closingMarkerRange)
    renderText(textRange)
  }

  private fun MarkdownRenderScope.renderStrikeThrough(node: StrikeThroughNode) {
    val range = node.range.resolve() ?: return
    val style = SpanStyle(
      color = theme.struckThroughTextColor,
      textDecoration = TextDecoration.LineThrough,
    )
    buffer.addStyle(style, range)
  }

  private fun MarkdownRenderScope.renderLink(node: LinkNode) {
    val textRange = node.textRange.resolve() ?: return
    val textOpeningMarkerRange = node.textOpeningMarkerRange.resolve(dropOnEdit = true) ?: return
    val textClosingMarkerRange = node.textClosingMarkerRange.resolve(dropOnEdit = true) ?: return

    val urlRange = node.urlRange.resolve() ?: return
    val urlOpeningMarkerRange = node.urlOpeningMarkerRange.resolve(dropOnEdit = true) ?: return
    val urlClosingMarkerRange = node.urlClosingMarkerRange.resolve(dropOnEdit = true) ?: return

    buffer.addStyle(SpanStyle(theme.linkTextColor), textRange)
    buffer.addStyle(SpanStyle(theme.linkUrlColor), urlRange)

    val markerStyle = SpanStyle(color = theme.markerColor)
    buffer.addStyle(markerStyle, textOpeningMarkerRange)
    buffer.addStyle(markerStyle, textClosingMarkerRange)
    buffer.addStyle(markerStyle, urlOpeningMarkerRange)
    buffer.addStyle(markerStyle, urlClosingMarkerRange)
  }

  private fun MarkdownRenderScope.renderBlockQuote(node: BlockQuoteNode) {
    val range = node.range.resolve() ?: return
    val markerRange = node.markerRange.resolve(dropOnEdit = true) ?: return

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
  }

  private fun MarkdownRenderScope.renderListBlock(node: ListBlockNode) {
    val range = node.range.resolve() ?: return
    val paragraphStyle = ParagraphStyle(
      textIndent = TextIndent(
        firstLine = theme.listBlockLeadingPadding,
        restLine = theme.listBlockLeadingPadding,
      )
    )
    buffer.addStyle(paragraphStyle, range, trimVerticalPadding = true)
  }

  /** Returns true when the marker resolved and children should be rendered. */
  private fun MarkdownRenderScope.renderListItem(node: ListItemNode): Boolean {
    val markerRange = node.markerRange.resolve(dropOnEdit = true) ?: return false
    buffer.addStyle(SpanStyle(theme.markerColor), markerRange)
    return true
  }

  private fun MarkdownRenderScope.renderHeading(node: HeadingNode) {
    val range = node.range.resolve() ?: return
    val openingMarkerRange = node.openingMarkerRange.resolve() ?: return

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
      when (node.level) {
        1 -> h1
        2 -> h2
        3 -> h3
        4 -> h4
        5 -> h5
        6 -> h6
        else -> error("invalid level: ${node.level}")
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
  }

  private fun MarkdownRenderScope.renderThematicBreak(node: ThematicBreakNode) {
    val range = node.range.resolve() ?: return
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
