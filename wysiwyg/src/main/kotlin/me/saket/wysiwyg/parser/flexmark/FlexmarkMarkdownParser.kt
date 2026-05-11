package me.saket.wysiwyg.parser.flexmark

import androidx.tracing.trace
import com.vladsch.flexmark.ast.BlockQuote
import com.vladsch.flexmark.ast.Code
import com.vladsch.flexmark.ast.Emphasis
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.ast.ListBlock
import com.vladsch.flexmark.ast.ListItem
import com.vladsch.flexmark.ast.StrongEmphasis
import com.vladsch.flexmark.ast.ThematicBreak
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItem
import com.vladsch.flexmark.util.misc.CharPredicate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.wysiwyg.parser.BlockQuoteNode
import me.saket.wysiwyg.parser.BoldNode
import me.saket.wysiwyg.parser.FencedCodeBlockNode
import me.saket.wysiwyg.parser.HeadingNode
import me.saket.wysiwyg.parser.InlineCodeNode
import me.saket.wysiwyg.parser.ItalicNode
import me.saket.wysiwyg.parser.LinkNode
import me.saket.wysiwyg.parser.ListBlockNode
import me.saket.wysiwyg.parser.ListItemNode
import me.saket.wysiwyg.parser.LocalTextRange
import me.saket.wysiwyg.parser.MarkdownChildNode
import me.saket.wysiwyg.parser.MarkdownDocument
import me.saket.wysiwyg.parser.MarkdownNode
import me.saket.wysiwyg.parser.MarkdownParser
import me.saket.wysiwyg.parser.StrikeThroughNode
import me.saket.wysiwyg.parser.TaskListItemNode
import me.saket.wysiwyg.parser.TextChangeListSnapshot
import me.saket.wysiwyg.parser.ThematicBreakNode
import com.vladsch.flexmark.parser.Parser as FlexmarkParser
import com.vladsch.flexmark.util.ast.Node as FlexmarkNode

// todo: restore extensions.
/**
 * Backed by [flexmark-java](https://github.com/vsch/flexmark-java).
 *
 * @param dispatcher The dispatcher on which flexmark's parsing + node emission runs.
 */
