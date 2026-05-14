package me.saket.wysiwyg

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.drewhamilton.poko.Poko

@Poko
class WysiwygTheme(
  val markerColor: Color,
  val linkTextColor: Color,
  val linkUrlColor: Color,
  val struckThroughTextColor: Color,
  val codeBackground: Color,
  val codeBlockLeadingPadding: TextUnit,
  val blockQuoteTextColor: Color,
  val blockQuoteLeadingPadding: TextUnit,
  val listBlockLeadingPadding: TextUnit,
  val headingColor: Color,
  val headingFontSizes: HeadingFontSizeMultipliers = HeadingFontSizeMultipliers(),
) {
  @Poko
  class HeadingFontSizeMultipliers(
    val h1: Float = 1.4f,
    val h2: Float = 1.27f,
    val h3: Float = 1.13f,
    val h4: Float = 1f,
    val h5: Float = 1f,
    val h6: Float = 1f,
  )

  companion object {
    val GithubLight: WysiwygTheme = WysiwygTheme(
      markerColor = Color(0xFF57606A),
      linkTextColor = Color(0xFF0969DA),
      linkUrlColor = Color(0xFF6E7781),
      struckThroughTextColor = Color(0xFF6E7781),
      codeBackground = Color(0x33AFB8C1),
      codeBlockLeadingPadding = 16.sp,
      blockQuoteTextColor = Color(0xFF59636E),
      blockQuoteLeadingPadding = 16.sp,
      listBlockLeadingPadding = 16.sp,
      headingColor = Color(0xFF1F2328),
    )

    val GithubDark: WysiwygTheme = WysiwygTheme(
      markerColor = Color(0xFF7D8590),
      linkTextColor = Color(0xFF2F81F7),
      linkUrlColor = Color(0xFF7D8590),
      struckThroughTextColor = Color(0xFF7D8590),
      codeBackground = Color(0x666E7681),
      codeBlockLeadingPadding = 16.sp,
      blockQuoteTextColor = Color(0xFF8D96A0),
      blockQuoteLeadingPadding = 16.sp,
      listBlockLeadingPadding = 16.sp,
      headingColor = Color(0xFFE6EDF3),
    )
  }
}
