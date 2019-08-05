package me.saket.markdownrenderer.flexmark.highlighters

import com.vladsch.flexmark.ast.BlockQuote
import com.vladsch.flexmark.util.ast.Node
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.flexmark.NodeVisitor
import me.saket.markdownrenderer.spans.BlockQuoteSpan
import me.saket.markdownrenderer.spans.pool.SpanPool
import me.saket.markdownrenderer.spans.pool.foregroundColor

class BlockQuoteVisitor : NodeVisitor<BlockQuote> {

  override fun visit(
    node: BlockQuote,
    pool: SpanPool,
    writer: SpanWriter
  ) {
    // Android requires quote spans to be inserted at the starting of the line. Nested
    // quote spans are otherwise not rendered correctly. Calculate the offset for this
    // quote's starting index instead and include all text from there under the spans.
    var nestedParents = 0
    var parent: Node = node.parent
    while (parent is BlockQuote) {
      ++nestedParents
      parent = parent.parent
    }

    // Quote's vertical rule.
    val quoteSpan = pool.quote()
    writer.add(quoteSpan, node.startOffset - nestedParents, node.endOffset)

    // Quote markers ('>').
    val markerStartOffset = node.startOffset - nestedParents
    writer.add(pool.foregroundColor(pool.theme.syntaxColor), markerStartOffset, node.startOffset + 1)

    // Text color.
    writer.add(
        pool.foregroundColor(pool.theme.blockQuoteTextColor), node.startOffset - nestedParents,
        node.endOffset
    )
  }

  private fun SpanPool.quote() = get { BlockQuoteSpan(theme, recycler) }
}
