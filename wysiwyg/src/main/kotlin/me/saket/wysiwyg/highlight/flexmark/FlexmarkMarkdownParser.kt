package me.saket.wysiwyg.highlight.flexmark

import androidx.compose.ui.text.TextRange
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
import com.vladsch.flexmark.util.misc.CharPredicate
import com.vladsch.flexmark.util.sequence.BasedSequence
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.wysiwyg.highlight.BlockQuoteNode
import me.saket.wysiwyg.highlight.BoldNode
import me.saket.wysiwyg.highlight.ChangeListSnapshot
import me.saket.wysiwyg.highlight.FencedCodeBlockNode
import me.saket.wysiwyg.highlight.HeadingNode
import me.saket.wysiwyg.highlight.InlineCodeNode
import me.saket.wysiwyg.highlight.ItalicNode
import me.saket.wysiwyg.highlight.LinkNode
import me.saket.wysiwyg.highlight.ListBlockNode
import me.saket.wysiwyg.highlight.ListItemNode
import me.saket.wysiwyg.highlight.MarkdownDocument
import me.saket.wysiwyg.highlight.MarkdownParser
import me.saket.wysiwyg.highlight.StrikeThroughNode
import me.saket.wysiwyg.highlight.ThematicBreakNode
import com.vladsch.flexmark.parser.Parser as FlexmarkParser
import com.vladsch.flexmark.util.ast.Node as FlexmarkNode
import me.saket.wysiwyg.highlight.MarkdownNode as WysiwygMarkdownNode

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
    }
    .extensions(listOf(StrikethroughExtension.create()))
    .build()

  override suspend fun parse(text: String, changes: ChangeListSnapshot): MarkdownDocument {
    return withContext(dispatcher) {
      MarkdownDocument(
        range = TextRange(0, text.length),
        children = parser.parse(text).walkSubtree {
          it.toWysiwygMarkdownNode()
        },
      )
    }
  }

  private fun FlexmarkNode.toWysiwygMarkdownNode(): WysiwygMarkdownNode? {
    return when (this) {
      is Emphasis -> {
        ItalicNode(
          range = TextRange(startOffset, endOffset),
          openingMarker = openingMarker.range(),
          closingMarker = closingMarker.range(),
        )
      }
      is StrongEmphasis -> {
        BoldNode(
          range = TextRange(startOffset, endOffset),
          openingMarker = openingMarker.range(),
          closingMarker = closingMarker.range(),
        )
      }
      is Strikethrough -> {
        StrikeThroughNode(
          range = TextRange(startOffset, endOffset),
        )
      }
      is Link -> {
        LinkNode(
          range = TextRange(textOpeningMarker.startOffset, linkClosingMarker.endOffset),
          textRange = TextRange(textOpeningMarker.endOffset, textClosingMarker.startOffset),
          urlRange = TextRange(linkOpeningMarker.startOffset, linkClosingMarker.endOffset),
          textOpeningMarker = textOpeningMarker.range(),
          textClosingMarker = textClosingMarker.range(),
          linkOpeningMarker = linkOpeningMarker.range(),
          linkClosingMarker = linkClosingMarker.range(),
        )
      }
      is Code -> {
        InlineCodeNode(
          range = TextRange(startOffset, endOffset),
          openingMarker = openingMarker.range(),
          closingMarker = closingMarker.range(),
        )
      }
      is FencedCodeBlock -> {
        if (!openingMarker.contains('`') || closingMarker.isEmpty()) return null
        FencedCodeBlockNode(
          range = TextRange(startOffset, closingMarker.endOffset),
          openingMarker = openingMarker.range(),
          closingMarker = closingMarker.range(),
        )
      }
      is BlockQuote -> {
        val trimmedEnd = endOffset - chars.countTrailing(CharPredicate.anyOf('\n'))
        BlockQuoteNode(
          range = TextRange(startOffset, endOffset),
          paragraphRange = TextRange(startOffset, trimmedEnd),
          openingMarker = openingMarker.range(),
        )
      }
      is ListBlock -> {
        // Workaround for https://github.com/vsch/flexmark-java/issues/519.
        val lastChild = lastChild as ListItem
        val wereSpacesIgnored = lastChild.chars == lastChild.openingMarker
        val correctEndOffset = if (wereSpacesIgnored) {
          endOffset + baseSequence
            .subSequence(lastChild.openingMarker.endOffset)
            .countLeadingSpace() + 1
        } else {
          endOffset
        }
        ListBlockNode(
          range = TextRange(this.startOffset, correctEndOffset),
          items = this.walkSubtree { it.toWysiwygMarkdownNode() }
        )
      }
      is ListItem -> {
        ListItemNode(
          range = TextRange(startOffset, endOffset),
          marker = openingMarker.range(),
          children = this.walkSubtree { it.toWysiwygMarkdownNode() },
        )
      }
      is Heading -> {
        if (isAtxHeading && text.isNotBlank) {
          HeadingNode(
            range = TextRange(startOffset, endOffset),
            openingMarker = openingMarker.range(),
            level = level,
          )
        } else {
          // Setext headings aren't supported. They use underlines using "=" for H1 or "-" for H2.
          //
          // This is an H1
          // =============
          //
          // This is an H2
          // -------------
          return null
        }
      }
      is ThematicBreak -> {
        ThematicBreakNode(
          range = TextRange(startOffset, endOffset),
        )
      }
      else -> null
    }
  }
}

/**
 * Iterative rather than recursive because parsing runs on every keystroke,
 * so squeeze every bit of perf out.
 *
 * @param map A non-null result claims that subtree, so descendants won't be visited separately.
 */
private inline fun <R> FlexmarkNode.walkSubtree(map: (FlexmarkNode) -> R?): List<R> {
  val out = mutableListOf<R>()
  val root = this
  var current: FlexmarkNode = firstChild ?: return out
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
        out += mapped
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
        if (parent == null || parent === root) return out
        current = parent
        next = current.next
      }
    }
    current = next
  }
}

private fun BasedSequence.range(): TextRange {
  return TextRange(startOffset, endOffset)
}
