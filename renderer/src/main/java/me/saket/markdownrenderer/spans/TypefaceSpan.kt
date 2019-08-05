package me.saket.markdownrenderer.spans

import android.text.style.TypefaceSpan
import me.saket.markdownrenderer.spans.pool.Recycler

class MonospaceTypefaceSpan(val recycler: Recycler) : TypefaceSpan("monospace"), WysiwygSpan {

  override fun recycle() {
    recycler(this)
  }
}