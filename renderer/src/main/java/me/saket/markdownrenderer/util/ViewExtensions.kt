package me.saket.markdownrenderer.util

import android.text.TextWatcher
import android.widget.TextView

fun TextView.suspendTextWatcherAndRun(watcher: TextWatcher, runnable: () -> Unit) {
  removeTextChangedListener(watcher)
  runnable()
  addTextChangedListener(watcher)
}
