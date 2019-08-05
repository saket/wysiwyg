package me.saket.markdownrenderer.flexmark.highlighters

import com.vladsch.flexmark.ast.ListItem
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.flexmark.NodeVisitor
import me.saket.markdownrenderer.spans.pool.SpanPool
import me.saket.markdownrenderer.spans.pool.foregroundColor

class ListItemVisitor : NodeVisitor<ListItem> {

  override fun visit(
    node: ListItem,
    pool: SpanPool,
    writer: SpanWriter
  ) {
    writer.add(pool.foregroundColor(pool.theme.syntaxColor), node.startOffset, node.startOffset + 1)
  }
}
