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
        val totalLength = chars.length - chars.countTrailing(CharPredicate.anyOf('\n'))
        BlockQuoteNode(
          range = LocalTextRange.span(0, totalLength),
          markerRange = LocalTextRange.span(0, openingMarker.length),
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
        val trailingNewlines = chars.countTrailing(CharPredicate.anyOf('\n'))
        ListBlockNode(
          range = LocalTextRange.span(0, chars.length + ignoredTrailingSpaces - trailingNewlines),
          children = this.walkSubtree { it.toWysiwygMarkdownNode() },
        )
      }
      is TaskListItem -> {
        val body = firstChild?.chars ?: markerSuffix.subSequence(markerSuffix.length)
        TaskListItemNode(
          range = LocalTextRange.span(0, chars.length),
          listItemMarkerRange = LocalTextRange.span(0, openingMarker.length),
          taskMarkerRange = LocalTextRange(
            startOffset = markerSuffix.startOffset - chars.startOffset,
            endOffset = markerSuffix.endOffset - chars.startOffset,
          ),
          childrenRange = LocalTextRange(
            startOffset = body.startOffset - chars.startOffset,
            endOffset = body.endOffset - chars.startOffset,
          ),
          isChecked = isItemDoneMarker,
          children = this.walkSubtree { it.toWysiwygMarkdownNode() },
        )
      }
      is ListItem -> {
        ListItemNode(
          range = LocalTextRange.span(0, chars.length),
          markerRange = LocalTextRange.span(0, openingMarker.length),
          children = this.walkSubtree { it.toWysiwygMarkdownNode() },
        )
      }
      is Heading -> {
        if (isAtxHeading && text.isNotBlank) {
          HeadingNode(
            range = LocalTextRange.span(0, chars.length),
            openingMarkerRange = LocalTextRange.span(0, text.startOffset - chars.startOffset),
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
          range = LocalTextRange.span(0, chars.length),
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
