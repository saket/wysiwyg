package me.saket.markdownrenderer.flexmark.stylers

import com.vladsch.flexmark.ast.Code
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.spans.InlineCodeSpan
import me.saket.markdownrenderer.spans.pool.SpanPool
import me.saket.markdownrenderer.spans.pool.monospaceTypeface

class InlineCodeVisitor : DelimitedNodeVisitor<Code>() {

  override fun visit(
    node: Code,
    pool: SpanPool,
    writer: SpanWriter
  ) {
    writer.add(pool.inlineCode(), node.startOffset, node.endOffset)
    writer.add(pool.monospaceTypeface(), node.startOffset, node.endOffset)
    super.visit(node, pool, writer)
  }

  private fun SpanPool.inlineCode() =
    get { InlineCodeSpan(theme, recycler) }
}