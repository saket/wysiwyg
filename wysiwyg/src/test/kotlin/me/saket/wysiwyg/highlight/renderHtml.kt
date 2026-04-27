package me.saket.wysiwyg.highlight

import androidx.compose.ui.text.TextRange
import me.saket.wysiwyg.highlight.rebased

internal fun MarkdownDocument.renderHtml(source: String): String {
  val tags = collectTags(children, changes).sortedBy { it.offsetInRoot }

  return buildString {
    var cursor = 0
    for (tag in tags) {
      append(source, cursor, tag.offsetInRoot)
      append(tag.text)
      cursor = tag.offsetInRoot
    }
    append(source, cursor, source.length)
  }
}

private fun collectTags(
  children: List<MarkdownChildNode>,
  changes: TextChangeListSnapshot,
  parentOldStart: Int = 0,
): List<HtmlTag> {
  return buildList {
    for (child in children) {
      val oldStart = parentOldStart + child.offsetInParent
      val newRange = TextRange.span(oldStart, child.node.totalLength).rebased(changes) ?: continue
      child.node.htmlTag()?.let { tag ->
        add(HtmlTag(newRange.start, "<$tag>"))
        add(HtmlTag(newRange.end, "</$tag>"))
      }
      addAll(
        collectTags(child.node.contents(), changes, oldStart)
      )
    }
  }
}

private fun TextRange.Companion.span(startOffset: Int, length: Int): TextRange {
  return TextRange(startOffset, startOffset + length)
}

private fun MarkdownNode.htmlTag(): String? {
  return when (this) {
    is BoldNode -> "b"
    is LinkNode -> "link"
    is BlockQuoteNode -> "blockquote"
    is HeadingNode -> "h1"
    is ListBlockNode -> "list"
    else -> null
  }
}

private fun MarkdownNode.contents(): List<MarkdownChildNode> {
  return when (this) {
    is ListBlockNode -> children
    is ListItemNode -> children
    else -> emptyList()
  }
}

private data class HtmlTag(
  val offsetInRoot: Int,
  val text: String
)
