package me.saket.wysiwyg.internal

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import androidx.tracing.trace
import me.saket.wysiwyg.MarkdownSpanPainter

// todo: kdoc
interface MarkdownStyleBuffer {
  /** The source text being styled. */
  val unstyledText: String

  /** Painters collected via [addSpanPainter] during the last render. */
  val spanPainters: List<MarkdownSpanPainter>

  fun addStyle(
    style: SpanStyle,
    range: TextRange,
  )

  /**
   * @param trimVerticalPadding Compose always adds one extra empty line at every ParagraphStyle
   *   slice boundary on top of whatever the source markdown already produces. When this is true,
   *   the boundary newline's own line height is shrunk to nearly zero as a workaround, so the
   *   styled block sits flush against its neighbour, matching what a plain TextField would
   *   render. Author-intended blank-line separators are preserved because they contribute a
   *   *second* `\n` that we don't touch.
   *   https://issuetracker.google.com/issues/241426911
   */
  fun addStyle(
    style: ParagraphStyle,
    range: TextRange,
    trimVerticalPadding: Boolean = false, // todo: default to true?
  )

  fun addSpanPainter(
    painter: MarkdownSpanPainter,
  )

  /**
   * Records a tag used by tests to assert the structure of rendered markdown. No-ops in production.
   */
  fun addTestTag(
    tag: String,
    range: TextRange,
  )

  companion object {
    /** A no-op buffer used as a placeholder before any rendering has occurred. */
    val Empty: MarkdownStyleBuffer = EmptyMarkdownStyleBuffer
  }
}

internal class TextFieldMarkdownStyleBuffer(
  private val textBuffer: TextFieldBuffer,
  override val unstyledText: String,
) : MarkdownStyleBuffer {
  override val spanPainters: MutableList<MarkdownSpanPainter> = mutableListOf()

  override fun addStyle(style: SpanStyle, range: TextRange) {
    trace("Wysiwyg:render:addStyle") {
      textBuffer.addStyle(
        spanStyle = style,
        start = range.start.coerceAtMost(unstyledText.length - 1),
        end = range.end.coerceAtMost(unstyledText.length),
      )
    }
  }

  override fun addStyle(
    style: ParagraphStyle,
    range: TextRange,
    trimVerticalPadding: Boolean,
  ) {
    trace("Wysiwyg:render:addStyle") {
      textBuffer.addStyle(
        paragraphStyle = style,
        start = range.start.coerceAtMost(unstyledText.length - 1),
        end = range.end.coerceAtMost(unstyledText.length),
      )
      if (trimVerticalPadding) {
        if (unstyledText.getOrNull(range.start - 1) == '\n') {
          textBuffer.addStyle(
            paragraphStyle = TinyParagraphStyle,
            start = range.start - 1,
            end = range.start
          )
        }
        if (unstyledText.getOrNull(range.end) == '\n') {
          textBuffer.addStyle(
            paragraphStyle = TinyParagraphStyle,
            start = range.end,
            end = range.end + 1
          )
        }
      }
    }
  }

  override fun addSpanPainter(painter: MarkdownSpanPainter) {
    spanPainters.add(painter)
  }

  override fun addTestTag(tag: String, range: TextRange) = Unit

  companion object {
    private val TinyParagraphStyle = ParagraphStyle(
      lineHeight = 0.sp,
      lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
        mode = LineHeightStyle.Mode.Tight,
      ),
    )
  }
}

private object EmptyMarkdownStyleBuffer : MarkdownStyleBuffer {
  override val unstyledText: String = ""
  override val spanPainters: List<MarkdownSpanPainter> = emptyList()
  override fun addTestTag(tag: String, range: TextRange) = Unit
  override fun addSpanPainter(painter: MarkdownSpanPainter) = Unit
  override fun addStyle(style: SpanStyle, range: TextRange) = Unit
  override fun addStyle(
    style: ParagraphStyle,
    range: TextRange,
    trimVerticalPadding: Boolean
  ) = Unit
}
