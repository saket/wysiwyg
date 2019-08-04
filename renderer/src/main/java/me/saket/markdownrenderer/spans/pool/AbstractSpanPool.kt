package me.saket.markdownrenderer.spans.pool

import me.saket.markdownrenderer.spans.WysiwygSpan
import timber.log.Timber
import java.util.Stack
import kotlin.DeprecationLevel.WARNING

@Suppress("UNCHECKED_CAST", "DEPRECATION")
abstract class AbstractSpanPool {
  private val spans = mutableMapOf<Class<*>, Stack<Any>>()

  @Deprecated(message = "Use get<T>() instead", level = WARNING)
  open fun <T : WysiwygSpan> get(
    clazz: Class<T>,
    default: () -> T
  ): T {
    val similarSpans = spans.getOrElse(clazz) { Stack() }
    return when {
      similarSpans.isEmpty() -> {
        Timber.i("Similar spans empty: $similarSpans, Getting default.")
        default()
      }
      else -> {
        Timber.i("Similar spans available: $similarSpans")
        similarSpans.pop() as T
      }
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
