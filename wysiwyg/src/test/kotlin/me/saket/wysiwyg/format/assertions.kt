package me.saket.wysiwyg.format

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
