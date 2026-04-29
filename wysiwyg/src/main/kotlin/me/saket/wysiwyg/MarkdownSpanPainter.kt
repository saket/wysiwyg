package me.saket.wysiwyg

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult

// todo: kdoc
fun interface MarkdownSpanPainter {
  fun DrawScope.draw(layoutResult: TextLayoutResult)
}
