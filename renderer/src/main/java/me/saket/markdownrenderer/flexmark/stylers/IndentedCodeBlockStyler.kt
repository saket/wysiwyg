package me.saket.markdownrenderer.flexmark.stylers

import com.vladsch.flexmark.ast.IndentedCodeBlock
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.flexmark.SimpleFlexmarkSyntaxStyler
import me.saket.markdownrenderer.spans.pool.SpanPool

class IndentedCodeBlockStyler : SimpleFlexmarkSyntaxStyler<IndentedCodeBlock>() {

  override fun visit(
    node: IndentedCodeBlock,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme
  ) {
    // A LineBackgroundSpan needs to start at the starting of the line.
    val lineStartOffset = node.startOffset - 4

    writer.add(pool.indentedCodeBlock(theme.markwonTheme), lineStartOffset, node.endOffset)
    writer.add(pool.monospaceTypeface(), node.startOffset, node.endOffset)
  }
}