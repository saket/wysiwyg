package me.saket.wysiwyg.internal

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tracing.trace
import me.saket.wysiwyg.MarkdownSpanPainter
import me.saket.wysiwyg.extendedspans.BlockQuoteSpanPainter
import me.saket.wysiwyg.extendedspans.RoundedCornerSpanPainter
import me.saket.wysiwyg.extendedspans.RoundedCornerSpanPainter.TextPaddingValues
import me.saket.wysiwyg.extendedspans.TaskCheckboxSpanPainter
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
import me.saket.wysiwyg.parser.LocalTextRange
import me.saket.wysiwyg.parser.MarkdownNode
import me.saket.wysiwyg.parser.MarkdownRenderScope
import me.saket.wysiwyg.parser.MarkdownRenderer
import me.saket.wysiwyg.parser.RenderResult
import me.saket.wysiwyg.parser.StrikeThroughNode
import me.saket.wysiwyg.parser.TaskListItemNode
import me.saket.wysiwyg.parser.TextChangeListSnapshot
import me.saket.wysiwyg.parser.ThematicBreakNode
import me.saket.wysiwyg.parser.editsOverlap
import me.saket.wysiwyg.parser.rebased

internal class AnnotatedStringMarkdownRenderer(
  private val buffer: TextFieldBuffer,
  private val unstyledText: String,
  private val changes: List<TextChangeListSnapshot>,
) : MarkdownRenderer {
  private var offsetInRoot: Int = 0
  private val spanPainters = mutableListOf<MarkdownSpanPainter>()

  override fun MarkdownRenderScope.render(node: MarkdownNode): RenderResult {
    when (node) {
      is BoldNode -> renderBold(node)
      is ItalicNode -> renderItalic(node)
      is InlineCodeNode -> renderInlineCode(node)
      is FencedCodeBlockNode -> renderFencedCodeBlock(node)
      is StrikeThroughNode -> renderStrikeThrough(node)
      is LinkNode -> renderLink(node)
      is BlockQuoteNode -> renderBlockQuote(node)
      is ListBlockNode -> renderListBlock(node)
      is ListItemNode -> renderListItem(node)
      is TaskListItemNode -> renderTaskListItem(node)
      is HeadingNode -> renderHeading(node)
      is ThematicBreakNode -> renderThematicBreak(node)
    }
    for (child in node.children) {
      offsetInRoot += child.offsetInParent
      this.render(child.node)
      offsetInRoot -= child.offsetInParent
    }
    return RenderResult(spanPainters = spanPainters)
  }

  /**
   * Resolves this local range to its absolute position in the rendered text.
   *
   * When [dropOnEdit] is true, returns `null` if any edit overlaps this range. The caller's
   * `?: return` then drops the node's styling for one frame until the reparse arrives. Use
   * it for fixed-shape markers (emphasis's `**`, a link's `]`, a list item's `-`, a
   * blockquote's `>`) where any edit invalidates the syntax. Skip it for repeatable markers
   * like a heading's `#`s.
   */
  context(scope: MarkdownRenderScope)
  private fun LocalTextRange.resolve(dropOnEdit: Boolean = false): TextRange? {
    val rangeInRoot = TextRange(
      start = localStart + offsetInRoot,
      end = localEnd + offsetInRoot,
    )
    val rebased = if (dropOnEdit && changes.editsOverlap(rangeInRoot)) {
      null
    } else {
      rangeInRoot.rebased(changes)
    }
    // Skip nodes whose absolute range falls outside the visible viewport.
    return if (rebased == null || scope.isInsideViewport(rebased)) {
      rebased
    } else {
      null
    }
  }

  private fun MarkdownRenderScope.renderDelimitedMarkers(node: DelimitedMarkdownNode): TextRange? {
    // Note to self: resolve the ranges before adding any style/span objects to avoid
    // allocating objects that aren't needed, and more importantly to avoid adding partial styles.
    val textRange = node.range.resolve() ?: return null
    val openingMarkerRange = node.openingMarkerRange.resolve(dropOnEdit = true) ?: return null
    val closingMarkerRange = node.closingMarkerRange.resolve(dropOnEdit = true) ?: return null

    val markerSpan = SpanStyle(theme.markerColor)
    addSpanStyle(markerSpan, openingMarkerRange)
    addSpanStyle(markerSpan, closingMarkerRange)
    return textRange
  }

  private fun MarkdownRenderScope.renderBold(node: BoldNode) {
    val range = renderDelimitedMarkers(node) ?: return
    addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold), range)
  }

  private fun MarkdownRenderScope.renderItalic(node: ItalicNode) {
    val range = renderDelimitedMarkers(node) ?: return
    addSpanStyle(SpanStyle(fontStyle = FontStyle.Italic), range)
  }

  private fun MarkdownRenderScope.renderInlineCode(node: InlineCodeNode) {
    val range = renderDelimitedMarkers(node) ?: return
    addSpanStyle(
      SpanStyle(fontFamily = FontFamily.Monospace),
      range,
    )
    spanPainters.add(
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

  private fun MarkdownRenderScope.renderFencedCodeBlock(node: FencedCodeBlockNode) {
    val range = renderDelimitedMarkers(node) ?: return
    val textStyle = SpanStyle(fontFamily = FontFamily.Monospace)
    val paragraphStyle = ParagraphStyle(
      textIndent = TextIndent(
        firstLine = theme.codeBlockLeadingPadding,
        restLine = theme.codeBlockLeadingPadding,
      )
    )
    addSpanStyle(textStyle, range)
    addParagraphStyle(paragraphStyle, range)
    spanPainters.add(
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

  private fun MarkdownRenderScope.renderStrikeThrough(node: StrikeThroughNode) {
    val range = node.range.resolve() ?: return
    node.openingMarkerRange.resolve(dropOnEdit = true) ?: return
    node.closingMarkerRange.resolve(dropOnEdit = true) ?: return

    val style = SpanStyle(
      color = theme.struckThroughTextColor,
      textDecoration = TextDecoration.LineThrough,
    )
    addSpanStyle(style, range)
  }

  private fun MarkdownRenderScope.renderLink(node: LinkNode) {
    node.range.resolve() ?: return
    val textRange = node.textRange.resolve() ?: return
    val textOpeningMarkerRange = node.textOpeningMarkerRange.resolve(dropOnEdit = true) ?: return
    val textClosingMarkerRange = node.textClosingMarkerRange.resolve(dropOnEdit = true) ?: return

    val urlRange = node.urlRange.resolve() ?: return
    val urlOpeningMarkerRange = node.urlOpeningMarkerRange.resolve(dropOnEdit = true) ?: return
    val urlClosingMarkerRange = node.urlClosingMarkerRange.resolve(dropOnEdit = true) ?: return

    addSpanStyle(SpanStyle(theme.linkTextColor), textRange)
    addSpanStyle(SpanStyle(theme.linkUrlColor), urlRange)

    val markerStyle = SpanStyle(color = theme.markerColor)
    addSpanStyle(markerStyle, textOpeningMarkerRange)
    addSpanStyle(markerStyle, textClosingMarkerRange)
    addSpanStyle(markerStyle, urlOpeningMarkerRange)
    addSpanStyle(markerStyle, urlClosingMarkerRange)
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
    addSpanStyle(SpanStyle(color = theme.markerColor), markerRange)
    addSpanStyle(textStyle, range)
    addParagraphStyle(paragraphStyle, range)

    spanPainters.add(BlockQuoteSpanPainter(range = range, markerColor = theme.markerColor))
  }

  private fun MarkdownRenderScope.renderListBlock(node: ListBlockNode) {
    node.range.resolve()
  }

  private fun MarkdownRenderScope.renderListItem(node: ListItemNode) {
    val range = node.range.resolve() ?: return
    val markerRange = node.markerRange.resolve(dropOnEdit = true) ?: return

    addParagraphStyleToContent(
      range = range,
      markerRange = markerRange,
    )

    addSpanStyle(
      SpanStyle(color = theme.markerColor, fontFamily = FontFamily.Monospace),
      markerRange,
    )
  }

  private fun MarkdownRenderScope.renderTaskListItem(node: TaskListItemNode) {
    val range = node.range.resolve() ?: return
    val markerRange = node.markerRange.resolve(dropOnEdit = true) ?: return

    addParagraphStyleToContent(
      range = range,
      markerRange = markerRange,
    )

    val markerColor = if (node.isChecked) theme.struckThroughTextColor else theme.markerColor
    addSpanStyle(
      SpanStyle(fontFamily = FontFamily.Monospace),
      markerRange,
    )

    val checkboxRange = node.checkboxRange.resolve(dropOnEdit = true) ?: return
    addSpanStyle(
      SpanStyle(markerColor),
      TextRange(markerRange.start, checkboxRange.start),
    )
    addSpanStyle(
      SpanStyle(color = markerColor),
      checkboxRange,
    )
    if (node.isChecked) {
      val contentRange = node.contentRange.resolve()
      if (contentRange != null) {
        addSpanStyle(
          SpanStyle(
            color = theme.struckThroughTextColor,
            textDecoration = TextDecoration.LineThrough,
          ),
          contentRange,
        )
      }
    }

    spanPainters.add(
      TaskCheckboxSpanPainter(range = checkboxRange, isChecked = node.isChecked),
    )
  }

  private fun MarkdownRenderScope.addParagraphStyleToContent(
    range: TextRange,
    markerRange: TextRange,
  ) {
    val lineEnd = unstyledText.lineEndAfter(range.start)
    val hasContentAfterMarker = unstyledText.hasContentBetween(markerRange.end, lineEnd)

    val restLineIndent = if (hasContentAfterMarker) {
      // TextMeasurer caches measurements internally so calling this on every render() is okay.
      val markerCharacterWidthPx = textMeasurer.measure(
        text = "X",
        style = textStyle.copy(fontFamily = FontFamily.Monospace),
      ).size.width
      (theme.listBlockLeadingPadding.toPx() + markerCharacterWidthPx * markerRange.length).toSp()
    } else {
      theme.listBlockLeadingPadding
    }

    val paragraphStyle = ParagraphStyle(
      textIndent = TextIndent(
        firstLine = theme.listBlockLeadingPadding,
        restLine = restLineIndent,
      )
    )
    addParagraphStyle(
      style = paragraphStyle,
      range = TextRange(range.start, maxOf(range.end, markerRange.end, lineEnd)),
    )
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
    if (!unstyledText.isAtxHeadingMarker(openingMarkerRange)) return

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

    addSpanStyle(
      style = SpanStyle(
        fontSize = 1.em * fontSizeMultiplier,
        fontWeight = FontWeight.Bold,
        color = theme.headingColor,
      ),
      range = range,
    )
    addSpanStyle(
      style = SpanStyle(color = theme.markerColor),
      range = openingMarkerRange,
    )
  }

  private fun MarkdownRenderScope.renderThematicBreak(node: ThematicBreakNode) {
    val range = node.range.resolve() ?: return
    addSpanStyle(
      SpanStyle(color = theme.markerColor),
      range,
    )
    spanPainters.add(
      ThematicBreakSpanPainter(range = range, markerColor = theme.markerColor)
    )
  }

  private fun addSpanStyle(style: SpanStyle, range: TextRange) {
    trace("Wysiwyg:render:addStyle") {
      buffer.addStyle(
        spanStyle = style,
        start = range.start.coerceAtMost(unstyledText.length - 1),
        end = range.end.coerceAtMost(unstyledText.length),
      )
    }
  }

  /**
   * @param trimVerticalPadding Compose always adds one extra empty line at every ParagraphStyle
   *   slice boundary on top of whatever the source markdown already produces. When this is true,
   *   the boundary newline's own line height is shrunk to nearly zero as a workaround, so the
   *   styled block sits flush against its neighbour, matching what a plain TextField would
   *   render. Author-intended blank-line separators are preserved because they contribute a
   *   *second* `\n` that we don't touch.
   *   https://issuetracker.google.com/issues/241426911
   */
  private fun addParagraphStyle(
    style: ParagraphStyle,
    range: TextRange,
    trimVerticalPadding: Boolean = true,
  ) {
    trace("Wysiwyg:render:addStyle") {
      buffer.addStyle(
        paragraphStyle = style,
        start = range.start.coerceAtMost(unstyledText.length - 1),
        end = range.end.coerceAtMost(unstyledText.length),
      )
      if (trimVerticalPadding) {
        if (unstyledText.getOrNull(range.start - 1) == '\n') {
          buffer.addStyle(
            paragraphStyle = TinyParagraphStyle,
            start = range.start - 1,
            end = range.start,
          )
        }
        if (unstyledText.getOrNull(range.end) == '\n') {
          buffer.addStyle(
            paragraphStyle = TinyParagraphStyle,
            start = range.end,
            end = range.end + 1,
          )
        }
      }
    }
  }

  companion object {
    private val TinyParagraphStyle = ParagraphStyle(
      lineHeight = 0.sp,
      lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
        mode = LineHeightStyle.Mode.Tight,
      ),
    )
  }
}

private fun String.lineEndAfter(offset: Int): Int {
  val newline = indexOf('\n', startIndex = offset)
  return if (newline == -1) length else newline
}

private fun String.hasContentBetween(start: Int, end: Int): Boolean {
  for (index in start.coerceAtMost(length) until end.coerceAtMost(length)) {
    if (!this[index].isWhitespace()) {
      return true
    }
  }
  return false
}

internal fun CharSequence.isAtxHeadingMarker(range: TextRange): Boolean {
  return range.end <= length &&
      range.length >= 2 &&
      this[range.end - 1].isWhitespace() &&
      (range.start..<range.end - 1).all { this[it] == '#' }
}
