package me.saket.wysiwyg.render.spans

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.util.fastFirstOrNull
import android.graphics.Rect as AndroidRect

/**
 * Returns the vertical center of the glyph's ink bounds in this layout's coordinate space, or null
 * when the glyph or typeface can't be measured.
 */
internal fun TextLayoutResult.getInkCenterY(offset: Int): Float? {
  val glyph = layoutInput.text.getOrNull(offset) ?: return null
  val style = layoutInput.style
  val fontSizePx = with(layoutInput.density) { style.fontSize.toPx() }

  // Safe to call per-frame: Compose UI caches typeface resolutions internally.
  val typeface = layoutInput.fontFamilyResolver.resolveAsTypeface(
    fontFamily = style.fontFamily,
    fontWeight = style.fontWeight ?: FontWeight.Normal,
    fontStyle = style.fontStyle ?: FontStyle.Normal,
    fontSynthesis = style.fontSynthesis ?: FontSynthesis.All,
  )

  val relativeCenterY = InkCenterYCache.getOrMeasure(
    glyph = glyph,
    typeface = typeface.value,
    fontSizePx = fontSizePx,
    letterSpacing = style.letterSpacing,
    density = layoutInput.density,
  ) ?: return null

  val baseline = getLineBaseline(getLineForOffset(offset))
  return baseline + relativeCenterY
}

private object InkCenterYCache {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val rectBuffer = AndroidRect()
  private val cache = mutableListOf<InkCenterYCacheEntry>()

  fun getOrMeasure(
    glyph: Char,
    typeface: Typeface,
    fontSizePx: Float,
    letterSpacing: TextUnit,
    density: Density,
  ): Float? {
    val cached = cache.fastFirstOrNull { entry ->
      entry.key.matches(glyph, typeface, fontSizePx, letterSpacing)
    }
    if (cached != null) {
      return cached.centerY
    }

    val key = InkCenterYKey(
      glyph = glyph,
      typeface = typeface,
      fontSizePx = fontSizePx,
      letterSpacing = letterSpacing,
    )
    val centerY = measure(
      glyph = glyph,
      typeface = typeface,
      fontSizePx = fontSizePx,
      letterSpacing = letterSpacing,
      density = density,
    ) ?: return null
    cache += InkCenterYCacheEntry(key, centerY)
    return centerY
  }

  private fun measure(
    glyph: Char,
    typeface: Typeface,
    fontSizePx: Float,
    letterSpacing: TextUnit,
    density: Density,
  ): Float? {
    paint.apply {
      this.typeface = typeface
      this.textSize = fontSizePx
      this.setLetterSpacing(letterSpacing, density)
    }
    paint.getTextBounds(
      /* text = */ glyph.toString(),
      /* start = */ 0,
      /* end = */ 1,
      /* bounds = */ rectBuffer,
    )
    return if (rectBuffer.isEmpty) null else (rectBuffer.top + rectBuffer.bottom) / 2f
  }
}

private class InkCenterYKey(
  val glyph: Char,
  val typeface: Typeface,
  val fontSizePx: Float,
  val letterSpacing: TextUnit,
) {
  /**
   * Note to self: cache comparison doesn't use equals() because the call-site already have the
   * raw key components. Comparing them directly avoids allocating a temporary [InkCenterYKey].
   */
  fun matches(
    glyph: Char,
    typeface: Typeface,
    fontSizePx: Float,
    letterSpacing: TextUnit,
  ): Boolean {
    return this.glyph == glyph &&
        this.typeface === typeface &&
        this.fontSizePx == fontSizePx &&
        this.letterSpacing == letterSpacing
  }
}

private class InkCenterYCacheEntry(
  val key: InkCenterYKey,
  val centerY: Float,
)

private fun Paint.setLetterSpacing(letterSpacing: TextUnit, density: Density) {
  this.letterSpacing = when {
    !letterSpacing.isSpecified || textSize <= 0f -> 0f
    letterSpacing.isEm -> letterSpacing.value
    else -> with(density) { letterSpacing.toPx() / textSize }
  }
}
