package me.saket.markdownrenderer.spans.pool

import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.annotation.Px
import me.saket.markdownrenderer.spans.BlockQuoteSpan
import me.saket.markdownrenderer.spans.ForegroundColorSpan
import me.saket.markdownrenderer.spans.HeadingSpanWithLevel
import me.saket.markdownrenderer.spans.HorizontalRuleSpan
import me.saket.markdownrenderer.spans.HorizontalRuleSpan.Mode
import me.saket.markdownrenderer.spans.IndentedCodeBlockSpan
import me.saket.markdownrenderer.spans.InlineCodeSpan
import me.saket.markdownrenderer.spans.MonospaceTypefaceSpan
import me.saket.markdownrenderer.spans.ParagraphLeadingMarginSpan
import me.saket.markdownrenderer.spans.StrikethroughSpan
import me.saket.markdownrenderer.spans.StyleSpan
import me.saket.markdownrenderer.spans.WysiwygSpan
import ru.noties.markwon.core.MarkwonTheme

/**
 * Pool for reusing spans instead of creating and throwing them on every text change.
 * TODO: Convert all these to extension functions?
 */
open class SpanPool : AbstractSpanPool() {
  private val horizontalRuleSpans = mutableMapOf<String, HorizontalRuleSpan>()

  open fun italics() =
    get { StyleSpan() }.apply {
      style = Typeface.ITALIC
    }

  open fun bold() =
    get { StyleSpan() }.apply {
      style = Typeface.BOLD
    }

  open fun foregroundColor(@ColorInt color: Int) =
    get { ForegroundColorSpan() }.apply {
      this.color = color
    }

  open fun inlineCode(markwonTheme: MarkwonTheme) =
    get { InlineCodeSpan() }.apply {
      theme = markwonTheme
    }

  open fun indentedCodeBlock(markwonTheme: MarkwonTheme) =
    get { IndentedCodeBlockSpan() }.apply {
      theme = markwonTheme
    }

  open fun strikethrough() =
    get { StrikethroughSpan() }

  open fun monospaceTypeface() =
    get { MonospaceTypefaceSpan() }

  open fun heading(
    level: Int,
    markwonTheme: MarkwonTheme
  ) =
    get { HeadingSpanWithLevel() }.apply {
      this.theme = markwonTheme
      this.level = level
    }

  open fun quote(markwonTheme: MarkwonTheme) =
    get { BlockQuoteSpan() }.apply {
      this.theme = markwonTheme
    }

  open fun leadingMargin(margin: Int) =
    get { ParagraphLeadingMarginSpan() }.apply {
      this.margin = margin
    }

  /**
   * @param text See [HorizontalRuleSpan.syntax].
   */
  open fun horizontalRule(
    text: CharSequence,
    @ColorInt ruleColor: Int,
    @Px ruleStrokeWidth: Int,
    mode: Mode
  ): HorizontalRuleSpan {
    val key = "${text}_${ruleColor}_${ruleStrokeWidth}_$mode"
    return when {
      horizontalRuleSpans.containsKey(key) -> horizontalRuleSpans.remove(key)!!
      else -> HorizontalRuleSpan(
          text, ruleColor, ruleStrokeWidth, mode
      )
    }
  }

  override fun recycle(span: WysiwygSpan) {
    when (span) {
      is HorizontalRuleSpan -> recycle(span)
      else -> super.recycle(span)
    }
  }

  private fun recycle(span: HorizontalRuleSpan) {
    val key = "${span.syntax}_${span.ruleColor}_${span.ruleStrokeWidth}_${span.mode}"
    horizontalRuleSpans[key] = span
  }
}