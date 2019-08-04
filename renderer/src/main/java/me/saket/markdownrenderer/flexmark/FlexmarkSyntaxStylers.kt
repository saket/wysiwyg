package me.saket.markdownrenderer.flexmark

import com.vladsch.flexmark.ast.Emphasis
import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.ast.StrongEmphasis
import me.saket.markdownrenderer.flexmark.stylers.EmphasisStyler
import me.saket.markdownrenderer.flexmark.stylers.LinkStyler
import me.saket.markdownrenderer.flexmark.stylers.StrongEmphasisStyler

@Suppress("UNCHECKED_CAST")
class FlexmarkSyntaxStylers {

  private val stylers = mapOf(
      Emphasis::class.java to EmphasisStyler(),
      StrongEmphasis::class.java to StrongEmphasisStyler(),
      Link::class.java to LinkStyler()
  )

  fun nodeVisitor(node: Node): NodeVisitor<Node> {
    val visitor = stylers[node::class.java] as NodeVisitor<Node>?
    return visitor ?: NodeVisitor.DEFAULT
  }
}