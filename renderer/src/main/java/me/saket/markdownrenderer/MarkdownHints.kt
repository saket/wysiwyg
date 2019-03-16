package me.saket.markdownrenderer

import android.text.Editable
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.widget.EditText
import com.vladsch.flexmark.Extension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.sequence.SubSequence
import me.saket.markdownrenderer.spans.HeadingSpanWithLevel
import me.saket.markdownrenderer.spans.HorizontalRuleSpan
import me.saket.markdownrenderer.spans.IndentedCodeBlockSpan
import me.saket.markdownrenderer.spans.InlineCodeSpan
import ru.noties.markwon.spans.BlockQuoteSpan
import timber.log.Timber
import java.util.HashSet

/**
 * Usage: EditText#addTextChangedListener(new MarkdownHints(EditText, HighlightOptions, SpanPool));
 */
class MarkdownHints(
    private val editText: EditText,
    private val markdownHintOptions: MarkdownHintOptions,
    private val spanPool: MarkdownSpanPool
) : SimpleTextWatcher() {

  private val markdownNodeTreeVisitor: MarkdownNodeTreeVisitor = MarkdownNodeTreeVisitor(spanPool, markdownHintOptions)
  private val markdownHintsSpanWriter: MarkdownHintsSpanWriter = MarkdownHintsSpanWriter()
  private val parser: Parser = Parser.builder()
      .extensions(listOf<Extension>(StrikethroughExtension.create()))
      .build()

  override fun afterTextChanged(editable: Editable) {
    editText.removeTextChangedListener(this)

    // We'll see stale styling if previous spans aren't removed.
    removeHintSpans(editable)

    val markdownRootNode = parser.parse(SubSequence.of(editable))
    markdownHintsSpanWriter.setText(editable)
    markdownNodeTreeVisitor.visit(markdownRootNode, markdownHintsSpanWriter)

    editText.addTextChangedListener(this)
  }

  private fun removeHintSpans(spannable: Spannable) {
    val spans = spannable.getSpans(0, spannable.length, Any::class.java)
    for (span in spans) {
      if (SUPPORTED_MARKDOWN_SPANS.contains(span.javaClass)) {
        spannable.removeSpan(span)
        spanPool.recycle(span)
      }
    }
  }

  companion object {
    val SUPPORTED_MARKDOWN_SPANS: MutableSet<Any> = HashSet()

    init {
      SUPPORTED_MARKDOWN_SPANS.add(StyleSpan::class.java)
      SUPPORTED_MARKDOWN_SPANS.add(ForegroundColorSpan::class.java)
      SUPPORTED_MARKDOWN_SPANS.add(StrikethroughSpan::class.java)
      SUPPORTED_MARKDOWN_SPANS.add(TypefaceSpan::class.java)
      SUPPORTED_MARKDOWN_SPANS.add(HeadingSpanWithLevel::class.java)
      SUPPORTED_MARKDOWN_SPANS.add(SuperscriptSpan::class.java)
      SUPPORTED_MARKDOWN_SPANS.add(BlockQuoteSpan::class.java)
      SUPPORTED_MARKDOWN_SPANS.add(LeadingMarginSpan.Standard::class.java)
      SUPPORTED_MARKDOWN_SPANS.add(HorizontalRuleSpan::class.java)
      SUPPORTED_MARKDOWN_SPANS.add(InlineCodeSpan::class.java)
      SUPPORTED_MARKDOWN_SPANS.add(IndentedCodeBlockSpan::class.java)
    }

    fun enableLogging() {
      Timber.plant(Timber.DebugTree())
    }
  }
}
