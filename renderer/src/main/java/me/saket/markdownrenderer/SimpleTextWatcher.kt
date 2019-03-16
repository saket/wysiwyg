package me.saket.markdownrenderer

import android.text.TextWatcher

internal abstract class SimpleTextWatcher : TextWatcher {

  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
}
