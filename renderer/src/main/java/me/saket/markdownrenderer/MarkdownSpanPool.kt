package me.saket.markdownrenderer

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import androidx.annotation.ColorInt
import androidx.annotation.Px
import me.saket.markdownrenderer.spans.HeadingSpanWithLevel
import me.saket.markdownrenderer.spans.HorizontalRuleSpan
import me.saket.markdownrenderer.spans.IndentedCodeBlockSpan
import me.saket.markdownrenderer.spans.InlineCodeSpan
import ru.noties.markwon.core.MarkwonTheme
import ru.noties.markwon.core.spans.BlockQuoteSpan
import java.util.HashMap
import java.util.Stack

/**
 * Pool for reusing spans instead of creating and throwing them on every text change.
 */
@SuppressLint("UseSparseArrays")
open class MarkdownSpanPool {

  private val italicsSpans = Stack<StyleSpan>()
  private val boldSpans = Stack<StyleSpan>()
  private val strikethroughSpans = Stack<StrikethroughSpan>()
  private val monospaceTypefaceSpans = Stack<TypefaceSpan>()
  private val foregroundColorSpans = HashMap<Int, ForegroundColorSpan>()
  private val inlineCodeSpans = Stack<InlineCodeSpan>()
  private val indentedCodeSpans = Stack<IndentedCodeBlockSpan>()
  private val headingSpans = HashMap<Int, HeadingSpanWithLevel>()
  private val superscriptSpans = Stack<SuperscriptSpan>()
  private val quoteSpans = Stack<BlockQuoteSpan>()
  private val leadingMarginSpans = HashMap<Int, LeadingMarginSpan.Standard>()
  private val horizontalRuleSpans = HashMap<String, HorizontalRuleSpan>()

  open fun italics(): Any {
    return when {
      italicsSpans.empty() -> StyleSpan(Typeface.ITALIC)
      else -> italicsSpans.pop()
    }
  }

  open fun bold(): Any {
    return when {
      boldSpans.empty() -> StyleSpan(Typeface.BOLD)
      else -> boldSpans.pop()
    }
  }

  open fun foregroundColor(@ColorInt color: Int): Any {
    return when {
      foregroundColorSpans.containsKey(color) -> foregroundColorSpans.remove(color)!!
      else -> ForegroundColorSpan(color)
    }
  }

  open fun inlineCode(markwonTheme: MarkwonTheme): Any {
    return when {
      inlineCodeSpans.empty() -> InlineCodeSpan(markwonTheme)
      else -> inlineCodeSpans.pop()
    }
  }

  open fun indentedCodeBlock(markwonTheme: MarkwonTheme): Any {
    return when {
      indentedCodeSpans.empty() -> IndentedCodeBlockSpan(markwonTheme)
      else -> indentedCodeSpans.pop()
    }
  }

  open fun strikethrough(): Any {
    return when {
      strikethroughSpans.empty() -> StrikethroughSpan()
      else -> strikethroughSpans.pop()
    }
  }

  open fun monospaceTypeface(): Any {
    return when {
      monospaceTypefaceSpans.empty() -> TypefaceSpan("monospace")
      else -> monospaceTypefaceSpans.pop()
    }
  }

  open fun heading(level: Int, markwonTheme: MarkwonTheme): Any {
    return when {
      headingSpans.containsKey(level) -> headingSpans.remove(level)!!
      else -> HeadingSpanWithLevel(markwonTheme, level)
    }
  }

  open fun superscript(): Any {
    return when {
      superscriptSpans.empty() -> SuperscriptSpan()
      else -> superscriptSpans.pop()
    }
  }

  open fun quote(markwonTheme: MarkwonTheme): Any {
    return when {
      quoteSpans.empty() -> BlockQuoteSpan(markwonTheme)
      else -> quoteSpans.pop()
    }
  }

  open fun leadingMargin(margin: Int): Any {
    return when {
      leadingMarginSpans.containsKey(margin) -> leadingMarginSpans.remove(margin)!!
      else -> LeadingMarginSpan.Standard(margin)
    }
  }

  /**
   * @param text See [HorizontalRuleSpan.HorizontalRuleSpan].
   */
  open fun horizontalRule(
    text: CharSequence,
    @ColorInt ruleColor: Int,
    @Px ruleStrokeWidth: Int,
    mode: HorizontalRuleSpan.Mode
  ): Any {
    val key = text.toString() + "_" + ruleColor + "_" + ruleStrokeWidth + "_" + mode
    return when {
      horizontalRuleSpans.containsKey(key) -> horizontalRuleSpans.remove(key)!!
      else -> HorizontalRuleSpan(text, ruleColor, ruleStrokeWidth, mode)
    }
  }

  open fun recycle(span: Any) {
    when (span) {
      is StyleSpan -> recycle(span)
      is ForegroundColorSpan -> recycle(span)
      is StrikethroughSpan -> recycle(span)
      is TypefaceSpan -> recycle(span)
      is HeadingSpanWithLevel -> recycle(span)
      is SuperscriptSpan -> recycle(span)
      is BlockQuoteSpan -> recycle(span)
      is LeadingMarginSpan.Standard -> recycle(span)
      is HorizontalRuleSpan -> recycle(span)
      is IndentedCodeBlockSpan -> recycle(span)
      is InlineCodeSpan -> recycle(span)
      else -> throw UnsupportedOperationException("Unknown span: " + span.javaClass.simpleName)
    }
  }

  private fun recycle(span: StyleSpan) {
    when {
      span.style == Typeface.ITALIC -> italicsSpans.push(span)
      span.style == Typeface.BOLD -> boldSpans.add(span)
      else -> throw UnsupportedOperationException("Only italics and bold spans supported.")
    }
  }

  private fun recycle(span: ForegroundColorSpan) {
    val key = span.foregroundColor
    foregroundColorSpans[key] = span
  }

  private fun recycle(span: InlineCodeSpan) {
    inlineCodeSpans.push(span)
  }

  private fun recycle(span: IndentedCodeBlockSpan) {
    indentedCodeSpans.push(span)
  }

  private fun recycle(span: StrikethroughSpan) {
    strikethroughSpans.push(span)
  }

  private fun recycle(span: TypefaceSpan) {
    if (span.family != "monospace") {
      throw UnsupportedOperationException("Only monospace typeface spans exist in this pool.")
    }
    monospaceTypefaceSpans.push(span)
  }

  private fun recycle(span: HeadingSpanWithLevel) {
    headingSpans[span.level] = span
  }

  private fun recycle(span: SuperscriptSpan) {
    superscriptSpans.push(span)
  }

  private fun recycle(span: BlockQuoteSpan) {
    quoteSpans.push(span)
  }

  private fun recycle(span: LeadingMarginSpan.Standard) {
    val key = span.getLeadingMargin(true /* irrelevant */)
    leadingMarginSpans[key] = span
  }

  private fun recycle(span: HorizontalRuleSpan) {
    val key = span.text.toString() + "_" + span.ruleColor + "_" + span.ruleStrokeWidth + "_" + span.mode
    horizontalRuleSpans[key] = span
  }
}
