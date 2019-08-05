package me.saket.markdownrenderer.flexmark

import com.vladsch.flexmark.ast.Node
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.spans.pool.SpanPool

interface FlexmarkSyntaxStyler<in T : Node> {
  fun visitor(node: T): NodeVisitor<T>?
  fun buildParser(builder: FlexmarkParserBuilder) = Unit
}

interface NodeVisitor<in T : Node> {
  fun visit(
    node: T,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme
  )

  /**
   * Override this if you don't want the default behavior of
   * visiting children AFTER this [node]'s styling is applied.
   */
  fun visitWithChildren(
    node: T,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme,
    parentVisitor: FlexmarkNodeTreeVisitor
  ) {
    visit(node, pool, writer, theme)
    parentVisitor.visitChildren(node, writer)
  }

  companion object {
    val EMPTY = object : NodeVisitor<Node> {
      override fun visit(
        node: Node,
        pool: SpanPool,
        writer: SpanWriter,
        theme: WysiwygTheme
      ) = Unit
    }
  }
}
