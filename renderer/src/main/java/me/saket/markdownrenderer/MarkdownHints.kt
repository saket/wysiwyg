package me.saket.markdownrenderer

import android.text.Editable
import android.view.View
import android.widget.EditText
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Usage: EditText#addTextChangedListener(new MarkdownHints(EditText, HighlightOptions, SpanPool));
 */
class MarkdownHints(
    private val editText: EditText,
    private val parser: MarkdownParser
) : SimpleTextWatcher(), View.OnAttachStateChangeListener {

  private val bgExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  private val uiExecutor = UiThreadExecutor()

  init {
    editText.addOnAttachStateChangeListener(this)
  }

  override fun onViewAttachedToWindow(v: View) {}

  override fun onViewDetachedFromWindow(v: View) {
    bgExecutor.shutdownNow()
  }

  override fun afterTextChanged(editable: Editable) {
    editText.removeTextChangedListener(this)

    bgExecutor.submit {
      val spanWriter = parser.parseSpans(editable)

      uiExecutor.execute {
        parser.removeSpans(editable)
        spanWriter.writeTo(editable)

        editText.addTextChangedListener(this)
      }
    }.get()
  }
}
