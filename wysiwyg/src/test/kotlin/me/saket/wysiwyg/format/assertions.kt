package me.saket.wysiwyg.format

import androidx.compose.ui.text.input.TextFieldValue
import me.saket.wysiwyg.decodeTextSelection
import me.saket.wysiwyg.encodeTextSelection

internal fun OnEnterFormatters.assertOnEnter(
  input: String,
  expect: String?
) {
  val output = onEnterPressed(decodeTextSelection(input))
  val expectedValue = expect?.let(::decodeTextSelection)
  assertTextsAreEquals(output, expectedValue)
}

internal fun MarkdownSyntaxInserter.assertOnInsert(
  input: String,
  expect: String?
) {
  val output = insertInto(decodeTextSelection(input))
  val expectedValue = expect?.let(::decodeTextSelection)
  assertTextsAreEquals(output, expectedValue)
}

private fun assertTextsAreEquals(output: TextReplacement?, expected: TextFieldValue?) {
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
      }
    )
  }
}
