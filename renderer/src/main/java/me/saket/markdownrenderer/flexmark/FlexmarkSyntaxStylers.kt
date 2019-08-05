package me.saket.markdownrenderer.flexmark

import com.vladsch.flexmark.ast.BlockQuote
import com.vladsch.flexmark.ast.Code
import com.vladsch.flexmark.ast.Emphasis
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.IndentedCodeBlock
import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.ast.ListBlock
import com.vladsch.flexmark.ast.ListItem
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.ast.StrongEmphasis
import com.vladsch.flexmark.ast.ThematicBreak
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough
import me.saket.markdownrenderer.flexmark.stylers.BlockQuoteStyler
import me.saket.markdownrenderer.flexmark.stylers.EmphasisStyler
import me.saket.markdownrenderer.flexmark.stylers.FencedCodeBlockStyler
import me.saket.markdownrenderer.flexmark.stylers.HeadingStyler
import me.saket.markdownrenderer.flexmark.stylers.IndentedCodeBlockStyler
import me.saket.markdownrenderer.flexmark.stylers.InlineCodeStyler
import me.saket.markdownrenderer.flexmark.stylers.LinkStyler
import me.saket.markdownrenderer.flexmark.stylers.ListBlockStyler
import me.saket.markdownrenderer.flexmark.stylers.ListItemStyler
import me.saket.markdownrenderer.flexmark.stylers.StrikethroughStyler
import me.saket.markdownrenderer.flexmark.stylers.StrongEmphasisStyler
import me.saket.markdownrenderer.flexmark.stylers.ThematicBreakStyler

@Suppress("UNCHECKED_CAST")
class FlexmarkSyntaxStylers {

  private val stylers: Map<Class<*>, List<FlexmarkSyntaxStyler<*>>> = mapOf(
      Emphasis::class.java to listOf(EmphasisStyler()),
      StrongEmphasis::class.java to listOf(StrongEmphasisStyler()),
      Link::class.java to listOf(LinkStyler()),
      Strikethrough::class.java to listOf(StrikethroughStyler()),
      Heading::class.java to listOf(HeadingStyler()),
      Code::class.java to listOf(InlineCodeStyler()),
      IndentedCodeBlock::class.java to listOf(IndentedCodeBlockStyler()),
      FencedCodeBlock::class.java to listOf(FencedCodeBlockStyler()),
      BlockQuote::class.java to listOf(BlockQuoteStyler()),
      ListBlock::class.java to listOf(ListBlockStyler()),
      ListItem::class.java to listOf(ListItemStyler()),
      ThematicBreak::class.java to listOf(ThematicBreakStyler())
  )

  fun nodeVisitor(node: Node): NodeVisitor<Node> {
    val nodeStylers = stylers[node::class.java] as Set<FlexmarkSyntaxStyler<Node>>?
    return nodeStylers
        ?.firstOrNull { it.visitor(node) != null }
        ?.visitor(node) ?: NodeVisitor.EMPTY
  }
}