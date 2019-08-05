package me.saket.markdownrenderer.flexmark.stylers

import com.vladsch.flexmark.ast.Code
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.spans.InlineCodeSpan
import me.saket.markdownrenderer.spans.pool.SpanPool
import me.saket.markdownrenderer.spans.pool.monospaceTypeface
import io.noties.markwon.core.MarkwonTheme

class InlineCodeVisitor : DelimitedNodeVisitor<Code>() {

  override fun visit(
    node: Code,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme
  ) {
    writer.add(pool.inlineCode(theme.markwonTheme), node.startOffset, node.endOffset)
    writer.add(pool.monospaceTypeface(), node.startOffset, node.endOffset)
    super.visit(node, pool, writer, theme)
  }

  private fun SpanPool.inlineCode(markwonTheme: MarkwonTheme) =
    get { InlineCodeSpan(markwonTheme, recycler) }
}