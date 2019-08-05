package me.saket.markdownrenderer.flexmark.stylers

import com.vladsch.flexmark.ast.StrongEmphasis
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.spans.pool.SpanPool

class StrongEmphasisStyler : DelimitedNodeVisitor<StrongEmphasis>() {

  override fun visit(
    node: StrongEmphasis,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme
  ) {
    writer.add(pool.bold(), node.startOffset, node.endOffset)
    super.visit(node, pool, writer, theme)
  }
}