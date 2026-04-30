package me.saket.wysiwyg.format

import org.junit.Test

class SymmetricMarkdownMarkerInserterTest {
  private val inserter = SymmetricMarkdownMarkerInserter(
    marker = "***",
    placeholder = "stars",
  )

  @Test fun `insert at cursor position`() {
    inserter.assertOnInsert(
      input = "He was trying to kill ▮ millions of innocent people.",
      expect = "He was trying to kill ***▮stars▮*** millions of innocent people.",
    )
  }

  @Test fun `apply to selection`() {
    inserter.assertOnInsert(
      input = "Innocent is a ▮strong▮ word to throw around Gotham, Bruce.",
      expect = "Innocent is a ***▮strong▮*** word to throw around Gotham, Bruce.",
    )
  }

  @Test fun `applying twice toggles off`() {
    // Re-clicking after wrap-on with a selection toggles off.
    inserter.assertOnInsert(
      input = "Innocent is a ***▮strong▮*** word to throw around Gotham, Bruce.",
      expect = "Innocent is a ▮strong▮ word to throw around Gotham, Bruce.",
    )
    // Re-clicking after wrap-on with a bare cursor toggles off.
    inserter.assertOnInsert(
      input = "He was trying to kill ***▮stars▮*** millions of innocent people.",
      expect = "He was trying to kill ▮stars▮ millions of innocent people.",
    )
  }
}
