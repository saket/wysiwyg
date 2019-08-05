package me.saket.markdownrenderer.flexmark

import com.vladsch.flexmark.ast.Node
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.spans.pool.SpanPool

open class FlexmarkNodeTreeVisitor(
    private val stylers: FlexmarkSyntaxStylers,
    private val theme: WysiwygTheme,
    private val pool: SpanPool
) {

  fun visit(markdownRootNode: Node, hintsWriter: SpanWriter) {
    visitChildren(markdownRootNode, hintsWriter)
  }

  open fun visitChildren(
    parent: Node,
    writer: SpanWriter
  ) {
    var node: Node? = parent.firstChild
    while (node != null) {
      // A subclass of this visitor might modify the node, resulting in getNext returning a
      // different node or no node after visiting it. So get the next node before visiting.
      val next = node.next

      val visitor = stylers.nodeVisitor(node)
      visitor.visitWithChildren(node, pool, writer, theme, this)

      node = next
    }
  }
}
