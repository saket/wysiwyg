package me.saket.markdownrenderer.flexmark

import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.parser.Parser
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.spans.pool.SpanPool

interface FlexmarkSyntaxStyler<in T : Node> {
  fun visitor(node: T): NodeVisitor<T>?
  fun buildParser(): Parser = TODO()
}

interface NodeVisitor<in T : Node> {
  fun visit(
    node: T,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme,
    parentVisitor: FlexmarkNodeTreeVisitor
  )

  companion object {
    val EMPTY = object : NodeVisitor<Node> {
      override fun visit(
        node: Node,
        pool: SpanPool,
        writer: SpanWriter,
        theme: WysiwygTheme,
        parentVisitor: FlexmarkNodeTreeVisitor
      ) {
        parentVisitor.visitChildren(node)
      }
    }
  }
}

@Suppress("UNCHECKED_CAST")
abstract class SimpleFlexmarkSyntaxStyler<in T : Node> : FlexmarkSyntaxStyler<T> {

  override fun visitor(node: T): NodeVisitor<T>? {
    return object : NodeVisitor<T> {
      override fun visit(
        node: T,
        pool: SpanPool,
        writer: SpanWriter,
        theme: WysiwygTheme,
        parentVisitor: FlexmarkNodeTreeVisitor
      ) {
        visit(node, pool, writer, theme)
        parentVisitor.visitChildren(node)
      }
    }
  }

  abstract fun visit(
    node: T,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme
  )
}