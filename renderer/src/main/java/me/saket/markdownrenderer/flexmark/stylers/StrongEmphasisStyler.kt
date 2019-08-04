package me.saket.markdownrenderer.flexmark.stylers

import com.vladsch.flexmark.ast.DelimitedNode
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.ast.StrongEmphasis
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.flexmark.SimpleFlexmarkSyntaxStyler
import me.saket.markdownrenderer.spans.pool.SpanPool

class StrongEmphasisStyler : SimpleFlexmarkSyntaxStyler<StrongEmphasis>() {

  override fun visit(
    node: StrongEmphasis,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme
  ) {
    writer.add(pool.bold(), node.startOffset, node.endOffset)
    highlightMarkdownSyntax(node, writer, pool, theme)
  }

  private fun <T> highlightMarkdownSyntax(
    delimitedNode: T,
    writer: SpanWriter,
    pool: SpanPool,
    theme: WysiwygTheme
  ) where T : Node, T : DelimitedNode {
    if (delimitedNode.openingMarker.isNotEmpty()) {
      writer.add(
          pool.foregroundColor(theme.syntaxColor),
          delimitedNode.startOffset,
          delimitedNode.startOffset + delimitedNode.openingMarker.length
      )
    }

    if (delimitedNode.closingMarker.isNotEmpty()) {
      writer.add(
          pool.foregroundColor(theme.syntaxColor),
          delimitedNode.endOffset - delimitedNode.closingMarker.length,
          delimitedNode.endOffset
      )
    }
  }

}