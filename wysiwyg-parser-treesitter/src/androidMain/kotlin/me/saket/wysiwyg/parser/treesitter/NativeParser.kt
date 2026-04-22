@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package me.saket.wysiwyg.parser.treesitter

import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Parser
import io.github.treesitter.ktreesitter.Tree
import java.lang.reflect.Field

/**
 * Parse Kotlin strings as UTF-16 directly, bypassing kotlin-tree-sitter's hardcoded-UTF-8
 * [Parser.parse] string overload. Offsets on nodes returned from this path are byte offsets
 * into the UTF-16 buffer (i.e. `char index * 2`), so dividing by 2 gives Kotlin `String`
 * char indices — no byte→char table required.
 *
 * Still depends on private-field access for `Parser.self` (`private val`, so JVM-private
 * too — reflection is the only way). Spike only — upstream an encoding setter to
 * kotlin-tree-sitter if we want to keep this long-term.
 *
 * TODO: remove this whole file once https://github.com/tree-sitter/kotlin-tree-sitter/issues/61
 *  is resolved.
 */
internal fun Parser.parseUtf16(language: Language, source: String): Tree {
  val parserPtr = parserSelfField.getLong(this)
  val treePtr = NativeParser.parseUtf16(parserPtr, source)
  check(treePtr != 0L) { "ts_parser_parse_string_encoding returned null" }
  return Tree(self = treePtr, source = source, language = language)
}

private val parserSelfField: Field =
  Parser::class.java.getDeclaredField("self").apply { isAccessible = true }

private object NativeParser {
  init {
    // Same .so as the grammar; already loaded by TreeSitterMarkdown
    // at class init time, but System.loadLibrary is idempotent.
    System.loadLibrary("ktreesitter-markdown")
  }

  @JvmStatic
  external fun parseUtf16(parserPtr: Long, source: String): Long
}
