package me.saket.markdownrenderer.spans

import me.saket.markdownrenderer.spans.pool.Recycler

class StrikethroughSpan(
  val recycler: Recycler
) : android.text.style.StrikethroughSpan(), WysiwygSpan {

  override fun recycle() {
    recycler(this)
  }
}