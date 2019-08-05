package me.saket.markdownrenderer.flexmark.stylers

import com.vladsch.flexmark.ast.DelimitedNode
import com.vladsch.flexmark.ast.Node
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.flexmark.SimpleFlexmarkSyntaxStyler
import me.saket.markdownrenderer.spans.pool.SpanPool

abstract class DelimitedNodeVisitor<T>
  : SimpleFlexmarkSyntaxStyler<T>() where T : Node, T : DelimitedNode {

  override fun visit(
    node: T,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme
  ) {
    if (node.openingMarker.isNotEmpty()) {
      writer.add(
          pool.foregroundColor(theme.syntaxColor),
          node.startOffset,
          node.startOffset + node.openingMarker.length
      )
    }

    if (node.closingMarker.isNotEmpty()) {
      writer.add(
          pool.foregroundColor(theme.syntaxColor),
          node.endOffset - node.closingMarker.length,
          node.endOffset
      )
    }
  }
}