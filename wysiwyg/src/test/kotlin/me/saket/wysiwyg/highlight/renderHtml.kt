package me.saket.wysiwyg.highlight

import androidx.compose.ui.text.TextRange

internal fun MarkdownDocument.renderHtml(source: String): String {
  return buildString {
    var cursor = 0
    for (child in children) {
      append(source, cursor, child.range.start)
      appendNodeHtml(source, child)
      cursor = child.range.end
    }
    append(source, cursor, source.length)
  }
}

private fun StringBuilder.appendNodeHtml(source: String, node: MarkdownNode) {
  when (node) {
    is BoldNode -> {
      append("<b>${source.substring(node.range)}</b>")
    }
    is HeadingNode -> {
      append("<h1>${source.substring(node.range)}</h1>")
    }
    is ListBlockNode -> {
      append("<list>")
      var cursor = node.range.start
      for (item in node.items) {
        append(source, cursor, item.range.start)
        appendNodeHtml(source, item)
        cursor = item.range.end
      }
      append(source, cursor, node.range.end)
      append("</list>")
    }
    is ListItemNode -> {
      var cursor = node.marker.start
      for (child in node.children) {
        append(source, cursor, child.range.start)
        appendNodeHtml(source, child)
        cursor = child.range.end
      }
      append(source, cursor, node.range.end)
    }
    else -> {
      append(source, node.range.start, node.range.end)
    }
  }
}

private fun String.substring(range: TextRange): String {
  return this.substring(range.start, range.end)
}
