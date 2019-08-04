package me.saket.markdownrenderer.flexmark.stylers

import com.vladsch.flexmark.ast.DelimitedNode
import com.vladsch.flexmark.ast.Emphasis
import com.vladsch.flexmark.ast.Node
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.flexmark.SimpleFlexmarkSyntaxStyler
import me.saket.markdownrenderer.spans.pool.SpanPool

class EmphasisStyler : SimpleFlexmarkSyntaxStyler<Emphasis>() {

  override fun visit(
    node: Emphasis,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme
  ) {
    writer.add(pool.italics(), node.startOffset, node.endOffset)
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