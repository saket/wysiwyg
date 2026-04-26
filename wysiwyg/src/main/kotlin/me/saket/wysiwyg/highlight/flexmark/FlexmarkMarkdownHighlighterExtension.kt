package me.saket.wysiwyg.highlight.flexmark

import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import me.saket.wysiwyg.highlight.MarkdownNode

interface FlexmarkMarkdownHighlighterExtension {
  /**
   * Flexmark extensions or post-processor factories can be registered
   * here for parsing text and inserting custom nodes to the AST.
   */
  fun buildParser(builder: Parser.Builder)

  /**
   * Once an AST is generated, this function is called for each markdown
   * node in the tree to create markdown nodes for them.
   */
  fun Node.addNodesInto(buffer: MutableList<MarkdownNode>)
}
