package me.saket.markdownrenderer.flexmark.stylers

import com.vladsch.flexmark.ast.Link
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.flexmark.NodeVisitor
import me.saket.markdownrenderer.spans.pool.SpanPool
import me.saket.markdownrenderer.spans.pool.foregroundColor

class LinkVisitor : NodeVisitor<Link> {

  override fun visit(
    node: Link,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme
  ) {
    // Text.
    writer.add(pool.foregroundColor(theme.linkTextColor), node.startOffset, node.endOffset)

    // Url.
    val textClosingPosition = node.startOffset + node.text.length + 1
    val urlOpeningPosition = textClosingPosition + 1
    writer.add(pool.foregroundColor(theme.linkUrlColor), urlOpeningPosition, node.endOffset)
  }
}