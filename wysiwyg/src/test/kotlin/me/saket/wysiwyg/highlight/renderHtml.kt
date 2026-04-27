package me.saket.wysiwyg.highlight

import androidx.compose.ui.text.TextRange
import me.saket.wysiwyg.highlight.MarkdownEditOverlay.Companion.overlayed

internal fun MarkdownDocument.renderHtml(source: String): String {
  val tags = collectTags(children, overlay).sortedBy { it.offsetInRoot }

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
  children: List<MarkdownNode>,
  overlay: MarkdownEditOverlay,
  parentOldStart: Int = 0,
): List<HtmlTag> {
  return buildList {
    for (child in children) {
      val oldStart = parentOldStart + child.offsetInParent
      val newRange = TextRange.span(oldStart, child.totalLength).overlayed(overlay) ?: continue
      child.htmlTag()?.let { tag ->
        add(HtmlTag(newRange.start, "<$tag>"))
        add(HtmlTag(newRange.end, "</$tag>"))
      }
      addAll(
        collectTags(child.contents(), overlay, oldStart)
      )
    }
  }
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

private fun MarkdownNode.contents(): List<MarkdownNode> {
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
