package me.saket.wysiwyg.toolbar

data class MarkdownBlock(
    val prefix: String,
    val suffix: String = ""
) {

  companion object {
    val BOLD = MarkdownBlock("**", "**")
    val ITALIC = MarkdownBlock("*", "*")
    val STRIKE_THROUGH = MarkdownBlock("~~", "~~")
    val QUOTE = MarkdownBlock("> ")
    val INLINE_CODE = MarkdownBlock("`", "`")
    val HEADING = MarkdownBlock("# ")
  }
}
