package me.saket.markdownrenderer.flexmark

import com.vladsch.flexmark.ast.Node
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.spans.pool.SpanPool

/**
 * To support:
 * - Superscript
 */
open class FlexmarkNodeTreeVisitor(
    private val stylers: FlexmarkSyntaxStylers,
    private val styles: WysiwygTheme,
    private val pool: SpanPool
) {

  private lateinit var writer: SpanWriter

  fun visit(markdownRootNode: Node, hintsWriter: SpanWriter) {
    visitChildren(markdownRootNode, hintsWriter)
  }

  /**
   * Visit the child nodes.
   *
   * @param parent the parent node whose children should be visited
   */
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
      visitor.visit(node, pool, this.writer, styles, this)

      node = next
    }
  }
}
