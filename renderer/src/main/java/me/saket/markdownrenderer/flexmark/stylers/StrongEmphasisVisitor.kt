package me.saket.markdownrenderer.flexmark.stylers

import android.graphics.Typeface
import com.vladsch.flexmark.ast.StrongEmphasis
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.spans.StyleSpan
import me.saket.markdownrenderer.spans.pool.SpanPool

class StrongEmphasisVisitor : DelimitedNodeVisitor<StrongEmphasis>() {

  override fun visit(
    node: StrongEmphasis,
    pool: SpanPool,
    writer: SpanWriter
  ) {
    writer.add(pool.bold(), node.startOffset, node.endOffset)
    super.visit(node, pool, writer)
  }

  private fun SpanPool.bold() =
    get { StyleSpan(recycler) }.apply {
      style = Typeface.BOLD
    }
}