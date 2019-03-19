package me.saket.markdownrenderer

import android.widget.EditText
import me.saket.markdownrenderer.util.AfterTextChange
import me.saket.markdownrenderer.util.OnViewDetach
import me.saket.markdownrenderer.util.UiThreadExecutor
import me.saket.markdownrenderer.util.suspendTextWatcherAndRun
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Highlights markdown syntax in real-time.
 *
 * Usage:
 * val markdownHints = MarkdownHints(EditText, MarkdownParser))
 * editText.addTextChangedListener(markdownHints.textWatcher())
 */
class MarkdownHints(
    private val editText: EditText,
    private val parser: MarkdownParser
) {

  private val bgExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  private val uiExecutor = UiThreadExecutor()

  init {
    cleanupOnViewDetach()
  }

  private fun cleanupOnViewDetach() {
    editText.addOnAttachStateChangeListener(OnViewDetach {
      bgExecutor.shutdownNow()
    })
  }

  fun textWatcher() = AfterTextChange { editable, textWatcher ->
    bgExecutor.submit {
      val spanWriter = parser.parseSpans(editable)

      uiExecutor.execute {
        // Because the text is being parsed in background, it is possible that
        // the text is changing faster than they get processed.
        val isStale = editable.length != editText.text.length

        if (isStale.not()) {
          editText.suspendTextWatcherAndRun(textWatcher) {
            parser.removeSpans(editable)
            spanWriter.writeTo(editable)
          }
        }
      }
    }.get()
  }
}
