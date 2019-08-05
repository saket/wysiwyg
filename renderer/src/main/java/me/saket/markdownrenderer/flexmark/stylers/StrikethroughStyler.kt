package me.saket.markdownrenderer.flexmark.stylers

import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.spans.pool.SpanPool

class StrikethroughStyler : DelimitedNodeVisitor<Strikethrough>() {

  override fun visit(
    node: Strikethrough,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme
  ) {
    writer.add(pool.strikethrough(), node.startOffset, node.endOffset)
    super.visit(node, pool, writer, theme)
  }
}