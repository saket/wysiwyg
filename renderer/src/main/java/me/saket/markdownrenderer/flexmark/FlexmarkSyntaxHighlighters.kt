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
import com.vladsch.flexmark.ast.StrongEmphasis
import com.vladsch.flexmark.ast.ThematicBreak
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import me.saket.markdownrenderer.flexmark.highlighters.BlockQuoteVisitor
import me.saket.markdownrenderer.flexmark.highlighters.EmphasisVisitor
import me.saket.markdownrenderer.flexmark.highlighters.FencedCodeBlockVisitor
import me.saket.markdownrenderer.flexmark.highlighters.HeadingVisitor
import me.saket.markdownrenderer.flexmark.highlighters.IndentedCodeBlockVisitor
import me.saket.markdownrenderer.flexmark.highlighters.InlineCodeVisitor
import me.saket.markdownrenderer.flexmark.highlighters.LinkVisitor
import me.saket.markdownrenderer.flexmark.highlighters.ListBlockVisitor
import me.saket.markdownrenderer.flexmark.highlighters.ListItemVisitor
import me.saket.markdownrenderer.flexmark.highlighters.StrikethroughVisitor
import me.saket.markdownrenderer.flexmark.highlighters.StrongEmphasisVisitor
import me.saket.markdownrenderer.flexmark.highlighters.ThematicBreakVisitor

@Suppress("UNCHECKED_CAST")
class FlexmarkSyntaxHighlighters {

  private val highlighters = mutableMapOf<Class<out Node>, MutableList<FlexmarkSyntaxHighlighter<*>>>()

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
   * Because multiple [FlexmarkSyntaxHighlighter] could be present for the same [node] and
   * [FlexmarkSyntaxHighlighter] are allowed to have a missing visitor, this tries finds
   * the first NodeVisitor that can read [node].
   */
  fun nodeVisitor(node: Node): NodeVisitor<Node> {
    val nodeHighlighters = highlighters[node::class.java] as List<FlexmarkSyntaxHighlighter<Node>>?

    if (nodeHighlighters != null) {
      // Intentionally using for-i loop instead of for-each or
      // anything else that creates a new Iterator under the hood.
      for (i in 0 until nodeHighlighters.size) {
        val nodeVisitor = nodeHighlighters[i].visitor(node)
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
        highlighter = object : FlexmarkSyntaxHighlighter<T> {
          override fun visitor(node: T) = visitor
        }
    )
  }

  fun <T : Node> add(
    nodeType: Class<T>,
    highlighter: FlexmarkSyntaxHighlighter<T>
  ) {
    if (nodeType in highlighters) {
      highlighters[nodeType]!!.add(highlighter)
    } else {
      @Suppress("ReplacePutWithAssignment") // `highlighters[key] = value` doesn't compile.
      highlighters.put(nodeType, mutableListOf(highlighter))
    }
  }

  fun buildParser(): Parser {
    val parserBuilder = FlexmarkParserBuilder()

    val allStylers = highlighters.flatMap { it.value }
    for (styler in allStylers) {
      styler.buildParser(parserBuilder)
    }

    return parserBuilder
        .addExtension(StrikethroughExtension.create())
        .build()
  }
}