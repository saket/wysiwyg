package me.saket.markdownrenderer

import org.junit.Test

class InlineClassTest {

  @Test fun `inline class bug`() {
    val string: String? = "2019"
    val typeAdapter = StringToDoubleTypeAdapter()
    string?.let(typeAdapter::decode)
  }
}

inline class NumberInlineClass(val value: Double)

class StringToDoubleTypeAdapter : TypeAdapter<String, NumberInlineClass>, TypeAdapter2 {
  override fun decode(string: String) = NumberInlineClass(string.toDouble())
}

// The error only occurs if an interface is implemented by the reference function.
interface TypeAdapter<FROM, TO> {
  fun decode(string: FROM): TO
}

interface TypeAdapter2 {
  fun decode(string: String): NumberInlineClass
}