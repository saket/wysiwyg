package me.saket.wysiwyg.parser

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.util.fastAny
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
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.misc.CharPredicate
import com.vladsch.flexmark.util.sequence.BasedSequence
import me.saket.wysiwyg.SpanTextRange
import me.saket.wysiwyg.internal.fastForEach
import me.saket.wysiwyg.internal.fastForEachReverseIndexed
import me.saket.wysiwyg.parser.MarkdownParser.ParseResult
import com.vladsch.flexmark.parser.Parser as FlexmarkParser

interface FlexmarkMarkdownParserExtension {
  /**
   * Flexmark extensions or post-processor factories can be registered
   * here for parsing text and inserting custom nodes to the AST.
   */
  fun buildParser(builder: FlexmarkParser.Builder)

  /**
   * Once an AST is generated, this function is called for each markdown
   * node in the tree to create markdown spans for them.
   */
  fun Node.addSpansInto(spans: MutableList<MarkdownSpan>)

  /**
   * Return `true` if [style] is one this extension emits and carries an explicit closing marker
   * (e.g. the trailing `!<` of a Reddit spoiler).
   *
   * Used by the flicker-avoidance heuristic inside [FlexmarkMarkdownParser]: when text is deleted at
   * a span's last index, spans with a closing marker are discarded (the marker was likely what got
   * deleted), while spans without one are kept and shrunk. Extensions only need to override this if
   * they want correct behavior in the brief window before the real parser re-parses.
   */
  fun hasClosingMarker(style: MarkdownSpanStyle): Boolean = false
}

