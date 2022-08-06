package me.saket.wysiwyg.internal

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/** Copied from [androidx.compose.ui.util.fastForEach]. */
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
  contract { callsInPlace(action) }
  for (index in indices) {
    val item = get(index)
    action(item)
  }
}

@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastForEachReverseIndexed(action: (Int, T) -> Unit) {
  contract { callsInPlace(action) }
  for (index in indices.reversed()) {
    val item = get(index)
    action(index, item)
  }
}
