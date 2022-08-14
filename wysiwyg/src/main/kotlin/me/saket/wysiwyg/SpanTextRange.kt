package me.saket.wysiwyg

import androidx.compose.ui.text.TextRange

@JvmInline
value class SpanTextRange private constructor(private val range: TextRange) {
  val startIndex: Int get() = range.start
  val endIndexExclusive: Int get() = range.end
  val endIndexInclusive: Int get() = range.end - 1

  constructor(startIndex: Int, endIndexExclusive: Int) : this(TextRange(startIndex, endIndexExclusive))

  init {
    check(endIndexExclusive >= startIndex) { "Invalid offsets for $this" }
  }

  override fun toString(): String {
    return "SpanTextRange(start=$startIndex, endExclusive=$endIndexExclusive)"
  }
}
