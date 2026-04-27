package me.saket.wysiwyg.highlight.flexmark

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
        totalLength = text.length,
        children = parser.parse(text).walkSubtree {
          it.toWysiwygMarkdownNode(parentStartOffset = 0)
        },
      )
    }
  }

  private fun FlexmarkNode.toWysiwygMarkdownNode(parentStartOffset: Int): WysiwygMarkdownNode? {
    val offsetInParent = startOffset - parentStartOffset
    return when (this) {
      is Emphasis -> {
        ItalicNode(
          offsetInParent = offsetInParent,
          totalLength = chars.length,
          openingMarkerLength = openingMarker.length,
          closingMarkerLength = closingMarker.length,
        )
      }
      is StrongEmphasis -> {
        BoldNode(
          offsetInParent = offsetInParent,
          totalLength = chars.length,
          openingMarkerLength = openingMarker.length,
          closingMarkerLength = closingMarker.length,
        )
      }
      is Strikethrough -> {
        StrikeThroughNode(
          offsetInParent = offsetInParent,
          totalLength = chars.length,
        )
      }
      is Link -> {
        LinkNode(
          offsetInParent = offsetInParent,
          totalLength = chars.length,
          textLength = text.length,
          textOpeningMarkerLength = textOpeningMarker.length,
          textClosingMarkerLength = textClosingMarker.length,
          urlLength = linkClosingMarker.startOffset - linkOpeningMarker.endOffset,
          linkOpeningMarkerLength = linkOpeningMarker.length,
        )
      }
      is Code -> {
        InlineCodeNode(
          offsetInParent = offsetInParent,
          totalLength = textLength,
          openingMarkerLength = openingMarker.length,
          closingMarkerLength = closingMarker.length,
        )
      }
      is FencedCodeBlock -> {
        if (openingMarker.contains('`') && !closingMarker.isEmpty()) {
          FencedCodeBlockNode(
            offsetInParent = offsetInParent,
            totalLength = chars.length,
            openingMarkerLength = openingMarker.length,
            closingMarkerLength = closingMarker.length,
          )
        } else return null
      }
      is BlockQuote -> {
        BlockQuoteNode(
          offsetInParent = offsetInParent,
          totalLength = chars.length - chars.countTrailing(CharPredicate.anyOf('\n')),
          markerLength = openingMarker.length,
        )
      }
      is ListBlock -> {
        // Workaround for https://github.com/vsch/flexmark-java/issues/519.
        val lastItem = lastChild as ListItem
        val wereSpacesIgnored = lastItem.chars == lastItem.openingMarker
        val ignoredTrailingSpaces = if (wereSpacesIgnored) {
          baseSequence.subSequence(lastItem.openingMarker.endOffset).countLeadingSpace() + 1
        } else {
          0
        }
        ListBlockNode(
          offsetInParent = offsetInParent,
          totalLength = chars.length + ignoredTrailingSpaces,
          children = this.walkSubtree {
            it.toWysiwygMarkdownNode(parentStartOffset = startOffset)
          }
        )
      }
      is ListItem -> {
        ListItemNode(
          offsetInParent = offsetInParent,
          totalLength = chars.length,
          markerLength = openingMarker.length,
          children = this.walkSubtree {
            it.toWysiwygMarkdownNode(parentStartOffset = startOffset)
          },
        )
      }
      is Heading -> {
        if (isAtxHeading && text.isNotBlank) {
          HeadingNode(
            offsetInParent = offsetInParent,
            totalLength = chars.length,
            openingMarkerLength = openingMarker.length,
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
          offsetInParent = offsetInParent,
          totalLength = chars.length,
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