class FlexmarkMarkdownParser(
  private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : MarkdownParser {

  private val parser = FlexmarkParser.builder()
    .apply {
      // Disable parsers for unsupported syntaxes.
      set(FlexmarkParser.HTML_BLOCK_PARSER, false)
      set(FlexmarkParser.INDENTED_CODE_BLOCK_PARSER, false)
      set(FlexmarkParser.REFERENCE_PARAGRAPH_PRE_PROCESSOR, false)

      // List items should start with a space.
      set(FlexmarkParser.LISTS_ITEM_MARKER_SPACE, true)

      // Let an empty list item interrupt the preceding paragraph. CommonMark's default
      // requires non-empty content to break out, which causes a one-keystroke flicker
      // while typing a fresh "- [ ] " right below a paragraph: the empty task body
      // folds the line back into the paragraph until the user types content.
      set(FlexmarkParser.LISTS_EMPTY_BULLET_ITEM_INTERRUPTS_PARAGRAPH, true)
      set(FlexmarkParser.LISTS_EMPTY_ORDERED_ITEM_INTERRUPTS_PARAGRAPH, true)
      set(FlexmarkParser.LISTS_EMPTY_ORDERED_NON_ONE_ITEM_INTERRUPTS_PARAGRAPH, true)

      // Disable setext headings (e.g., `Foo\n---`). Without this, an empty bullet
      // typed below a paragraph (`Foo\n- `) is swallowed as a setext H2 underline
      // before the list parser gets a chance. The factory has no off switch, so set
      // the required marker length above anything a user could plausibly type.
      set(FlexmarkParser.HEADING_SETEXT_MARKER_LENGTH, Int.MAX_VALUE)
    }
    .extensions(listOf(StrikethroughExtension.create(), TaskListExtension.create()))
    .build()

  override suspend fun parse(text: String, changes: TextChangeListSnapshot): MarkdownDocument {
    return withContext(dispatcher) {
      val parsed = trace("Wysiwyg:flexmarkParse") {
        parser.parse(text)
      }
      MarkdownDocument(
        range = LocalTextRange.span(0, text.length),
        children = trace("Wysiwyg:convertFlexmarkAst") {
          parsed.walkSubtree { it.toWysiwygMarkdownNode() }
        },
      )
    }
  }

  private fun FlexmarkNode.toWysiwygMarkdownNode(): MarkdownNode? {
    return when (this) {
      is Emphasis -> {
        ItalicNode(
          range = LocalTextRange.span(0, chars.length),
          openingMarkerRange = LocalTextRange.span(0, openingMarker.length),
          closingMarkerRange = LocalTextRange(chars.length - closingMarker.length, chars.length),
        )
      }
      is StrongEmphasis -> {
        BoldNode(
          range = LocalTextRange.span(0, chars.length),
          openingMarkerRange = LocalTextRange.span(0, openingMarker.length),
          closingMarkerRange = LocalTextRange(chars.length - closingMarker.length, chars.length),
        )
      }
      is Strikethrough -> {
        StrikeThroughNode(
          range = LocalTextRange.span(0, chars.length),
          openingMarkerRange = LocalTextRange.span(0, openingMarker.length),
          closingMarkerRange = LocalTextRange(chars.length - closingMarker.length, chars.length),
        )
      }
      is Link -> {
        val textOpeningMarkerRange = LocalTextRange.span(
          startOffset = 0,
          length = textOpeningMarker.length,
        )
        val textRange = LocalTextRange.span(
          startOffset = textOpeningMarkerRange.end,
          length = text.length,
        )
        val textClosingMarkerRange = LocalTextRange.span(
          startOffset = textRange.end,
          length = textClosingMarker.length,
        )
        val linkOpeningMarkerRange = LocalTextRange.span(
          startOffset = textClosingMarkerRange.end,
          length = linkOpeningMarker.length,
        )
        val urlRange = LocalTextRange.span(
          startOffset = linkOpeningMarkerRange.end,
          length = linkClosingMarker.startOffset - linkOpeningMarker.endOffset,
        )
        val urlClosingMarkerRange = LocalTextRange(
          startOffset = urlRange.end,
          endOffset = chars.length,
        )

        LinkNode(
          range = LocalTextRange(0, chars.length),
          textRange = textRange,
          textOpeningMarkerRange = textOpeningMarkerRange,
          textClosingMarkerRange = textClosingMarkerRange,
          urlRange = urlRange,
          urlOpeningMarkerRange = linkOpeningMarkerRange,
          urlClosingMarkerRange = urlClosingMarkerRange,
        )
      }
      is Code -> {
        InlineCodeNode(
          range = LocalTextRange.span(0, chars.length),
          openingMarkerRange = LocalTextRange.span(0, openingMarker.length),
          closingMarkerRange = LocalTextRange(chars.length - closingMarker.length, chars.length),
        )
      }
      is FencedCodeBlock -> {
        if (openingMarker.contains('`') && !closingMarker.isEmpty()) {
          FencedCodeBlockNode(
            range = LocalTextRange.span(0, chars.length),
            openingMarkerRange = LocalTextRange.span(0, openingMarker.length),
            closingMarkerRange = LocalTextRange(textLength - closingMarker.length, textLength),
          )
        } else return null
      }
      is BlockQuote -> {
        val trimmedEnd = lazyContinuationTrimmedEnd()
        val rangeEnd = if (trimmedEnd < chars.length) {
          // The blockquote ends with a lazy continuation that lacks `>`. Stop the range
          // at the trim point so that line falls outside the quote's leading padding.
          trimmedEnd
        } else {
          chars.length - chars.countTrailing(CharPredicate.anyOf('\n'))
        }
        BlockQuoteNode(
          range = LocalTextRange.span(0, rangeEnd),
          markerRange = LocalTextRange.span(0, openingMarker.length),
        )
      }
      is ListBlock -> {
        val lastItem = lastChild as ListItem
        val lastItemTrimmedEnd = lastItem.lazyContinuationTrimmedEnd()
        val rangeEnd = if (lastItemTrimmedEnd < lastItem.chars.length) {
          // The last item ends with a lazy continuation line. Stop the block at the trim
          // point so the unindented continuation falls outside the list block's leading
          // padding, matching what most non-CommonMark editors render.
          lastItem.chars.startOffset + lastItemTrimmedEnd - chars.startOffset
        } else {
          // Workaround for https://github.com/vsch/flexmark-java/issues/519. Flexmark drops
          // trailing spaces from an empty item's `chars`, so add them back explicitly. The
          // `+1` over-extends the range by one phantom character so that a keystroke at the
          // cursor (which sits at end of text) gets absorbed by the overlay's range rebasing,
          // keeping the new character inside the list paragraph until the reparse arrives.
          // Skip the phantom char when a newline already follows, otherwise it gobbles the
          // newline into the list.
          val tail = baseSequence.subSequence(lastItem.chars.endOffset)
          val ignoredTrailingSpaces = tail.countLeadingSpace()
            .let { if (it > 0 && tail.length == it) it + 1 else it }
          val trailingNewlines = chars.countTrailing(CharPredicate.anyOf('\n'))
          chars.length + ignoredTrailingSpaces - trailingNewlines
        }
        ListBlockNode(
          range = LocalTextRange.span(0, rangeEnd),
          children = this.walkSubtree { it.toWysiwygMarkdownNode() },
        )
      }
      is TaskListItem -> {
        if (openingMarker.first() in unorderedItemMarkers) {
          val content = firstChild?.chars
          val contentStartOffset = content?.startOffset ?: markerSuffix.endOffset
          val contentEndOffset = content?.endOffset ?: contentStartOffset
          val markerEndOffset = markerSuffix.endOffset +
            baseSequence.subSequence(markerSuffix.endOffset).countLeadingSpace().coerceAtMost(1)
          TaskListItemNode(
            range = LocalTextRange.span(0, lazyContinuationTrimmedEnd()),
            markerRange = LocalTextRange(
              startOffset = 0,
              endOffset = markerEndOffset - chars.startOffset,
            ),
            checkboxRange = LocalTextRange(
              startOffset = markerSuffix.startOffset - chars.startOffset,
              endOffset = markerSuffix.endOffset - chars.startOffset,
            ),
            contentRange = LocalTextRange(
              startOffset = contentStartOffset - chars.startOffset,
              endOffset = contentEndOffset - chars.startOffset,
            ),
            isChecked = isItemDoneMarker,
            content = this.walkSubtree { it.toWysiwygMarkdownNode() },
          )
        } else {
          toListItemNode()
        }
      }
      is ListItem -> toListItemNode()
      is Heading -> {
        if (isAtxHeading && text.isNotBlank) {
          HeadingNode(
            range = LocalTextRange.span(0, chars.length),
            openingMarkerRange = LocalTextRange.span(0, text.startOffset - chars.startOffset),
            level = level,
          )
        } else {
          // Unreachable in theory: setext headings are disabled at the parser via
          // HEADING_SETEXT_MARKER_LENGTH, and ATX headings always carry non-blank text
          // because the ATX regex requires content after the `#`s. Kept as a defensive no-op.
          return null
        }
      }
      is ThematicBreak -> {
        ThematicBreakNode(
          range = LocalTextRange.span(0, chars.length),
        )
      }
      else -> null
    }
  }

  private fun ListItem.toListItemNode(): ListItemNode {
    val markerEndOffset = openingMarker.endOffset +
        baseSequence.subSequence(openingMarker.endOffset).countLeadingSpace().coerceAtMost(1)
    return ListItemNode(
      range = LocalTextRange.span(0, lazyContinuationTrimmedEnd()),
      markerRange = LocalTextRange(
        startOffset = 0,
        endOffset = markerEndOffset - chars.startOffset,
      ),
      content = this.walkSubtree { it.toWysiwygMarkdownNode() },
    )
  }

  private companion object {
    private const val unorderedItemMarkers = "-+*"
  }
}

private fun ListItem.lazyContinuationTrimmedEnd(): Int {
  val text = chars
  val contentColumn = openingMarker.length + 1
  return text.endBeforeLazyContinuation { lineStart, lineEnd ->
    var leadingSpaces = 0
    while (lineStart + leadingSpaces < lineEnd && text[lineStart + leadingSpaces] == ' ') {
      leadingSpaces++
    }
    leadingSpaces >= contentColumn
  }
}

private fun BlockQuote.lazyContinuationTrimmedEnd(): Int {
  val text = chars
  return text.endBeforeLazyContinuation { lineStart, lineEnd ->
    var probe = lineStart
    // CommonMark allows 0-3 spaces of indent before `>`.
    while (probe < lineEnd && probe - lineStart < 4 && text[probe] == ' ') {
      probe++
    }
    probe < lineEnd && text[probe] == '>'
  }
}

/**
 * Returns the offset within this text after the last line where [isProperContinuation] holds.
 *
 * CommonMark folds an unprefixed/unindented line below a list item or blockquote into
 * the containing block as a lazy continuation. Visually that line looks like it isn't
 * part of the block (no marker, no indent), so we trim the range there and let the
 * bytes past the trim render as plain text outside the block's leading padding. A
 * blank line already terminates both blocks, so anything past it is left alone.
 */
private inline fun CharSequence.endBeforeLazyContinuation(
  isProperContinuation: (lineStart: Int, lineEnd: Int) -> Boolean,
): Int {
  val firstNewline = indexOf('\n')
  if (firstNewline < 0) return length

  var cursor = firstNewline + 1
  while (cursor < length) {
    val nextNewline = indexOf('\n', cursor).let { if (it < 0) length else it }
    if (nextNewline == cursor) {
      // Blank line. CommonMark already terminates the block here, so anything past is
      // a separate block, not a lazy continuation we need to trim.
      break
    }
    if (!isProperContinuation(cursor, nextNewline)) {
      // Drop the lazy line along with the `\n` that ties it to the previous line.
      return cursor - 1
    }
    cursor = nextNewline + 1
  }
  return length
}

/**
 * Iterative rather than recursive because parsing runs on every keystroke,
 * so squeeze every bit of perf out.
 *
 * @param map A non-null result claims that subtree, so descendants won't be visited separately.
 */
private inline fun FlexmarkNode.walkSubtree(
  map: (FlexmarkNode) -> MarkdownNode?,
): List<MarkdownChildNode> {
  var current: FlexmarkNode = firstChild ?: return emptyList()
  val out = mutableListOf<MarkdownChildNode>()
  while (true) {
    // Wysiwyg keeps it simple and avoids some nested markdown blocks.
    // For example, nested bold/italic styling inside code blocks feels overkill.
    val isNestedSyntax = (current is ListBlock && current.parent is ListItem)
        || current.parent is BlockQuote
        || current.parent is FencedCodeBlock

    // `next` doubles as a descent signal: non-null: walk into it next;
    // null: we're done with `current` and need to find a sibling.
    var next: FlexmarkNode? = null
    if (!isNestedSyntax) {
      val mapped = map(current)
      if (mapped != null) {
        out += MarkdownChildNode(
          offsetInParent = current.startOffset - this.startOffset,
          node = mapped,
        )
      } else {
        next = current.firstChild
      }
    }
    if (next == null) {
      // No descent: move to the next sibling, climbing ancestors until we
      // find one with a sibling, or until we'd cross back over `root`.
      next = current.next
      while (next == null) {
        val parent = current.parent
        if (parent == null || parent === this) {
          return out
        }
        current = parent
        next = current.next
      }
    }
    current = next
  }
}
