package me.saket.wysiwyg.parser

import androidx.compose.ui.text.TextRange
import dev.drewhamilton.poko.Poko

interface MarkdownNode {
  val range: LocalTextRange
  val children: List<MarkdownChildNode> get() = emptyList()
}

@Poko
class MarkdownChildNode(
  val offsetInParent: Int,
  val node: MarkdownNode,
)

@JvmInline
value class LocalTextRange private constructor(
  private val textRange: TextRange,
) {
  val localStart: Int get() = textRange.start
  val localEnd: Int get() = textRange.end

  constructor(startOffset: Int, endOffset: Int) : this(
    TextRange(startOffset, endOffset)
  )

  companion object {
    /** A local range starting at [startOffset] and spanning [length] code units. */
    fun span(startOffset: Int, length: Int): LocalTextRange {
      return LocalTextRange(TextRange(startOffset, startOffset + length))
    }
  }
}
