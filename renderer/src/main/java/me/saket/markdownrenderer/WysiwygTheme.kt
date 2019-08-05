package me.saket.markdownrenderer

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.Px

/**
 * Colors and dimensions to use for highlighting markdown syntax.
 */
data class WysiwygTheme(

  /** Used for resolving default colors and dimensions. */
  val context: Context,

  /** Color used for highlighting '**', '~~' and other syntax characters. */
  @ColorInt
  val syntaxColor: Int = color("#78909C"),

  @ColorInt
  val blockQuoteVerticalRuleColor: Int = color("#78909C"),

  @ColorInt
  val blockQuoteTextColor: Int = color("#9E9E9E"),

  /** Width of a block-quote's vertical line/stripe/rule. */
  @Px
  val blockQuoteVerticalRuleStrokeWidth: Int = dip(context, 4).toInt(),

  /** Gap before a block-quote. */
  @Px
  val blockQuoteIndentationMargin: Int = dip(context, 24).toInt(),

  /** Gap before a block of ordered/unordered list. */
  @Px
  val listBlockIndentationMargin: Int = dip(context, 24).toInt(),

  @ColorInt
  val linkUrlColor: Int = color("#9E9E9E"),

  @ColorInt
  val linkTextColor: Int = color("#1DE9B6"),

  /** Thematic break a.k.a. horizontal rule. */
  @ColorInt
  val thematicBreakColor: Int = color("#616161"),

  @Px
  val thematicBreakThickness: Float = dip(context, 4),

  @ColorInt
  val codeBackgroundColor: Int = color("#424242"),

  @Px
  val codeBlockMargin: Int = dip(context, 8).toInt()
)

private fun color(hex: String) = Color.parseColor(hex)

private fun dip(context: Context, @Px px: Int): Float =
  TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      px.toFloat(),
      context.resources.displayMetrics
  )
