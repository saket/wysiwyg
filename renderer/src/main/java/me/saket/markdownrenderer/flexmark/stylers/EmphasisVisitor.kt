package me.saket.markdownrenderer.flexmark.stylers

import android.graphics.Typeface
import com.vladsch.flexmark.ast.Emphasis
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.spans.StyleSpan
import me.saket.markdownrenderer.spans.pool.SpanPool

class EmphasisVisitor : DelimitedNodeVisitor<Emphasis>() {

  override fun visit(
    node: Emphasis,
    pool: SpanPool,
    writer: SpanWriter
  ) {
    writer.add(pool.italics(), node.startOffset, node.endOffset)
    super.visit(node, pool, writer)
  }

  private fun SpanPool.italics() =
    get { StyleSpan(recycler) }.apply {
      style = Typeface.ITALIC
    }
}
