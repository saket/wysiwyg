package me.saket.wysiwyg.parser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.internal.TextFieldLayoutInfo
import me.saket.wysiwyg.internal.TextFieldViewport

// todo: kdoc
interface MarkdownRenderScope : Density {
  val theme: WysiwygTheme
  val textMeasurer: TextMeasurer
  val textStyle: TextStyle

  // todo: kdoc
  fun isInsideViewport(range: TextRange): Boolean
}

@Composable
internal fun rememberMarkdownRenderScope(
  theme: WysiwygTheme,
  textStyle: TextStyle,
  layoutInfo: TextFieldLayoutInfo,
): RealMarkdownRenderScope {
  val density = LocalDensity.current
  return remember(density) {
    RealMarkdownRenderScope(density)
  }.also {
    it.theme = theme
    it.textStyle = textStyle
    it.viewport = layoutInfo.currentViewport()
    it.textMeasurer = rememberTextMeasurer(cacheSize = 100)
  }
}

internal class RealMarkdownRenderScope(
  density: Density,
) : MarkdownRenderScope, Density by density {
  override lateinit var theme: WysiwygTheme
  override lateinit var textMeasurer: TextMeasurer
  internal lateinit var viewport: TextFieldViewport
  override lateinit var textStyle: TextStyle

  override fun isInsideViewport(range: TextRange): Boolean {
    return viewport.intersects(range, includeBeyondViewport = true)
  }
}
