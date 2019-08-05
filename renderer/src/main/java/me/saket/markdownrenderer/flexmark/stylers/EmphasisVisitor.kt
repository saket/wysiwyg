package me.saket.markdownrenderer.flexmark.stylers

import com.vladsch.flexmark.ast.Emphasis
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.spans.pool.SpanPool

class EmphasisVisitor : DelimitedNodeVisitor<Emphasis>() {

  override fun visit(
    node: Emphasis,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme
  ) {
    writer.add(pool.italics(), node.startOffset, node.endOffset)
    super.visit(node, pool, writer, theme)
  }
}
