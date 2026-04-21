package me.saket.wysiwyg.parser.treesitter.grammar

import dalvik.annotation.optimization.CriticalNative

/**
 * Hand-written mirror of the plugin-generated [TreeSitterMarkdown] wrapper for the inline
 * grammar. The ktreesitter-plugin's Gradle DSL only supports one grammar per module, so
 * the inline grammar's ~20 lines of glue (this file) and ~10 lines of JNI (`binding_inline.c`)
 * are written by hand.
 *
 * Shares the same native library as [TreeSitterMarkdown] — `CMakeLists.txt` links both
 * grammars into `libktreesitter-markdown.so`. No second `System.loadLibrary` call is needed.
 */
@Suppress("FunctionName")
object TreeSitterMarkdownInline {
  init {
    // Same .so as TreeSitterMarkdown; System.loadLibrary is idempotent per ClassLoader, so
    // calling it from both objects is safe and lets this object be used standalone.
    System.loadLibrary("ktreesitter-markdown")
  }

  fun language(): Any = tree_sitter_markdown_inline()

  @JvmStatic
  @CriticalNative
  private external fun tree_sitter_markdown_inline(): Long
}
