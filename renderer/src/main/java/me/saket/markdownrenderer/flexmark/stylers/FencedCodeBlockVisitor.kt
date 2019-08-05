package me.saket.markdownrenderer.flexmark.stylers

import com.vladsch.flexmark.ast.FencedCodeBlock
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.flexmark.FlexmarkSyntaxStyler
import me.saket.markdownrenderer.flexmark.NodeVisitor
import me.saket.markdownrenderer.spans.pool.SpanPool
import me.saket.markdownrenderer.spans.pool.indentedCodeBlock
import me.saket.markdownrenderer.spans.pool.monospaceTypeface

class FencedCodeBlockVisitor : FlexmarkSyntaxStyler<FencedCodeBlock> {

  override fun visitor(node: FencedCodeBlock): NodeVisitor<FencedCodeBlock>? {
    val clashesWithStrikethrough = node.openingMarker.contains('~')
    return when {
      clashesWithStrikethrough -> null
      else -> fencedCodeVisitor()
    }
  }

  private fun fencedCodeVisitor() =
    object : NodeVisitor<FencedCodeBlock> {
      override fun visit(
        node: FencedCodeBlock,
        pool: SpanPool,
        writer: SpanWriter
      ) {
        writer.add(pool.indentedCodeBlock(), node.startOffset, node.endOffset)
        writer.add(pool.monospaceTypeface(), node.startOffset, node.endOffset)
      }
    }
}