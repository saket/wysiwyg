// todo: change package back to parser
package me.saket.wysiwyg.parser

fun interface MarkdownParser {
  /**
   * Produces a markdown AST for [text]. Called on every edit (on the main thread).
   *
   * Implementations don't produce interim results. Flicker-free rendering is handled automatically
   * by Wysiwyg, which wraps every parser in [IncrementalMarkdownParser] to emit a rebased document
   * between calls.
   *
   * @param changes Describes what changed since the previous call. This can be used by
   * parsers that support incremental invalidation of their markdown ASTs.
   */
  suspend fun parse(text: String, changes: TextChangeListSnapshot): MarkdownDocument
}
