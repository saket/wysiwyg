package me.saket.wysiwyg.format

import org.junit.Test

class BlockQuoteMarkerInserterTest {
  private val markerInserter = CompoundableParagraphMarkerInserter.BlockQuote

  @Test fun `insert at cursor position at the end of the first line in a paragraph`() {
    markerInserter.assertOnInsert(
      input = """
              |Alfred: Shall you be taking the Batpod sir?▮
              |Batman/Bruce Wayne: In the middle of the day Alfred?
              |Alfred: The Lamborghini then? Much more subtle.
              """.trimMargin(),
      expect = """
              |> Alfred: Shall you be taking the Batpod sir?▮
              |Batman/Bruce Wayne: In the middle of the day Alfred?
              |Alfred: The Lamborghini then? Much more subtle.
              """.trimMargin(),
    )
  }

  @Test fun `insert at cursor position at the end of a line in the middle of a paragraph`() {
    markerInserter.assertOnInsert(
      input = """
              |Alfred: Shall you be taking the Batpod sir?
              |Batman/Bruce Wayne: In the middle of the day Alfred?▮
              |Alfred: The Lamborghini then? Much more subtle.
              """.trimMargin(),
      expect = """
              |Alfred: Shall you be taking the Batpod sir?
              |> Batman/Bruce Wayne: In the middle of the day Alfred?▮
              |Alfred: The Lamborghini then? Much more subtle.
              """.trimMargin(),
    )
  }

  @Test fun `insert at cursor position at the end of the last line in a paragraph`() {
    markerInserter.assertOnInsert(
      input = """
              |Alfred: Shall you be taking the Batpod sir?
              |Batman/Bruce Wayne: In the middle of the day Alfred?
              |Alfred: The Lamborghini then? Much more subtle.▮
              """.trimMargin(),
      expect = """
              |Alfred: Shall you be taking the Batpod sir?
              |Batman/Bruce Wayne: In the middle of the day Alfred?
              |> Alfred: The Lamborghini then? Much more subtle.▮
              """.trimMargin(),
    )
  }

  @Test fun `insert at cursor position on a line followed by an empty line in a paragraph`() {
    markerInserter.assertOnInsert(
      input = """
              |Alfred: Shall you be taking the Batpod sir?
              |Batman/Bruce Wayne: In the middle of the day Alfred?▮
              |
              |Alfred: The Lamborghini then? Much more subtle.
              """.trimMargin(),
      expect = """
              |Alfred: Shall you be taking the Batpod sir?
              |> Batman/Bruce Wayne: In the middle of the day Alfred?▮
              |
              |Alfred: The Lamborghini then? Much more subtle.
              """.trimMargin(),
    )
    markerInserter.assertOnInsert(
      input = """
              |Alfred: Shall you be taking the Batpod sir?
              |Batman/Bruce Wayne: In the middle of the day Alfred?
              |Alfred: The Lamborghini then? Much more subtle.▮
              |
              |Another line.
              """.trimMargin(),
      expect = """
              |Alfred: Shall you be taking the Batpod sir?
              |Batman/Bruce Wayne: In the middle of the day Alfred?
              |> Alfred: The Lamborghini then? Much more subtle.▮
              |
              |Another line.
              """.trimMargin(),
    )
  }

  @Test fun `insert at cursor position in blank content`() {
    markerInserter.assertOnInsert(
      input = """
              |▮
              """.trimMargin(),
      expect = """
              |> ▮
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
              |> ▮
              """.trimMargin(),
    )
  }

  @Test fun `insert at cursor position in the middle of a paragraph`() {
    markerInserter.assertOnInsert(
      input = """
              |Tell your men they work ▮ for me now. This is my city.
              """.trimMargin(),
      expect = """
              |> Tell your men they work ▮ for me now. This is my city.
              """.trimMargin(),
    )
  }

  @Test fun `apply to selection in the middle of a paragraph`() {
    markerInserter.assertOnInsert(
      input = """
              |Tell your men they work for ▮me▮ now. This is my city.
              """.trimMargin(),
      expect = """
              |> Tell your men they work for ▮me▮ now. This is my city.
              """.trimMargin(),
    )
  }

  @Test fun `apply to selection to a whole paragraph`() {
    markerInserter.assertOnInsert(
      input = """
              |▮Tell your men they work for me now. This is my city.▮
              """.trimMargin(),
      expect = """
              |> ▮Tell your men they work for me now. This is my city.▮
              """.trimMargin(),
    )
  }

  @Test fun `apply to selection of multiple paragraphs`() {
    markerInserter.assertOnInsert(
      input = """
              |James Gordon: ▮Batman. Batman! Why is he running dad?
              |Gordon: Because we have to▮ chase him.
              |Uniform Cop: Okay we're going in. Go go! Move!
              |James Gordon: He didn't do anything wrong.
              |Gordon: Because he's the hero Gotham deserves, but not the one it needs right now.
              |So we'll hunt him. Because he can take it. Because he's not a hero. He's a silent
              |guardian. A watchful protector. The Dark Knight.
              """.trimMargin(),
      expect = """
              |> James Gordon: ▮Batman. Batman! Why is he running dad?
              |Gordon: Because we have to▮ chase him.
              |Uniform Cop: Okay we're going in. Go go! Move!
              |James Gordon: He didn't do anything wrong.
              |Gordon: Because he's the hero Gotham deserves, but not the one it needs right now.
              |So we'll hunt him. Because he can take it. Because he's not a hero. He's a silent
              |guardian. A watchful protector. The Dark Knight.
              """.trimMargin(),
    )
  }

  @Test fun `insert at cursor position on a new line`() {
    markerInserter.assertOnInsert(
      input = """
              |Tell your men they work for me now. This is my city.
              |▮
              """.trimMargin(),
      expect = """
              |Tell your men they work for me now. This is my city.
              |> ▮
              """.trimMargin(),
    )
  }

  @Test fun `insert at cursor position on a new line with leading spaces`() {
    markerInserter.assertOnInsert(
      input = """
              |Tell your men they work for me now. This is my city.
              |  ▮
              """.trimMargin(),
      expect = """
              |Tell your men they work for me now. This is my city.
              |>  ▮
              """.trimMargin(),
    )
  }

  @Test fun `toggle a block-quote off`() {
    markerInserter.assertOnInsert(
      input = """
              |> Tell your men they work for me now. This is my city.▮
              """.trimMargin(),
      expect = """
              |Tell your men they work for me now. This is my city.▮
              """.trimMargin(),
    )
    // Collapses every leading marker, not just one.
    markerInserter.assertOnInsert(
      input = """
              |>> Tell your men they work for me now. This is my city.▮
              """.trimMargin(),
      expect = """
              |Tell your men they work for me now. This is my city.▮
              """.trimMargin(),
    )
  }
}
