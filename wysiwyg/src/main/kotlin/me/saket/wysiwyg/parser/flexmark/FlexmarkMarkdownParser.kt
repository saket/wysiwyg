package me.saket.wysiwyg.parser.flexmark

import androidx.compose.ui.util.fastForEach
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import me.saket.wysiwyg.BlockQuoteBodySpanStyle
import me.saket.wysiwyg.BlockQuoteParagraphLineSpanStyle
import me.saket.wysiwyg.BoldSpanStyle
import me.saket.wysiwyg.FencedCodeBlockSpanStyle
import me.saket.wysiwyg.HeadingSpanStyle
import me.saket.wysiwyg.InlineCodeSpanStyle
import me.saket.wysiwyg.ItalicSpanStyle
import me.saket.wysiwyg.LinkTextSpanStyle
import me.saket.wysiwyg.LinkUrlSpanStyle
import me.saket.wysiwyg.ListBlockSpanStyle
import me.saket.wysiwyg.MarkdownSpan
import me.saket.wysiwyg.MarkerColorSpanStyle
import me.saket.wysiwyg.MarkdownSpanTextRange
import me.saket.wysiwyg.StrikeThroughSpanStyle
import me.saket.wysiwyg.ThematicBreakSpanStyle
import me.saket.wysiwyg.parser.ChangeListSnapshot
import me.saket.wysiwyg.parser.MarkdownParser
import me.saket.wysiwyg.parser.MarkdownParser.ParseResult
import com.vladsch.flexmark.parser.Parser as FlexmarkParser

/**
 * Backed by [flexmark-java](https://github.com/vsch/flexmark-java). Not incremental — every
 * edit triggers a full re-parse.
 *
 * An incremental tree-sitter implementation was tried and removed (see git history). On a
 * Pixel 10 Pro, flexmark cold-parse was 2.3–3.7× faster across 100/1k/5k-line fixtures
 * (5.2/15.2/69.6 ms vs 11.8/50.5/259.1 ms), and tree-sitter's "hot" path failed to beat its
 * own cold path at 1k+ lines because span emission still walked the full tree — so the
 * native tax (JNI, UTF-16 offset conversion, grammar quirks) bought nothing.
 */
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

  override fun parse(text: String, changes: ChangeListSnapshot): Flow<ParseResult> {
    return flow {
      // Flexmark is not incremental, so [changes] is ignored and the flow emits exactly once.
      val buffer = mutableListOf<MarkdownSpan>()
      withContext(Dispatchers.Default) {
        parser.parse(text).traverse { node ->
          node.addSpansInto(buffer)
          extensions.fastForEach { extension ->
            with(extension) {
              node.addSpansInto(buffer)
            }
          }
        }
      }
      emit(ParseResult(spans = buffer))
    }
  }

  private fun Node.addSpansInto(buffer: MutableList<MarkdownSpan>) {
    when (this) {
      is Emphasis -> {
        buffer.addMarkerSpan(openingMarker)
        buffer.addMarkerSpan(closingMarker)
        buffer.add(
          MarkdownSpan(
            style = ItalicSpanStyle,
            range = MarkdownSpanTextRange(startOffset, endOffset)
          )
        )
      }
      is StrongEmphasis -> {
        buffer.addMarkerSpan(openingMarker)
        buffer.addMarkerSpan(closingMarker)
        buffer.add(
          MarkdownSpan(
            style = BoldSpanStyle,
            range = MarkdownSpanTextRange(startOffset, endOffset)
          )
        )
      }
      is Strikethrough -> {
        buffer.add(
          MarkdownSpan(
            style = StrikeThroughSpanStyle,
            range = MarkdownSpanTextRange(startOffset, endOffset)
          )
        )
      }
      is Link -> {
        buffer.addMarkerSpan(textOpeningMarker)
        buffer.addMarkerSpan(textClosingMarker)
        buffer.add(
          MarkdownSpan(
            style = LinkTextSpanStyle,
            range = MarkdownSpanTextRange(textOpeningMarker.endOffset, textClosingMarker.startOffset)
          )
        )
        buffer.add(
          MarkdownSpan(
            style = LinkUrlSpanStyle,
            range = MarkdownSpanTextRange(linkOpeningMarker.startOffset, linkClosingMarker.endOffset)
          )
        )
      }
      is Code -> {
        buffer.addMarkerSpan(openingMarker)
        buffer.addMarkerSpan(closingMarker)
        buffer.add(
          MarkdownSpan(
            style = InlineCodeSpanStyle,
            range = MarkdownSpanTextRange(startOffset, endOffset)
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
              range = MarkdownSpanTextRange(startOffset, closingMarker.endOffset)
            )
          )
        }
      }
      is BlockQuote -> {
        buffer.add(
          MarkdownSpan(
            style = BlockQuoteBodySpanStyle,
            range = MarkdownSpanTextRange(startOffset, endOffset)
          )
        )
        val withEndingLineBreaksTrimmed = chars.countTrailing(CharPredicate.anyOf('\n'))
        buffer.add(
          MarkdownSpan(
            style = BlockQuoteParagraphLineSpanStyle,
            range = MarkdownSpanTextRange(startOffset, endOffset - withEndingLineBreaksTrimmed)
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
            range = MarkdownSpanTextRange(startOffset, correctEndOffset)
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
              range = MarkdownSpanTextRange(startOffset, endOffset)
            )
          )
          buffer.addMarkerSpan(openingMarker)
        }
      }
      is ThematicBreak -> {
        buffer.add(
          MarkdownSpan(
            style = ThematicBreakSpanStyle,
            range = MarkdownSpanTextRange(startOffset, endOffset)
          )
        )
        buffer.add(
          MarkdownSpan(
            style = MarkerColorSpanStyle,
            range = MarkdownSpanTextRange(startOffset, endOffset)
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
          range = MarkdownSpanTextRange(sequence.startOffset, sequence.endOffset)
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
