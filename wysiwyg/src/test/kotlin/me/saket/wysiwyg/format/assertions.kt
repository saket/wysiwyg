package me.saket.wysiwyg.format

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState

internal fun OnEnterMarkdownFormatters.assertOnEnter(
  input: String,
  expect: String?,
) {
  val snapshot = decodeTextSelection(input)
  val output = onEnterPressed(snapshot.text, snapshot.selection)
  val expected = expect?.let(::decodeTextSelection)
  assertTextsAreEqual(output, expected)
}

internal fun MarkdownMarkerInserter.assertOnInsert(
  input: String,
  expect: String?,
) {
  val snapshot = decodeTextSelection(input)
  val output = insertInto(snapshot.text, snapshot.selection)
  val expected = expect?.let(::decodeTextSelection)
  assertTextsAreEqual(output, expected)
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
        appendLine("Expected:\n\"\"\"\n${encodeTextSelection(expectedSnapshot.text, expectedSnapshot.selection)}\n\"\"\"")
        appendLine("\nActual: \n\"\"\"\n${encodeTextSelection(actualText, state.selection)}\n\"\"\"")
      },
    )
  }
}

private fun assertTextsAreEqual(output: TextReplacement?, expected: TextSnapshot?) {
  if (output?.text?.toString() != expected?.text || output?.newSelection != expected?.selection) {
    error(
      buildString {
        appendLine("--------------------------------------")
        appendLine("Text doesn't match.")
        if (expected?.text != null) {
          appendLine("Expected:\n\"\"\"\n${encodeTextSelection(expected.text, expected.selection)}\n\"\"\"")
        } else {
          appendLine("Expected: \nnull")
        }
        if (output?.text != null) {
          appendLine("\nActual: \n\"\"\"\n${encodeTextSelection(output.text, output.newSelection)}\n\"\"\"")
        } else {
          appendLine("\nActual: \nnull")
        }
      },
    )
  }
}
