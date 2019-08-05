package me.saket.markdownrenderer.spans.pool

import me.saket.markdownrenderer.spans.WysiwygSpan
import java.util.Stack
import kotlin.DeprecationLevel.WARNING

typealias Recycler = (WysiwygSpan) -> Unit

@Suppress("UNCHECKED_CAST", "DEPRECATION")
abstract class AbstractSpanPool {
  private val spans = mutableMapOf<Class<*>, Stack<WysiwygSpan>>()

  @Deprecated(message = "Use get<T>() instead", level = WARNING)
  open fun <T : WysiwygSpan> get(
    clazz: Class<T>,
    default: () -> T
  ): T {
    val similarSpans = spans.getOrElse(clazz) { Stack() }
    return when {
      similarSpans.isEmpty() -> default()
      else -> similarSpans.pop() as T
    }
  }

  inline fun <reified T : WysiwygSpan> get(noinline default: () -> T): T {
    return get(T::class.java, default)
  }

  open fun recycle(span: WysiwygSpan) {
    val similarSpans = spans.getOrElse(span.javaClass) { Stack() }
    similarSpans.add(span)
    spans[span.javaClass] = similarSpans
  }
}
