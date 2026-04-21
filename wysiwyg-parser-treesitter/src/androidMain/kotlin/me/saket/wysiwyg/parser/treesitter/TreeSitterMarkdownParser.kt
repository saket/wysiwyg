package me.saket.wysiwyg.parser.treesitter

import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Parser
import io.github.treesitter.ktreesitter.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import me.saket.wysiwyg.BlockQuoteBodySpanStyle
import me.saket.wysiwyg.BlockQuoteParagraphLineSpanStyle
import me.saket.wysiwyg.FencedCodeBlockSpanStyle
import me.saket.wysiwyg.HeadingSpanStyle
import me.saket.wysiwyg.ListBlockSpanStyle
import me.saket.wysiwyg.MarkdownSpan
import me.saket.wysiwyg.MarkdownSpanTextRange
import me.saket.wysiwyg.MarkerColorSpanStyle
import me.saket.wysiwyg.ThematicBreakSpanStyle
import me.saket.wysiwyg.parser.ChangeListSnapshot
import me.saket.wysiwyg.parser.MarkdownParser
import me.saket.wysiwyg.parser.MarkdownParser.ParseResult
import me.saket.wysiwyg.parser.treesitter.grammar.TreeSitterMarkdown
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Markdown parser backed by tree-sitter.
 *
 * Uses tree-sitter queries rather than a manual tree walk to minimize JNI boundary crossings:
 * the C engine pre-filters matching nodes and hands Kotlin one batch per capture.
 *
 * TODOs:
 * - **Inline grammar.** Currently emits only block-level spans (headings, code fences, block
 *   quotes, lists, thematic breaks). Emphasis, links, code spans, and strikethroughs need the
 *   inline grammar (`tree-sitter-markdown-inline`) wired via [Parser.includedRanges].
 * - **Incremental parsing.** The parser currently re-parses and re-walks the entire document
 *   on every call, ignoring [ChangeListSnapshot]. A real incremental pipeline would apply
 *   edits via `tree.edit(InputEdit)`, re-parse with `oldTree`, then use
 *   `newTree.getChangedRanges(oldTree)` to scope span re-emission to just the changed regions.
 * - **UTF-8 vs UTF-16 offsets.** Tree-sitter byte offsets assume UTF-8. Compose's
 *   `AnnotatedString` uses UTF-16 char indices. Non-ASCII text will mis-align spans until
 *   we add a conversion layer.
 */
class TreeSitterMarkdownParser : MarkdownParser {
  private val language = Language(TreeSitterMarkdown.language())
  private val parser = Parser(language)
  private val query = Query(language, source = BlockQuery)

  override fun parse(text: String, changes: ChangeListSnapshot): Flow<ParseResult> {
    return flow {
      val tree = parser.parse(text)
      val spans = mutableListOf<MarkdownSpan>()

      query.matches(tree.rootNode).forEach { match ->
        match.captures.fastForEach { capture ->
          val node = capture.node
          val range = MarkdownSpanTextRange(
            startIndex = node.startByte.toInt(),
            endIndexExclusive = node.endByte.toInt(),
          )
          when (val name = capture.name) {
            "heading.1",
            "heading.2",
            "heading.3",
            "heading.4",
            "heading.5",
            "heading.6" -> {
              spans += MarkdownSpan(HeadingSpanStyle(level = name.last().digitToInt()), range)
            }
            "heading.marker",
            "code_fence.marker",
            "blockquote.marker",
            "list.item.marker" -> {
              spans += MarkdownSpan(MarkerColorSpanStyle, range)
            }
            "code_fence.block" -> {
              spans += MarkdownSpan(FencedCodeBlockSpanStyle, range)
            }
            "blockquote.block" -> {
              // Two spans to match the Flexmark parser's output: body for text styling and
              // paragraph-line for the extendedspans painter that draws the quote bar.
              spans += MarkdownSpan(BlockQuoteBodySpanStyle, range)
              spans += MarkdownSpan(BlockQuoteParagraphLineSpanStyle, range)
            }
            "list.block" -> {
              spans += MarkdownSpan(ListBlockSpanStyle, range)
            }
            "thematic_break" -> {
              spans += MarkdownSpan(ThematicBreakSpanStyle, range)
              spans += MarkdownSpan(MarkerColorSpanStyle, range)
            }
          }
        }
      }
      emit(ParseResult(spans))
    }.flowOn(Dispatchers.Default)
  }

  companion object {
    /**
     * Tree-sitter query matching every block-level node we render. Evaluated natively by the
     * C engine and returned as a batch of `QueryMatch`es, so one JNI call covers an entire
     * document instead of one call per node visited — see README §2.
     *
     * Heading patterns are deliberately split one-per-level (`atx_h1_marker` ... `atx_h6_marker`)
     * so the level is encoded in the pattern's own capture name (`@heading.1` ... `@heading.6`).
     * [parse] can then build a [HeadingSpanStyle] without a second JNI round-trip to inspect the
     * marker node's type string.
     *
     * Capture names are the dispatch key in the `when` inside [parse] — edit them both together.
     */
    private val BlockQuery = """
      (atx_heading (atx_h1_marker) @heading.marker) @heading.1
      (atx_heading (atx_h2_marker) @heading.marker) @heading.2
      (atx_heading (atx_h3_marker) @heading.marker) @heading.3
      (atx_heading (atx_h4_marker) @heading.marker) @heading.4
      (atx_heading (atx_h5_marker) @heading.marker) @heading.5
      (atx_heading (atx_h6_marker) @heading.marker) @heading.6

      (fenced_code_block (fenced_code_block_delimiter) @code_fence.marker) @code_fence.block

      (block_quote) @blockquote.block
      (block_quote_marker) @blockquote.marker

      (list) @list.block
      (list_item
        [(list_marker_dot) (list_marker_minus) (list_marker_parenthesis)
         (list_marker_plus) (list_marker_star)] @list.item.marker)

      (thematic_break) @thematic_break
    """.trimIndent()
  }
}

/** Copied from [androidx.compose.ui.util.fastForEach]. */
@OptIn(ExperimentalContracts::class)
private inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
  contract { callsInPlace(action) }
  for (index in indices) {
    val item = get(index)
    action(item)
  }
}