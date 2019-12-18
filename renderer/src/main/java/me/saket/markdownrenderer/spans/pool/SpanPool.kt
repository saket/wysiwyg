@file:Suppress("UNCHECKED_CAST", "DEPRECATION", "unused")

package me.saket.markdownrenderer.spans.pool

import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.spans.WysiwygSpan
import java.util.Stack
import kotlin.DeprecationLevel.WARNING

typealias Recycler = (WysiwygSpan) -> Unit

/**
 * Pool for reusing spans instead of creating and throwing them on every text change.
 */
class SpanPool(val theme: WysiwygTheme) {

  val recycler: Recycler = this::recycle

  private val spans = mutableMapOf<Class<*>, Stack<WysiwygSpan>>()

  @Deprecated(message = "Use get<T>() instead", level = WARNING)
  fun <T : WysiwygSpan> get(
    clazz: Class<T>,
    default: () -> T
  ): T {
    val similarSpans = spans.getOrElse(clazz) { Stack() }
    return when {
      similarSpans.isEmpty() -> default()
      else -> similarSpans.pop() as T
    }
  }

  fun recycle(span: WysiwygSpan) {
    val similarSpans = spans.getOrElse(span::class.java) { Stack() }
    similarSpans.add(span)
    spans[span::class.java] = similarSpans
  }

  /**
   * Offers `get<WysiwygSpan>()` instead of `get(WysiwygSpan::class.java)`.
   */
  @Suppress("DEPRECATION")
  inline fun <reified T : WysiwygSpan> get(noinline default: () -> T): T {
    return get(T::class.java, default)
  }
}
