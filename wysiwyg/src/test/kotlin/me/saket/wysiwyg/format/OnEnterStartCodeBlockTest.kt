package me.saket.wysiwyg.format

import org.junit.Test

class OnEnterStartCodeBlockTest {
  private val formatters = OnEnterMarkdownFormatters(listOf(OnEnterContinueList(), OnEnterStartCodeBlock))

  @Test fun `enter after fenced code syntax on the first line`() {
    formatters.assertOnEnter(
      input = """
              |```▮
              """.trimMargin(),
      expect = """
              |```
              |▮
              |```
              """.trimMargin()
    )
  }

  @Test fun `enter after fenced code syntax surrounded by text`() {
    formatters.assertOnEnter(
      input = """
              |Alfred: Shall you be taking the Batpod sir?
              |```▮
              |Batman/Bruce Wayne: In the middle of the day Alfred?
              """.trimMargin(),
      expect = """
              |Alfred: Shall you be taking the Batpod sir?
              |```
              |▮
              |```
              |Batman/Bruce Wayne: In the middle of the day Alfred?
              """.trimMargin()
    )
  }

  @Test fun `enter after fenced code syntax and language name`() {
    formatters.assertOnEnter(
      input = """
              |Alfred: Shall you be taking the Batpod sir?
              |```kotlin▮
              |Batman/Bruce Wayne: In the middle of the day Alfred?
              """.trimMargin(),
      expect = """
              |Alfred: Shall you be taking the Batpod sir?
              |```kotlin
              |▮
              |```
              |Batman/Bruce Wayne: In the middle of the day Alfred?
              """.trimMargin()
    )
  }

  @Test fun `enter after fenced code syntax when already inside a fenced code block`() {
    formatters.assertOnEnter(
      input = """
              |```
              |fun someCodeBlock() {}
              |```
              |
              |Alfred: Shall you be taking the Batpod sir?
              |
              |```kotlin▮
              |```
              |
              |Batman/Bruce Wayne: In the middle of the day Alfred?
              |
              |```javaOmg
              |fun anotherCodeBlock() {}
              |```
              """.trimMargin(),
      expect = null
    )
  }

  @Test fun `enter on the same line as the closing marker of fenced code syntax`() {
    formatters.assertOnEnter(
      input = """
              |```
              |fun someCodeBlock() {}
              |```
              |
              |Alfred: Shall you be taking the Batpod sir?
              |
              |```kotlin
              |```▮
              |Batman/Bruce Wayne: In the middle of the day Alfred?
              |
              |```javaOmg
              |fun anotherCodeBlock() {}
              |```
              """.trimMargin(),
      expect = null
    )
  }

  @Test fun `enter key after the second fenced code syntax`() {
    formatters.assertOnEnter(
      input = """
              |```
              |fun someCodeBlock() {}
              |```
              |
              |```▮
              """.trimMargin(),
      expect = """
              |```
              |fun someCodeBlock() {}
              |```
              |
              |```
              |▮
              |```
              """.trimMargin()
    )
  }
}
