package me.saket.markdownrenderer.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.spans.pool.Recycler

/**
 * @param syntax Used for calculating the left offset to avoid drawing under the text.
 */
class ThematicBreakSpan(
  val theme: WysiwygTheme,
  val syntax: CharSequence,
  val mode: Mode,
  val recycler: Recycler
) : LineBackgroundSpan, WysiwygSpan {

  private var offsetForSyntax = -1f

  /**
   * @param topOffsetFactor Used for centering the rule with the text.
   */
  enum class Mode(val topOffsetFactor: Float) {
    HYPHENS(topOffsetFactor = 0.07f),
    ASTERISKS(topOffsetFactor = -0.11f),
    UNDERSCORES(topOffsetFactor = 0.42f)
  }

  override fun drawBackground(
    canvas: Canvas,
    paint: Paint,
    left: Int,
    right: Int,
    top: Int,
    baseline: Int,
    bottom: Int,
    ignored: CharSequence,
    start: Int,
    end: Int,
    lineNumber: Int
  ) {
    val originalPaintColor = paint.color
    paint.color = theme.thematicBreakColor
    paint.strokeWidth = theme.thematicBreakThickness

    if (offsetForSyntax == -1f) {
      offsetForSyntax = paint.measureText(syntax.toString())
    }

    val lineCenter = ((top + bottom) / 2 + paint.textSize * mode.topOffsetFactor).toInt()
    canvas.drawLine(
        left + offsetForSyntax, lineCenter.toFloat(), right.toFloat(), lineCenter.toFloat(), paint
    )

    paint.color = originalPaintColor
  }

  override fun recycle() {
    recycler(this)
  }
}
