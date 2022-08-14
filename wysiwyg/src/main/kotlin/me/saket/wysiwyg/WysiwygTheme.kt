package me.saket.wysiwyg

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit

data class WysiwygTheme(
  val syntaxColor: Color,
  val linkTextColor: Color,
  val linkUrlColor: Color,
  val struckThroughTextColor: Color,
  val spoilersTextColor: Color,
  val spoilersBackground: Color,
  val codeBackground: Color,
  val codeBlockLeadingPadding: TextUnit,
  val blockQuoteText: Color,
  val blockQuoteLeadingPadding: TextUnit,
  val listBlockLeadingPadding: TextUnit,
  val headingColor: Color,
  val headingFontSizes: HeadingFontSizeMultipliers = HeadingFontSizeMultipliers(),
) {

  data class HeadingFontSizeMultipliers(
    val h1: Float = 1.4f,
    val h2: Float = 1.27f,
    val h3: Float = 1.13f,
    val h4: Float = 1f,
    val h5: Float = 1f,
    val h6: Float = 1f,
  )
}
