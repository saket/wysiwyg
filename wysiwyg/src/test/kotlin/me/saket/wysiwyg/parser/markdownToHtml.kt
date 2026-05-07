package me.saket.wysiwyg.parser

import androidx.compose.ui.text.TextRange

internal fun MarkdownDocument.renderHtml(source: String): String {
  val renderer = HtmlRenderer()
  renderer.render(this, RealMarkdownRenderScope())
  return renderer.build(source)
}

/**
 * Test-only [MarkdownRenderer] that records HTML-like tags around the rebased range of recognized
 * nodes, then interleaves them with the source in [build]. The tag set matches the expectations
 * in `IncrementalMarkdownParserTest`.
 */
private class HtmlRenderer : MarkdownRenderer {
  private val tags = mutableListOf<Tag>()

  override fun render(node: MarkdownNode, scope: MarkdownRenderScope) {
    if (node is MarkdownDocument) {
      scope.offsetInRoot = 0
      scope.changes = node.changes
      tags.clear()
    }
    val tagName = node.htmlTag()
    if (tagName != null) {
      with(scope) {
        val resolved = node.range.resolve()
        if (resolved != null) {
          tags += Tag(tagName, resolved)
        }
      }
    }
    descendInto(node, scope)
  }

  fun build(source: String): String {
    val markers = tags
      .flatMap { listOf(it.range.start to "<${it.name}>", it.range.end to "</${it.name}>") }
      .sortedBy { it.first }
    return buildString {
      var cursor = 0
      for ((offset, tag) in markers) {
        append(source, cursor, offset)
        append(tag)
        cursor = offset
      }
      append(source, cursor, source.length)
    }
  }

  private data class Tag(val name: String, val range: TextRange)
}

private fun MarkdownNode.htmlTag(): String? {
  return when (this) {
    is BoldNode -> "b"
    is StrikeThroughNode -> "s"
    is LinkNode -> "link"
    is BlockQuoteNode -> "blockquote"
    is ListBlockNode -> "list"
    is HeadingNode -> "h$level"
    else -> null
  }
}
