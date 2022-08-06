package me.saket.wysiwyg.format

import org.junit.Test

class FencedCodeBlockSyntaxInserterTest {
  private val syntaxInserter = FencedCodeBlockSyntaxInserter

  @Test fun `insert at cursor position at the end of the first line in a paragraph`() {
    syntaxInserter.assertOnInsert(
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
              """.trimMargin()
    )
  }

  @Test fun `insert at cursor position at the end of a line in the middle of a paragraph`() {
    syntaxInserter.assertOnInsert(
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
              """.trimMargin()
    )
  }

  @Test fun `insert at cursor position at the end of the last line in a paragraph`() {
    syntaxInserter.assertOnInsert(
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
              """.trimMargin()
    )
  }

  @Test fun `insert at cursor position in blank content`() {
    syntaxInserter.assertOnInsert(
      input = """
              |▮
              """.trimMargin(),
      expect = """
              |```
              |▮
              |```
              """.trimMargin()
    )
  }

  @Test fun `insert at cursor position in blank content with leading new line`() {
    syntaxInserter.assertOnInsert(
      input = """
              |
              |▮
              """.trimMargin(),
      expect = """
              |
              |```
              |▮
              |```
              """.trimMargin()
    )
  }

  @Test fun `insert at cursor position in the middle of a paragraph`() {
    syntaxInserter.assertOnInsert(
      input = """
              |You have something you ▮ wanna ask me Officer Blake?
              """.trimMargin(),
      expect = """
              |```
              |You have something you ▮ wanna ask me Officer Blake?
              |```
              """.trimMargin()
    )
  }

  @Test fun `apply to selection in the middle of a paragraph`() {
    syntaxInserter.assertOnInsert(
      input = """
              |You have something you wanna ▮ask me▮ Officer Blake?
              """.trimMargin(),
      expect = """
              |```
              |You have something you wanna ▮ask me▮ Officer Blake?
              |```
              """.trimMargin()
    )
  }

  @Test fun `apply to selection to a whole paragraph`() {
    syntaxInserter.assertOnInsert(
      input = """
              |▮You have something you wanna ask me Officer Blake?▮
              """.trimMargin(),
      expect = """
              |```
              |▮You have something you wanna ask me Officer Blake?▮
              |```
              """.trimMargin()
    )
  }

  @Test fun `insert at cursor position on a new line`() {
    syntaxInserter.assertOnInsert(
      input = """
              |What's your name son?
              |▮
              """.trimMargin(),
      expect = """
              |What's your name son?
              |```
              |▮
              |```
              """.trimMargin()
    )
  }

  @Test fun `insert at cursor position on a new line with leading spaces`() {
    syntaxInserter.assertOnInsert(
      input = """
              |What's your name son?
              |  ▮
              """.trimMargin(),
      expect = """
              |What's your name son?
              |```
              |  ▮
              |```
              """.trimMargin()
    )
  }
}