class FlexmarkMarkdownParser(
  vararg extensions: FlexmarkMarkdownParserExtension,
) : MarkdownParser {
  private val extensions = extensions.toList()

  private val parser = FlexmarkParser.builder()
    .apply {
      // Disable parsers for unsupported syntaxes.
      set(FlexmarkParser.HTML_BLOCK_PARSER, false)
      set(FlexmarkParser.INDENTED_CODE_BLOCK_PARSER, false)
      set(FlexmarkParser.REFERENCE_PARAGRAPH_PRE_PROCESSOR, false)

      // List items should start with a space.
      set(FlexmarkParser.LISTS_ITEM_MARKER_SPACE, true)

      // "#" should always be followed by a character to be considered a heading.
      set(FlexmarkParser.HEADING_NO_EMPTY_HEADING_WITHOUT_SPACE, true)
    }
    .extensions(listOf(StrikethroughExtension.create()))
    .apply { extensions.forEach { it.buildParser(this) } }
    .build()

  override fun parse(text: String): ParseResult {
    return ParseResult(
      spans = mutableListOf<MarkdownSpan>().apply {
        parser.parse(text).traverse { node ->
          node.addSpansInto(this)
          extensions.fastForEach {
            it.run { node.addSpansInto(this@apply) }
          }
        }
      }
    )
  }

  private fun Node.addSpansInto(buffer: MutableList<MarkdownSpan>) {
    when (this) {
      is Emphasis -> {
        buffer.addMarkerSpan(openingMarker)
        buffer.addMarkerSpan(closingMarker)
        buffer.add(
          MarkdownSpan(
            style = ItalicSpanStyle,
            range = SpanTextRange(startOffset, endOffset)
          )
        )
      }
      is StrongEmphasis -> {
        buffer.addMarkerSpan(openingMarker)
        buffer.addMarkerSpan(closingMarker)
        buffer.add(
          MarkdownSpan(
            style = BoldSpanStyle,
            range = SpanTextRange(startOffset, endOffset)
          )
        )
      }
      is Strikethrough -> {
        buffer.add(
          MarkdownSpan(
            style = StrikeThroughSpanStyle,
            range = SpanTextRange(startOffset, endOffset)
          )
        )
      }
      is Link -> {
        buffer.addMarkerSpan(textOpeningMarker)
        buffer.addMarkerSpan(textClosingMarker)
        buffer.add(
          MarkdownSpan(
            style = LinkTextSpanStyle,
            range = SpanTextRange(textOpeningMarker.endOffset, textClosingMarker.startOffset)
          )
        )
        buffer.add(
          MarkdownSpan(
            style = LinkUrlSpanStyle,
            range = SpanTextRange(linkOpeningMarker.startOffset, linkClosingMarker.endOffset)
          )
        )
      }
      is Code -> {
        buffer.addMarkerSpan(openingMarker)
        buffer.addMarkerSpan(closingMarker)
        buffer.add(
          MarkdownSpan(
            style = InlineCodeSpanStyle,
            range = SpanTextRange(startOffset, endOffset)
          )
        )
      }
      is FencedCodeBlock -> {
        if (openingMarker.contains('`') && closingMarker.isNotEmpty) {
          buffer.addMarkerSpan(openingMarker)
          buffer.addMarkerSpan(closingMarker)
          buffer.add(
            MarkdownSpan(
              style = FencedCodeBlockSpanStyle,
              range = SpanTextRange(startOffset, closingMarker.endOffset)
            )
          )
        }
      }
      is BlockQuote -> {
        buffer.add(
          MarkdownSpan(
            style = BlockQuoteBodySpanStyle,
            range = SpanTextRange(startOffset, endOffset)
          )
        )
        val withEndingLineBreaksTrimmed = chars.countTrailing(CharPredicate.anyOf('\n'))
        buffer.add(
          MarkdownSpan(
            style = BlockQuoteParagraphLineSpanStyle,
            range = SpanTextRange(startOffset, endOffset - withEndingLineBreaksTrimmed)
          )
        )
        buffer.addMarkerSpan(openingMarker)
      }
      is ListBlock -> {
        // Workaround for https://github.com/vsch/flexmark-java/issues/519.
        val lastChild = lastChild as ListItem
        val wereSpacesIgnored = lastChild.chars == lastChild.openingMarker
        val correctEndOffset = if (wereSpacesIgnored) {
          endOffset + baseSequence.subSequence(lastChild.openingMarker.endOffset).countLeadingSpace() + 1
        } else {
          endOffset
        }
        buffer.add(
          MarkdownSpan(
            style = ListBlockSpanStyle,
            range = SpanTextRange(startOffset, correctEndOffset)
          )
        )
      }
      is ListItem -> {
        buffer.addMarkerSpan(openingMarker)
      }
      is Heading -> {
        // Setext headings aren't supported. They use underlines using "=" for H1 or "-" for H2.
        //
        // This is an H1
        // =============
        //
        // This is an H2
        // -------------
        if (isAtxHeading) {
          buffer.add(
            MarkdownSpan(
              style = HeadingSpanStyle(level),
              range = SpanTextRange(startOffset, endOffset)
            )
          )
          buffer.addMarkerSpan(openingMarker)
        }
      }
      is ThematicBreak -> {
        buffer.add(
          MarkdownSpan(
            style = ThematicBreakSpanStyle,
            range = SpanTextRange(startOffset, endOffset)
          )
        )
        buffer.add(
          MarkdownSpan(
            style = MarkerColorSpanStyle,
            range = SpanTextRange(startOffset, endOffset)
          )
        )
      }
      else -> Unit
    }
  }

  private fun MutableList<MarkdownSpan>.addMarkerSpan(sequence: BasedSequence) {
    if (sequence.isNotEmpty) {
      add(
        MarkdownSpan(
          style = MarkerColorSpanStyle,
          range = SpanTextRange(sequence.startOffset, sequence.endOffset)
        )
      )
    }
  }

  private inline fun Node.traverse(action: (Node) -> Unit) {
    val stack = ArrayDeque<Node>()
    stack.add(this@traverse)

    var next: Node
    while (stack.isNotEmpty()) {
      next = stack.removeFirst()

      // Wysiwyg keeps it simple and avoids nested markdown blocks.
      // For example, nested block/italic styling inside headings feels overkill.
      val isNestedSyntax = (next is ListBlock && next.parent is ListItem)
        || next.parent is BlockQuote
        || next.parent is FencedCodeBlock

      if (!isNestedSyntax) {
        action(next)
        next.children.reversed().forEach { child ->
          stack.addFirst(child)
        }
      }
    }
  }

  /**
   * When text is changed, Compose emits a new text value and discards any previously generated span styles.
   * Parsing this new text value takes at least a few milliseconds, enough for the user to see markdown styling
   * flicker on every keystroke.
   *
   * To prevent this, Wysiwyg retains the span styles generated for the previous text value and immediately
   * re-applies them. This function manually adjusts their spans to account for text that may have changed
   * between their bounds. For example, if a letter was inserted within a bold span, this function shifts all
   * spans after the inserted index by one position.
   *
   * The implementation does not need to be perfect because the new text will be re-parsed in a few milliseconds.
   * It just needs to be good enough to give an illusion that parsing is happening instantly on every keystroke.
   */
  internal fun offsetSpansOnTextChange(
    newValue: TextFieldValue,
    previousValue: TextFieldValue,
    previousSpans: List<MarkdownSpan>
  ): List<MarkdownSpan> {
    val offsetShift = newValue.text.length - previousValue.text.length
    if (offsetShift == 0) {
      return previousSpans
    }

    val previousSpans = previousSpans.toMutableList()
    val offsetShiftStartsFrom = previousValue.selection.max

    val wasTextSelected = previousValue.selection.length > 0
    if (wasTextSelected) {
      // Some characters were deleted or replaced.
      // Remove spans within the affected text range.
      previousSpans.fastForEachReverseIndexed { index, span ->
        if (span.range.startIndex in previousValue.selection
          || (span.style.hasClosingMarker() && span.range.endIndexInclusive in previousValue.selection)
        ) {
          previousSpans.removeAt(index)
        }
      }
    }

    if (!wasTextSelected && offsetShift == -1) {
      // Back-press detected! Remove any span whose opening/closing marker was deleted.
      previousSpans.fastForEachReverseIndexed { index, span ->
        val newCursorAt = newValue.selection.min
        if (span.range.startIndex == newCursorAt
          || (span.style.hasClosingMarker() && span.range.endIndexInclusive == newCursorAt)
        ) {
          previousSpans.removeAt(index)
        }
      }
    }

    // todo: when enter is pressed at the end of a paragraph span, do not extend the span maybe? or maybe only do this for thematic break.

    // Expand spans to include text that was inserted within their range.
    previousSpans.fastForEachReverseIndexed { index, span ->
      val isStartAffected = span.range.startIndex >= offsetShiftStartsFrom
      val isEndAffected = span.range.endIndexExclusive >= offsetShiftStartsFrom

      if (isStartAffected || isEndAffected) {
        val adjusted = span.copy(
          range = SpanTextRange(
            startIndex = when {
              isStartAffected -> span.range.startIndex + offsetShift
              else -> span.range.startIndex
            },
            endIndexExclusive = when {
              isEndAffected -> span.range.endIndexExclusive + offsetShift
              else -> span.range.endIndexExclusive
            }
          )
        )
        previousSpans.removeAt(index)
        previousSpans.add(index, adjusted)
      }
    }

    return previousSpans
  }

  /**
   * Knowledge about which built-in styles carry a closing marker stays local to this parser rather
   * than leaking into the shared [MarkdownSpanStyle] type. Extensions opt in via
   * [FlexmarkMarkdownParserExtension.hasClosingMarker].
   */
  private fun MarkdownSpanStyle.hasClosingMarker(): Boolean {
    return when (this) {
      MarkerColorSpanStyle,
      BoldSpanStyle,
      ItalicSpanStyle,
      StrikeThroughSpanStyle,
      LinkTextSpanStyle,
      LinkUrlSpanStyle,
      InlineCodeSpanStyle,
      FencedCodeBlockSpanStyle,
      ThematicBreakSpanStyle -> true

      BlockQuoteBodySpanStyle,
      BlockQuoteParagraphLineSpanStyle,
      ListBlockSpanStyle -> false

      is HeadingSpanStyle -> false

      else -> extensions.fastAny { it.hasClosingMarker(this@hasClosingMarker) }
    }
  }
}
