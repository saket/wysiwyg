package me.saket.markdownrenderer.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan
import androidx.annotation.Px

/**
 * @param syntax Used for calculating the left offset to avoid drawing under the text.
 */
class HorizontalRuleSpan(
  val syntax: CharSequence,
  @Px val ruleColor: Int,
  @Px val ruleStrokeWidth: Int,
  val mode: Mode
) : LineBackgroundSpan, WysiwygSpan {

  private var offsetForSyntax = -1f

  enum class Mode {
    HYPHENS {
      override val topOffsetFactor = 0.07f
    },

    ASTERISKS {
      override val topOffsetFactor = -0.11f
    },

    UNDERSCORES {
      override val topOffsetFactor = 0.42f
    };

    /**
     * Used for centering the rule with the text.
     */
    open val topOffsetFactor: Float
      get() = throw UnsupportedOperationException()
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
    paint.color = ruleColor
    paint.strokeWidth = ruleStrokeWidth.toFloat()

    if (offsetForSyntax == -1f) {
      offsetForSyntax = paint.measureText(syntax.toString())
    }

    val lineCenter = ((top + bottom) / 2 + paint.textSize * mode.topOffsetFactor).toInt()
    canvas.drawLine(
        left + offsetForSyntax, lineCenter.toFloat(), right.toFloat(), lineCenter.toFloat(), paint
    )

    paint.color = originalPaintColor
  }
}
