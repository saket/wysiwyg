package me.saket.wysiwyg.format

import org.junit.Test

class FencedCodeBlockMarkerInserterTest {
  private val markerInserter = FencedCodeBlockMarkerInserter

  @Test fun `insert at cursor position at the end of the first line in a paragraph`() {
    markerInserter.assertOnInsert(
      input = """
              |Gordon: What's your name son?▮
              |John Blake: Blake sir.
              |Gordon: You have something you wanna ask me Officer Blake?
              """.trimMargin(),
      expect = """
              |```
              |Gordon: What's your name son?▮
              |```
              |John Blake: Blake sir.
              |Gordon: You have something you wanna ask me Officer Blake?
              """.trimMargin(),
    )
  }

  @Test fun `insert at cursor position at the end of a line in the middle of a paragraph`() {
    markerInserter.assertOnInsert(
      input = """
              |Gordon: What's your name son?
              |John Blake: Blake sir.▮
              |Gordon: You have something you wanna ask me Officer Blake?
              """.trimMargin(),
      expect = """
              |Gordon: What's your name son?
              |```
              |John Blake: Blake sir.▮
              |```
              |Gordon: You have something you wanna ask me Officer Blake?
              """.trimMargin(),
    )
  }

  @Test fun `insert at cursor position at the end of the last line in a paragraph`() {
    markerInserter.assertOnInsert(
      input = """
              |Gordon: What's your name son?
              |John Blake: Blake sir.
              |Gordon: You have something you wanna ask me Officer Blake?▮
              """.trimMargin(),
      expect = """
              |Gordon: What's your name son?
              |John Blake: Blake sir.
              |```
              |Gordon: You have something you wanna ask me Officer Blake?▮
              |```
              """.trimMargin(),
    )
  }

  @Test fun `insert at cursor position in blank content`() {
    markerInserter.assertOnInsert(
      input = """
              |▮
              """.trimMargin(),
      expect = """
              |```
              |▮
              |```
              """.trimMargin(),
    )
  }

  @Test fun `insert at cursor position in blank content with leading new line`() {
    markerInserter.assertOnInsert(
      input = """
              |
              |▮
              """.trimMargin(),
      expect = """
              |
              |```
              |▮
              |```
              """.trimMargin(),
    )
  }

  @Test fun `insert at cursor position in the middle of a paragraph`() {
    markerInserter.assertOnInsert(
      input = """
              |You have something you ▮ wanna ask me Officer Blake?
              """.trimMargin(),
      expect = """
              |```
              |You have something you ▮ wanna ask me Officer Blake?
              |```
              """.trimMargin(),
    )
  }

  @Test fun `apply to selection in the middle of a paragraph`() {
    markerInserter.assertOnInsert(
      input = """
              |You have something you wanna ▮ask me▮ Officer Blake?
              """.trimMargin(),
      expect = """
              |```
              |You have something you wanna ▮ask me▮ Officer Blake?
              |```
              """.trimMargin(),
    )
  }

  @Test fun `apply to selection to a whole paragraph`() {
    markerInserter.assertOnInsert(
      input = """
              |▮You have something you wanna ask me Officer Blake?▮
              """.trimMargin(),
      expect = """
              |```
              |▮You have something you wanna ask me Officer Blake?▮
              |```
              """.trimMargin(),
    )
  }

  @Test fun `insert at cursor position on a new line`() {
    markerInserter.assertOnInsert(
      input = """
              |What's your name son?
              |▮
              """.trimMargin(),
      expect = """
              |What's your name son?
              |```
              |▮
              |```
              """.trimMargin(),
    )
  }

  @Test fun `insert at cursor position on a new line with leading spaces`() {
    markerInserter.assertOnInsert(
      input = """
              |What's your name son?
              |  ▮
              """.trimMargin(),
      expect = """
              |What's your name son?
              |```
              |  ▮
              |```
              """.trimMargin(),
    )
  }
}
