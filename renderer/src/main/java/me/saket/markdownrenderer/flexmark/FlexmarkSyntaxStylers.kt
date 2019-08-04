package me.saket.markdownrenderer.flexmark

import com.vladsch.flexmark.ast.Emphasis
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.ast.StrongEmphasis
import me.saket.markdownrenderer.flexmark.stylers.EmphasisStyler
import me.saket.markdownrenderer.flexmark.stylers.HeadingStyler
import me.saket.markdownrenderer.flexmark.stylers.LinkStyler
import me.saket.markdownrenderer.flexmark.stylers.StrongEmphasisStyler

@Suppress("UNCHECKED_CAST")
class FlexmarkSyntaxStylers {

  private val stylers: Map<Class<*>, Set<FlexmarkSyntaxStyler<*>>> = mapOf(
      Emphasis::class.java to setOf(EmphasisStyler()),
      StrongEmphasis::class.java to setOf(StrongEmphasisStyler()),
      Link::class.java to setOf(LinkStyler()),
      Heading::class.java to setOf(HeadingStyler())
  )

  fun nodeVisitor(node: Node): NodeVisitor<Node> {
    val nodeStylers = stylers[node::class.java] as Set<FlexmarkSyntaxStyler<Node>>?
    return nodeStylers
        ?.firstOrNull { it.visitor(node) != null }
        ?.visitor(node) ?: NodeVisitor.EMPTY
  }
}