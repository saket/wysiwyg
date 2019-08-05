package me.saket.markdownrenderer.flexmark.highlighters

import com.vladsch.flexmark.ast.DelimitedNode
import com.vladsch.flexmark.util.ast.Node
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.flexmark.NodeVisitor
import me.saket.markdownrenderer.spans.pool.SpanPool
import me.saket.markdownrenderer.spans.pool.foregroundColor

abstract class DelimitedNodeVisitor<T>
  : NodeVisitor<T> where T : Node, T : DelimitedNode {

  override fun visit(
    node: T,
    pool: SpanPool,
    writer: SpanWriter
  ) {
    if (node.openingMarker.isNotEmpty()) {
      writer.add(
          pool.foregroundColor(pool.theme.syntaxColor),
          node.startOffset,
          node.startOffset + node.openingMarker.length
      )
    }

    if (node.closingMarker.isNotEmpty()) {
      writer.add(
          pool.foregroundColor(pool.theme.syntaxColor),
          node.endOffset - node.closingMarker.length,
          node.endOffset
      )
    }
  }
}