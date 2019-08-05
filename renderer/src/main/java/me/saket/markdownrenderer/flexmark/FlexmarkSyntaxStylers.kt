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
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.parser.Parser
import me.saket.markdownrenderer.flexmark.stylers.BlockQuoteVisitor
import me.saket.markdownrenderer.flexmark.stylers.EmphasisVisitor
import me.saket.markdownrenderer.flexmark.stylers.FencedCodeBlockVisitor
import me.saket.markdownrenderer.flexmark.stylers.HeadingVisitor
import me.saket.markdownrenderer.flexmark.stylers.IndentedCodeBlockVisitor
import me.saket.markdownrenderer.flexmark.stylers.InlineCodeVisitor
import me.saket.markdownrenderer.flexmark.stylers.LinkVisitor
import me.saket.markdownrenderer.flexmark.stylers.ListBlockVisitor
import me.saket.markdownrenderer.flexmark.stylers.ListItemVisitor
import me.saket.markdownrenderer.flexmark.stylers.StrikethroughVisitor
import me.saket.markdownrenderer.flexmark.stylers.StrongEmphasisVisitor
import me.saket.markdownrenderer.flexmark.stylers.ThematicBreakVisitor

@Suppress("UNCHECKED_CAST")
class FlexmarkSyntaxStylers {

  private val stylers = mutableMapOf<Class<out Node>, MutableList<FlexmarkSyntaxStyler<*>>>()

  init {
    add(Emphasis::class.java, EmphasisVisitor())
    add(StrongEmphasis::class.java, StrongEmphasisVisitor())
    add(Link::class.java, LinkVisitor())
    add(Strikethrough::class.java, StrikethroughVisitor())
    add(Code::class.java, InlineCodeVisitor())
    add(IndentedCodeBlock::class.java, IndentedCodeBlockVisitor())
    add(BlockQuote::class.java, BlockQuoteVisitor())
    add(ListBlock::class.java, ListBlockVisitor())
    add(ListItem::class.java, ListItemVisitor())
    add(ThematicBreak::class.java, ThematicBreakVisitor())
    add(Heading::class.java, HeadingVisitor())
    add(FencedCodeBlock::class.java, FencedCodeBlockVisitor())
  }

  /**
   * Because multiple [FlexmarkSyntaxStyler] could be present for the same [node] and
   * [FlexmarkSyntaxStyler] are allowed to have a missing visitor, this tries finds
   * the first NodeVisitor that can read [node].
   */
  fun nodeVisitor(node: Node): NodeVisitor<Node> {
    val nodeStylers = stylers[node::class.java] as List<FlexmarkSyntaxStyler<Node>>?

    if (nodeStylers != null) {
      // Intentionally using for-i loop instead of for-each or
      // anything else that creates a new Iterator under the hood.
      for (i in 0 until nodeStylers.size) {
        val nodeVisitor = nodeStylers[i].visitor(node)
        if (nodeVisitor != null) {
          return nodeVisitor
        }
      }
    }

    return NodeVisitor.EMPTY
  }

  fun <T : Node> add(
    nodeType: Class<T>,
    visitor: NodeVisitor<T>
  ) {
    add(
        nodeType = nodeType,
        styler = object : FlexmarkSyntaxStyler<T> {
          override fun visitor(node: T) = visitor
        }
    )
  }

  fun <T : Node> add(
    nodeType: Class<T>,
    styler: FlexmarkSyntaxStyler<T>
  ) {
    if (nodeType in stylers) {
      stylers[nodeType]!!.add(styler)
    } else {
      @Suppress("ReplacePutWithAssignment") // `stylers[key] = value` doesn't compile.
      stylers.put(nodeType, mutableListOf(styler))
    }
  }

  fun buildParser(): Parser {
    val parserBuilder = FlexmarkParserBuilder()

    val allStylers = stylers.flatMap { it.value }
    for (styler in allStylers) {
      styler.buildParser(parserBuilder)
    }

    return parserBuilder
        .addExtension(StrikethroughExtension.create())
        .build()
  }
}