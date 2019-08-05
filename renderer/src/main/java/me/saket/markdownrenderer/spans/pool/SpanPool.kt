package me.saket.markdownrenderer.spans.pool

import android.graphics.Typeface
import androidx.annotation.ColorInt
import me.saket.markdownrenderer.spans.BlockQuoteSpan
import me.saket.markdownrenderer.spans.ForegroundColorSpan
import me.saket.markdownrenderer.spans.HeadingSpanWithLevel
import me.saket.markdownrenderer.spans.IndentedCodeBlockSpan
import me.saket.markdownrenderer.spans.InlineCodeSpan
import me.saket.markdownrenderer.spans.MonospaceTypefaceSpan
import me.saket.markdownrenderer.spans.ParagraphLeadingMarginSpan
import me.saket.markdownrenderer.spans.StrikethroughSpan
import me.saket.markdownrenderer.spans.StyleSpan
import ru.noties.markwon.core.MarkwonTheme

/**
 * Pool for reusing spans instead of creating and throwing them on every text change.
 * TODO: Convert all these to extension functions?
 */
open class SpanPool : AbstractSpanPool() {

  private val recycler: Recycler = { wysiwygSpan -> recycle(wysiwygSpan) }

  open fun italics() =
    get { StyleSpan(recycler) }.apply {
      style = Typeface.ITALIC
    }

  open fun bold() =
    get { StyleSpan(recycler) }.apply {
      style = Typeface.BOLD
    }

  open fun foregroundColor(@ColorInt color: Int) =
    get { ForegroundColorSpan(recycler) }.apply {
      this.color = color
    }

  open fun inlineCode(markwonTheme: MarkwonTheme) =
    get { InlineCodeSpan(recycler) }.apply {
      theme = markwonTheme
    }

  open fun indentedCodeBlock(markwonTheme: MarkwonTheme) =
    get { IndentedCodeBlockSpan(recycler) }.apply {
      theme = markwonTheme
    }

  open fun strikethrough() =
    get { StrikethroughSpan(recycler) }

  open fun monospaceTypeface() =
    get { MonospaceTypefaceSpan(recycler) }

  open fun heading(
    level: Int,
    markwonTheme: MarkwonTheme
  ) =
    get { HeadingSpanWithLevel(recycler) }.apply {
      this.theme = markwonTheme
      this.level = level
    }

  open fun quote(markwonTheme: MarkwonTheme) =
    get { BlockQuoteSpan(recycler) }.apply {
      this.theme = markwonTheme
    }

  open fun leadingMargin(margin: Int) =
    get { ParagraphLeadingMarginSpan(recycler) }.apply {
      this.margin = margin
    }
}