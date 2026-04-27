package me.saket.wysiwyg.highlight

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.util.fastForEach
import dev.drewhamilton.poko.Poko
import me.saket.wysiwyg.internal.MarkdownNodeRenderScope

interface MarkdownNode {
  val range: LocalTextRange

  fun MarkdownNodeRenderScope.render(text: AnnotatedString.Builder)
}

@Poko
class MarkdownChildNode(
  val offsetInParent: Int,
  val node: MarkdownNode,
)

@JvmInline
value class LocalTextRange private constructor(
  val textRange: TextRange,
) {
  val start: Int get() = textRange.start
  val end: Int get() = textRange.end

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

@Poko
class MarkdownDocument(
  override val range: LocalTextRange,
  val children: List<MarkdownChildNode>,
  val changes: List<TextChangeListSnapshot> = emptyList(),
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(text: AnnotatedString.Builder) {
    children.fastForEach { child ->
      child.render(text)
    }
  }

  fun copy(changes: List<TextChangeListSnapshot>): MarkdownDocument {
    return MarkdownDocument(
      range = this.range,
      children = this.children,
      changes = changes,
    )
  }
}
