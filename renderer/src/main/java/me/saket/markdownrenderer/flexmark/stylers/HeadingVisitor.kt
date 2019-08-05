package me.saket.markdownrenderer.flexmark.stylers

import com.vladsch.flexmark.ast.Heading
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.flexmark.FlexmarkSyntaxStyler
import me.saket.markdownrenderer.flexmark.NodeVisitor
import me.saket.markdownrenderer.spans.HeadingLevel
import me.saket.markdownrenderer.spans.HeadingSpan
import me.saket.markdownrenderer.spans.headingLevel
import me.saket.markdownrenderer.spans.pool.SpanPool
import me.saket.markdownrenderer.spans.pool.foregroundColor

@Suppress("SpellCheckingInspection")
class HeadingVisitor : FlexmarkSyntaxStyler<Heading> {

  override fun visitor(node: Heading): NodeVisitor<Heading>? {
    // Setext styles aren't supported. Setext-style headers are "underlined" using "="
    // (for first-level headers) and dashes (for second-level headers). For example:
    // This is an H1
    // =============
    //
    // This is an H2
    // -------------
    return when {
      node.isAtxHeading -> headingVisitor()
      else -> null
    }
  }

  private fun headingVisitor() = object : NodeVisitor<Heading> {
    override fun visit(
      node: Heading,
      pool: SpanPool,
      writer: SpanWriter
    ) {
      writer.add(pool.heading(node.headingLevel), node.startOffset, node.endOffset)
      writer.add(
          pool.foregroundColor(pool.theme.syntaxColor),
          node.startOffset,
          node.startOffset + node.openingMarker.length
      )
    }
  }

  private fun SpanPool.heading(level: HeadingLevel) =
    get { HeadingSpan(theme, recycler) }.apply {
      this.level = level
    }
}