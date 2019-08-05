package me.saket.markdownrenderer

import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.Px
import io.noties.markwon.core.MarkwonTheme

/**
 * Colors and dimensions to use for highlighting markdown syntax.
 */
data class WysiwygTheme(

  /** Used for resolving default colors and dimensions in [MarkwonTheme.builderWithDefaults]. */
  val context: Context,

  /** Color used for highlighting '**', '~~' and other syntax characters. */
  @ColorInt
  val syntaxColor: Int = color(context, R.color.markdown_syntax),

  @ColorInt
  val blockQuoteIndentationRuleColor: Int = color(
      context, R.color.markdown_blockquote_indentation_rule
  ),

  @ColorInt
  val blockQuoteTextColor: Int = color(context, R.color.markdown_blockquote_text),

  /** Width of a block-quote's vertical line/stripe/rule. */
  @Px
  val blockQuoteVerticalRuleStrokeWidth: Int = dimensPx(
      context, R.dimen.markdown_blockquote_vertical_rule_stroke_width
  ),

  /** Gap before a block of ordered/unordered list. */
  @Px
  val listBlockIndentationMargin: Int = dimensPx(
      context, R.dimen.markdown_text_block_indentation_margin
  ),

  @ColorInt
  val linkUrlColor: Int = color(context, R.color.markdown_link_url),

  @ColorInt
  val linkTextColor: Int = color(context, R.color.markdown_link_text),

  @ColorInt
  val thematicBreakColor: Int = color(context, R.color.markdown_thematic_break),

  @Px
  val thematicBreakThickness: Int = dimensPx(context, R.dimen.markdown_thematic_break_thickness),

  @ColorInt
  val codeBackgroundColor: Int = color(context, R.color.markdown_code_background),

  @Px
  val codeBlockMargin: Int = dimensPx(context, R.dimen.markdown_code_block_margin)
) {
  val markwonTheme = MarkwonTheme.builderWithDefaults(context)
      .headingBreakHeight(0)
      .blockQuoteColor(blockQuoteIndentationRuleColor)
      .blockQuoteWidth(blockQuoteVerticalRuleStrokeWidth)
      .blockMargin(listBlockIndentationMargin)
      .codeBackgroundColor(codeBackgroundColor)
      .codeBlockMargin(codeBlockMargin)
      .build()
}

@Suppress("DEPRECATION")
private val color = { context: Context, colorResId: Int ->
  when {
    Build.VERSION.SDK_INT >= 23 -> context.resources.getColor(colorResId, context.theme)
    else -> context.resources.getColor(colorResId)
  }
}

private val dimensPx = { context: Context, dimenResId: Int ->
  context.resources.getDimensionPixelSize(dimenResId)
}
