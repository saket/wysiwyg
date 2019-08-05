package me.saket.markdownrenderer.flexmark.highlighters

import com.vladsch.flexmark.ast.IndentedCodeBlock
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.flexmark.NodeVisitor
import me.saket.markdownrenderer.spans.pool.SpanPool
import me.saket.markdownrenderer.spans.pool.indentedCodeBlock
import me.saket.markdownrenderer.spans.pool.monospaceTypeface

class IndentedCodeBlockVisitor : NodeVisitor<IndentedCodeBlock> {

  override fun visit(
    node: IndentedCodeBlock,
    pool: SpanPool,
    writer: SpanWriter
  ) {
    // A LineBackgroundSpan needs to start at the starting of the line.
    val lineStartOffset = node.startOffset - 4

    writer.add(pool.indentedCodeBlock(), lineStartOffset, node.endOffset)
    writer.add(pool.monospaceTypeface(), node.startOffset, node.endOffset)
  }
}