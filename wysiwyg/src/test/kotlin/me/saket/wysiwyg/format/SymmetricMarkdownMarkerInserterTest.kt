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
      expect = "Innocent is a ***strong***▮ word to throw around Gotham, Bruce.",
    )
  }

  @Test fun `toggle off when selection wraps existing markers`() {
    inserter.assertOnInsert(
      input = "Innocent is a ▮***strong***▮ word to throw around Gotham, Bruce.",
      expect = "Innocent is a ▮strong▮ word to throw around Gotham, Bruce.",
    )
  }

  @Test fun `toggle off when selection sits inside existing markers`() {
    inserter.assertOnInsert(
      input = "Innocent is a ***▮strong▮*** word to throw around Gotham, Bruce.",
      expect = "Innocent is a ▮strong▮ word to throw around Gotham, Bruce.",
    )
  }

  @Test fun `toggle off when cursor is inside a marked span`() {
    inserter.assertOnInsert(
      input = "Innocent is a ***str▮ong*** word to throw around Gotham, Bruce.",
      expect = "Innocent is a str▮ong word to throw around Gotham, Bruce.",
    )
  }
}
