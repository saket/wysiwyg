package me.saket.wysiwyg.sample.extensions

import me.saket.wysiwyg.sample.BuildConfig

internal inline fun runCatchingOnRelease(block: () -> Unit) {
  try {
    block()
  } catch (e: Throwable) {
    if (BuildConfig.DEBUG) {
      throw e
    } else {
      e.printStackTrace()
    }
  }
}
