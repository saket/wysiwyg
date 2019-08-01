package me.saket.markdownrenderer

import android.text.Spannable
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import me.saket.markdownrenderer.flexmark.FlexmarkMarkdownParser

/**
 * See [FlexmarkMarkdownParser].
 */
interface MarkdownParser {

  @WorkerThread
  fun parseSpans(text: Spannable): MarkdownHintsSpanWriter

  /**
   * Called on every text change so that stale spans can
   * be removed before applying new ones.
   */
  @UiThread
  fun removeSpans(text: Spannable)
}
