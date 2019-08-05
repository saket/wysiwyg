package me.saket.markdownrenderer.flexmark.stylers

import com.vladsch.flexmark.ast.BlockQuote
import com.vladsch.flexmark.ast.Node
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.flexmark.NodeVisitor
import me.saket.markdownrenderer.spans.pool.SpanPool

class BlockQuoteVisitor : NodeVisitor<BlockQuote> {

  override fun visit(
    node: BlockQuote,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme
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
    val quoteSpan = pool.quote(theme.markwonTheme)
    writer.add(quoteSpan, node.startOffset - nestedParents, node.endOffset)

    // Quote markers ('>').
    val markerStartOffset = node.startOffset - nestedParents
    writer.add(pool.foregroundColor(theme.syntaxColor), markerStartOffset, node.startOffset + 1)

    // Text color.
    writer.add(
        pool.foregroundColor(theme.blockQuoteTextColor), node.startOffset - nestedParents,
        node.endOffset
    )
  }
}