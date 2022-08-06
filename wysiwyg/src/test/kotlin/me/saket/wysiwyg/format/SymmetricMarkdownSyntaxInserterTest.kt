package me.saket.wysiwyg.format

import org.junit.Test

class SymmetricMarkdownSyntaxInserterTest {
  private val inserter = SymmetricMarkdownSyntaxInserter(
    syntax = "***",
    placeholder = "stars"
  )

  @Test fun `insert at cursor position`() {
    inserter.assertOnInsert(
      input = "He was trying to kill ▮ millions of innocent people.",
      expect = "He was trying to kill ***▮stars▮*** millions of innocent people."
    )
  }

  @Test fun `apply to selection`() {
    inserter.assertOnInsert(
      input = "Innocent is a ▮strong▮ word to throw around Gotham, Bruce.",
      expect = "Innocent is a ***strong***▮ word to throw around Gotham, Bruce."
    )
  }
}
