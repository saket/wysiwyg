package me.saket.wysiwyg.parser.flexmark

import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import me.saket.wysiwyg.MarkdownSpan

interface FlexmarkMarkdownParserExtension {
  /**
   * Flexmark extensions or post-processor factories can be registered
   * here for parsing text and inserting custom nodes to the AST.
   */
  fun buildParser(builder: Parser.Builder)

  /**
   * Once an AST is generated, this function is called for each markdown
   * node in the tree to create markdown spans for them.
   */
  fun Node.addSpansInto(buffer: MutableList<MarkdownSpan>)
}