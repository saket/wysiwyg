package me.saket.wysiwyg.parser.treesitter

import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Parser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.saket.wysiwyg.parser.ChangeListSnapshot
import me.saket.wysiwyg.parser.MarkdownParser
import me.saket.wysiwyg.parser.MarkdownParser.ParseResult
import me.saket.wysiwyg.parser.treesitter.grammar.TreeSitterMarkdown

/**
 * Incremental markdown parser backed by tree-sitter. Node→span mapping pending.
 */
class TreeSitterMarkdownParser : MarkdownParser {
  private val parser = Parser(Language(TreeSitterMarkdown.language()))

  override fun parse(text: String, changes: ChangeListSnapshot): Flow<ParseResult> = flow {
    val tree = parser.parse(text)
    TODO("walk $tree and emit spans")
  }
}
