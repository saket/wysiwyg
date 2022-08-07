package me.saket.wysiwyg.parser

import androidx.compose.ui.text.input.TextFieldValue
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
import com.vladsch.flexmark.util.ast.DelimitedNode
import com.vladsch.flexmark.util.ast.Node
import me.saket.wysiwyg.internal.fastForEachReverseIndexed
import me.saket.wysiwyg.parser.MarkdownParser.ParseResult
import com.vladsch.flexmark.parser.Parser as FlexmarkParser
import com.vladsch.flexmark.util.misc.Extension as FlexmarkExtension

internal class FlexmarkMarkdownParser : MarkdownParser {
  private val parser = FlexmarkParser.builder()
    .extensions(listOf<FlexmarkExtension>(StrikethroughExtension.create()))
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
    .build()

  override fun parse(text: String): ParseResult {
    return ParseResult(
      spans = mutableListOf<MarkdownSpan>().apply {
        parser.parse(text).traverse { node ->
          node.addSpansInto(this)
        }
      }
    )
  }

  private fun Node.addSpansInto(buffer: MutableList<MarkdownSpan>) {
    when (this) {
      is Emphasis -> {
        buffer.addSyntaxSpans(this)
        buffer.add(
          MarkdownSpan(
            token = MarkdownSpanToken.Italic,
            startIndex = startOffset,
            endIndexExclusive = endOffset
          )
        )
      }
      is StrongEmphasis -> {
        buffer.addSyntaxSpans(this)
        buffer.add(
          MarkdownSpan(
            token = MarkdownSpanToken.Bold,
            startIndex = startOffset,
            endIndexExclusive = endOffset
          )
        )
      }
      is Strikethrough -> {
        buffer.add(
          MarkdownSpan(
            token = MarkdownSpanToken.StrikeThrough,
            startIndex = startOffset,
            endIndexExclusive = endOffset
          )
        )
      }
      is Link -> {
        buffer.add(
          MarkdownSpan(
            token = MarkdownSpanToken.SyntaxColor,
            startIndex = textOpeningMarker.startOffset,
            endIndexExclusive = textOpeningMarker.endOffset
          )
        )
        buffer.add(
          MarkdownSpan(
            token = MarkdownSpanToken.LinkText,
            startIndex = textOpeningMarker.endOffset,
            endIndexExclusive = textClosingMarker.endOffset
          )
        )
        buffer.add(
          MarkdownSpan(
            token = MarkdownSpanToken.SyntaxColor,
            startIndex = textClosingMarker.startOffset,
            endIndexExclusive = textClosingMarker.endOffset
          )
        )
        buffer.add(
          MarkdownSpan(
            token = MarkdownSpanToken.LinkUrl,
            startIndex = url.startOffset - 1,
            endIndexExclusive = url.endOffset + 1
          )
        )
      }
      is Code -> {
        buffer.addSyntaxSpans(this)
        buffer.add(
          MarkdownSpan(
            token = MarkdownSpanToken.InlineCode,
            startIndex = startOffset,
            endIndexExclusive = endOffset
          )
        )
      }
      is FencedCodeBlock -> {
        if (openingMarker.contains('`') && closingMarker.isNotEmpty) {
          buffer.add(
            MarkdownSpan(
              token = MarkdownSpanToken.SyntaxColor,
              startIndex = startOffset,
              endIndexExclusive = openingMarker.endOffset
            )
          )
          buffer.add(
            MarkdownSpan(
              token = MarkdownSpanToken.SyntaxColor,
              startIndex = endOffset - (closingMarker.length),
              endIndexExclusive = endOffset,
            )
          )
          buffer.add(
            MarkdownSpan(
              token = MarkdownSpanToken.FencedCodeBlock,
              startIndex = startOffset,
              endIndexExclusive = closingMarker.endOffset
            )
          )
        }
      }
      is BlockQuote -> {
        buffer.add(
          MarkdownSpan(
            token = MarkdownSpanToken.BlockQuote,
            startIndex = startOffset,
            endIndexExclusive = endOffset
          )
        )
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
            token = MarkdownSpanToken.ListBlock,
            startIndex = startOffset,
            endIndexExclusive = correctEndOffset
          )
        )
      }
      is ListItem -> {
        buffer.add(
          MarkdownSpan(
            token = MarkdownSpanToken.SyntaxColor,
            startIndex = startOffset,
            endIndexExclusive = openingMarker.endOffset
          )
        )
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
              token = MarkdownSpanToken.SyntaxColor,
              startIndex = startOffset,
              endIndexExclusive = openingMarker.endOffset
            )
          )
          buffer.add(
            MarkdownSpan(
              token = MarkdownSpanToken.Heading(level),
              startIndex = startOffset,
              endIndexExclusive = endOffset,
            )
          )
        }
      }
      is ThematicBreak -> {
        buffer.add(
          MarkdownSpan(
            token = MarkdownSpanToken.SyntaxColor,
            startIndex = startOffset,
            endIndexExclusive = endOffset
          )
        )
      }
      else -> Unit
    }
  }

  private fun <T> MutableList<MarkdownSpan>.addSyntaxSpans(node: T) where T : Node, T : DelimitedNode {
    if (node.openingMarker.isNotEmpty) {
      add(
        MarkdownSpan(
          token = MarkdownSpanToken.SyntaxColor,
          startIndex = node.startOffset,
          endIndexExclusive = node.startOffset + node.openingMarker.length
        )
      )
    }

    if (node.closingMarker.isNotEmpty) {
      add(
        MarkdownSpan(
          token = MarkdownSpanToken.SyntaxColor,
          startIndex = node.endOffset - (node.closingMarker.length),
          endIndexExclusive = node.endOffset
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

      val isNestedSyntax = (next is ListBlock && next.parent is ListItem)
        || next.parent is BlockQuote
        || next.parent is Heading
        || next.parent is FencedCodeBlock

      if (!isNestedSyntax) {
        action(next)
        next.children.reversed().forEach { child ->
          stack.addFirst(child)
        }
      }
    }
  }

  override fun offsetSpansOnTextChange(
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
        if (span.startIndex in previousValue.selection
          || (span.hasClosingMarker && span.endIndexInclusive in previousValue.selection)
        ) {
          previousSpans.removeAt(index)
        }
      }
    }

    if (!wasTextSelected && offsetShift == -1) {
      // Back-press detected! Remove any span whose opening/closing marker was deleted.
      previousSpans.fastForEachReverseIndexed { index, span ->
        val newCursorAt = newValue.selection.min
        if (span.startIndex == newCursorAt || (span.hasClosingMarker && span.endIndexInclusive == newCursorAt)) {
          previousSpans.removeAt(index)
        }
      }
    }

    previousSpans.fastForEachReverseIndexed { index, span ->
      val isStartAffected = span.startIndex >= offsetShiftStartsFrom
      val isEndAffected = span.endIndexExclusive >= offsetShiftStartsFrom

      if (isStartAffected || isEndAffected) {
        val adjusted = span.copy(
          startIndex = if (isStartAffected) span.startIndex + offsetShift else span.startIndex,
          endIndexExclusive = if (isEndAffected) span.endIndexExclusive + offsetShift else span.endIndexExclusive
        )
        previousSpans.removeAt(index)
        previousSpans.add(index, adjusted)
      }
    }

    return previousSpans
  }
}

private val MarkdownSpan.hasClosingMarker
  get() = when (token) {
    MarkdownSpanToken.SyntaxColor,
    MarkdownSpanToken.Bold,
    MarkdownSpanToken.Italic,
    MarkdownSpanToken.StrikeThrough,
    MarkdownSpanToken.LinkText,
    MarkdownSpanToken.LinkUrl,
    MarkdownSpanToken.InlineCode -> true
    MarkdownSpanToken.FencedCodeBlock,
    MarkdownSpanToken.BlockQuote,
    MarkdownSpanToken.ListBlock,
    is MarkdownSpanToken.Heading -> false
  }
