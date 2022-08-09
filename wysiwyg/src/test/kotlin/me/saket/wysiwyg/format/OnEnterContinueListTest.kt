package me.saket.wysiwyg.format

import org.junit.Test

class OnEnterContinueListTest {
  private val formatters = OnEnterFormatters(listOf(OnEnterContinueList(), OnEnterStartCodeBlock))

  @Test fun `enter key after a valid unordered list item`() {
    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |- Milk
              |- Bread▮
              """.trimMargin(),
      expect = """
              |# Shopping list
              |- Milk
              |- Bread
              |- ▮
              """.trimMargin()
    )

    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |+ Milk
              |+ Bread▮
              """.trimMargin(),
      expect = """
              |# Shopping list
              |+ Milk
              |+ Bread
              |+ ▮
              """.trimMargin()
    )

    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |* Milk
              |* Bread▮
              """.trimMargin(),
      expect = """
              |# Shopping list
              |* Milk
              |* Bread
              |* ▮
              """.trimMargin()
    )
  }

  @Test fun `enter key after a valid unordered list item with margin`() {
    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |- Milk
              |- Drinks
              |  - Coke▮
              """.trimMargin(),
      expect = """
              |# Shopping list
              |- Milk
              |- Drinks
              |  - Coke
              |  - ▮
              """.trimMargin()
    )
  }

  @Test fun `enter key after a valid ordered list item with margin`() {
    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |1. Milk
              |2. Drinks
              |  3. Coke▮
              """.trimMargin(),
      expect = """
              |# Shopping list
              |1. Milk
              |2. Drinks
              |  3. Coke
              |  4. ▮
              """.trimMargin()
    )

    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |1. Milk
              |2. Drinks
              |  1. Coke▮
              """.trimMargin(),
      expect = """
              |# Shopping list
              |1. Milk
              |2. Drinks
              |  1. Coke
              |  2. ▮
              """.trimMargin()
    )
  }

  @Test fun `enter key after an invalid unordered list item`() {
    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |- Milk
              |Bread▮
              """.trimMargin(),
      expect = null
    )

    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |+ Milk
              |+Bread▮
              """.trimMargin(),
      expect = null
    )
  }

  @Test fun `enter key after a valid ordered list item`() {
    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |1. Milk
              |2. Bread▮
              """.trimMargin(),
      expect = """
              |# Shopping list
              |1. Milk
              |2. Bread
              |3. ▮
              """.trimMargin()
    )

    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |199. Milk
              |200. Bread▮
              """.trimMargin(),
      expect = """
              |# Shopping list
              |199. Milk
              |200. Bread
              |201. ▮
              """.trimMargin()
    )
  }

  @Test fun `enter key after an invalid ordered list item`() {
    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |1. Milk
              |2.Bread▮
              """.trimMargin(),
      expect = null
    )

    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |1. Milk
              |2Bread▮
              """.trimMargin(),
      expect = null
    )

    formatters.assertOnEnter(
      input = """
              |# Ingredients
              |180 Tomatoes
              |200 Onions▮
              """.trimMargin(),
      expect = null
    )
  }

  @Test fun `enter key on an empty unordered list item with a space`() {
    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |- Milk
              |- Bread
              |- ▮
              |
              |Some other text
              """.trimMargin(),
      expect = """
              |# Shopping list
              |- Milk
              |- Bread
              |
              |▮
              |
              |Some other text
              """.trimMargin()
    )

    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |+ Milk
              |+ Bread
              |+ ▮
              |
              |Some other text
              """.trimMargin(),
      expect = """
              |# Shopping list
              |+ Milk
              |+ Bread
              |
              |▮
              |
              |Some other text
              """.trimMargin()
    )

    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |* Milk
              |* Bread
              |* ▮
              |
              |Some other text
              """.trimMargin(),
      expect = """
              |# Shopping list
              |* Milk
              |* Bread
              |
              |▮
              |
              |Some other text
              """.trimMargin()
    )
  }

  @Test fun `enter key on a valid empty ordered list item with a space`() {
    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |1. Milk
              |2. Bread
              |3. ▮
              |
              |Some other text
              """.trimMargin(),
      expect = """
              |# Shopping list
              |1. Milk
              |2. Bread
              |
              |▮
              |
              |Some other text
              """.trimMargin()
    )
  }

  @Test fun `enter key on an empty unordered list item without a space`() {
    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |- Milk
              |- Bread
              |-▮
              |
              |Some other text
              """.trimMargin(),
      expect = null
    )

    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |+ Milk
              |+ Bread
              |+▮
              |
              |Some other text
              """.trimMargin(),
      expect = null
    )

    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |* Milk
              |* Bread
              |*▮
              |
              |Some other text
              """.trimMargin(),
      expect = null
    )
    formatters.assertOnEnter(
      input = """
              |# Shopping list
              |1. Milk
              |2. Bread
              |3.▮
              |
              |Some other text
              """.trimMargin(),
      expect = null
    )
  }

  @Test fun `enter key on a list item on the first line`() {
    formatters.assertOnEnter(
      input = """
              |-▮
              """.trimMargin(),
      expect = null
    )
  }
}
