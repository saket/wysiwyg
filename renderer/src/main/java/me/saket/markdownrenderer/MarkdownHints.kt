package me.saket.markdownrenderer

import android.text.Editable
import android.view.View
import android.widget.EditText
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Highlights markdown syntax in real-time.
 *
 * Usage:
 * val markdownHints = new MarkdownHints(EditText, MarkdownParser))
 * editText.addTextChangedListener(markdownHints.textWatcher())
 */
class MarkdownHints(
    private val editText: EditText,
    private val parser: MarkdownParser
) : View.OnAttachStateChangeListener {

  private val bgExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  private val uiExecutor = UiThreadExecutor()

  init {
    editText.addOnAttachStateChangeListener(this)
  }

  override fun onViewAttachedToWindow(v: View) {}

  override fun onViewDetachedFromWindow(v: View) {
    bgExecutor.shutdownNow()
  }

  fun textWatcher(): SimpleTextWatcher {
    return object : SimpleTextWatcher() {
      override fun afterTextChanged(editable: Editable) {
        bgExecutor.submit {
          val spanWriter = parser.parseSpans(editable)

          uiExecutor.execute {
            editText.suspendTextWatcherAndRun(this) {
              parser.removeSpans(editable)
              spanWriter.writeTo(editable)
            }
          }
        }.get()
      }
    }
  }
}
