package me.saket.wysiwyg.parser

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
  fun Node.addSpansInto(buffer: MutableList<MarkdownSpan>)
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

}
