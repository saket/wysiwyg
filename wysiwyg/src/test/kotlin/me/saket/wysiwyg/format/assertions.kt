package me.saket.wysiwyg.format

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange

internal fun OnEnterMarkdownFormatters.assertOnEnter(
  input: String,
  expect: String?,
) {
  // Simulate the user pressing Enter: replace the current selection with `\n`
  // and put the cursor right after it.
  val before = decodeTextSelection(input)
  val afterText = before.text.replaceRange(
    startIndex = before.selection.min,
    endIndex = before.selection.max,
    replacement = "\n",
  )
  val after = encodeTextSelection(afterText, TextRange(before.selection.min + 1)).toString()

  asInputTransformation().assertOnChange(
    before = input,
    after = after,
    // When no formatter triggers, the buffer is left in its just-after-Enter state.
    expect = expect ?: after,
  )
}

internal fun MarkdownMarkerInserter.assertOnInsert(
  input: String,
  expect: String?,
) {
  val snapshot = decodeTextSelection(input)
  val replacement = insertInto(snapshot.text, snapshot.selection)
  val state = TextFieldState(
    initialText = snapshot.text,
    initialSelection = snapshot.selection,
  )
  state.edit {
    with(replacement) { replace() }
  }

  val expected = expect?.let(::decodeTextSelection)
  val actualText = state.text.toString()
  if (actualText != expected?.text || state.selection != expected.selection) {
    error(
      buildString {
        this.appendLine("--------------------------------------")
        this.appendLine("Text doesn't match.")
        if (expected?.text != null) {
          this.appendLine(
            "Expected:\n\"\"\"\n${encodeTextSelection(expected.text, expected.selection)}\n\"\"\""
          )
        } else {
          this.appendLine("Expected: \nnull")
        }
        this.appendLine(
          "\nActual: \n\"\"\"\n${encodeTextSelection(actualText, state.selection)}\n\"\"\""
        )
      },
    )
  }
}

/**
 * Runs the [InputTransformation] against the transition from [before] to [after]
 * and compares the resulting text + selection against [expect]. All three use
 * `▮` markers to encode the cursor/selection.
 */
internal fun InputTransformation.assertOnChange(
  before: String,
  after: String,
  expect: String,
) {
  val beforeSnapshot = decodeTextSelection(before)
  val afterSnapshot = decodeTextSelection(after)
  val state = TextFieldState(
    initialText = beforeSnapshot.text,
    initialSelection = beforeSnapshot.selection,
  )
  state.edit {
    replace(0, length, afterSnapshot.text)
    selection = afterSnapshot.selection
    with(this@assertOnChange) {
      transformInput()
    }
  }

  val expectedSnapshot = decodeTextSelection(expect)
  val actualText = state.text.toString()
  if (actualText != expectedSnapshot.text || state.selection != expectedSnapshot.selection) {
    error(
      buildString {
        appendLine("--------------------------------------")
        appendLine("Text doesn't match.")
        val encodedExpected = encodeTextSelection(expectedSnapshot.text, expectedSnapshot.selection)
        appendLine("Expected:\n\"\"\"\n$encodedExpected\n\"\"\"")
        appendLine(
          "\nActual: \n\"\"\"\n${encodeTextSelection(actualText, state.selection)}\n\"\"\""
        )
      },
    )
  }
}

