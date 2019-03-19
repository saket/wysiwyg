package me.saket.markdownrenderer.util

import android.view.View

class OnViewDetach(private val onDetach: () -> Unit) : View.OnAttachStateChangeListener {

  override fun onViewDetachedFromWindow(v: View) {
    onDetach()
  }

  override fun onViewAttachedToWindow(v: View) {}
}
