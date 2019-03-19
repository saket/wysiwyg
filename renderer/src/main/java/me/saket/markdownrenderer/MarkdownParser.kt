package me.saket.markdownrenderer

import android.text.Spannable
import me.saket.markdownrenderer.flexmark.FlexmarkMarkdownParser

/**
 * See [FlexmarkMarkdownParser].
 */
interface MarkdownParser {

  fun parseSpans(text: Spannable): MarkdownHintsSpanWriter

  /**
   * Called on every text change so that stale spans can
   * be removed before applying new ones.
   */
  fun removeSpans(text: Spannable)
}
