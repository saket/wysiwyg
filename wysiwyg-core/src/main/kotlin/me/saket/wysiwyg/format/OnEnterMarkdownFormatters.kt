package me.saket.wysiwyg.format

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextRange
import kotlin.coroutines.cancellation.CancellationException

@Stable
class OnEnterMarkdownFormatters(
  private val formatters: List<OnEnterMarkdownFormatter>,
) {
  fun asInputTransformation() =
    InputTransformation { applyIfEnterWasPressed() }

  private fun TextFieldBuffer.applyIfEnterWasPressed() {
    try {
      val wasEnterPressed = originalSelection.collapsed
          && length - originalText.length == 1
          && asCharSequence().getOrNull(selection.start - 1) == '\n'
      if (!wasEnterPressed) {
        return
      }

      val editedParagraph = TextParagraph.findUnderCursor(originalText, originalSelection)
      if (editedParagraph.text.isBlank()) {
        return
      }

      // todo: use a fast variant of firstNotNullOfOrNull.
      val replacement = formatters.firstNotNullOfOrNull {
        it.onEnterPressed(
          text = originalText,
          paragraph = editedParagraph,
          cursorPositionBeforeEnter = originalSelection.start,
        )
      } ?: return
      with(replacement) {
        replace()
      }

    } catch (e: CancellationException) {
      throw e
    } catch (_: Throwable) {
      // Formatter bugs (bad indices, regex edge cases) should not crash
      // text input. Next keystroke gets another chance at formatting.
      // todo: expose errors to consumers
    }
  }

  companion object {
    val Default = OnEnterMarkdownFormatters(
      listOf(
        OnEnterStartCodeBlock,
        OnEnterContinueList(),
      ),
    )
  }
}

/**
 * Note for self: unlike functions in [String], this does not return
 * -1 for empty paragraphs. See FindTextParagraphTest.
 */
internal fun TextParagraph.Companion.findUnderCursor(
  text: CharSequence,
  selection: TextRange,
): TextParagraph {
  val lastNewline = text.lastIndexOf('\n', startIndex = selection.min - 1)
  val startOffset = if (lastNewline == -1) 0 else lastNewline + 1

  val nextNewline = text.indexOf('\n', startIndex = selection.max)
  val endOffsetExclusive = if (nextNewline == -1) text.length else nextNewline

  return TextParagraph(
    text = text.substring(startOffset, endOffsetExclusive),
    startIndex = startOffset,
    endIndexExclusive = endOffsetExclusive,
  )
}
