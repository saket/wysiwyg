package me.saket.markdownrenderer.flexmark

import android.text.Spannable
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.vladsch.flexmark.Extension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.sequence.SubSequence
import me.saket.markdownrenderer.MarkdownParser
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.spans.WysiwygSpan
import me.saket.markdownrenderer.spans.pool.SpanPool
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Usage:
 * FlexmarkMarkdownParser(WysiwygTheme, MarkdownSpanPool)
 */
open class FlexmarkMarkdownParser(
  private val theme: WysiwygTheme,
  private val syntaxStylers: FlexmarkSyntaxStylers = FlexmarkSyntaxStylers(),
  private val pool: SpanPool = SpanPool()
) : MarkdownParser {

  private val markdownNodeTreeVisitor by lazy(NONE) { treeVisitor() }
  private val parser: Parser by lazy(NONE) { buildParser() }

  open fun treeVisitor() =
    FlexmarkNodeTreeVisitor(syntaxStylers, theme, pool)

  open fun buildParser(): Parser =
    Parser.builder()
        .extensions(supportedParserExtensions())
        .build()

  open fun supportedParserExtensions() =
    listOf<Extension>(StrikethroughExtension.create())

  @WorkerThread
  override fun parseSpans(text: Spannable): SpanWriter {
    // Instead of creating immutable CharSequences, Flexmark uses SubSequence that
    // maintains a mutable text and changes its visible window whenever a new
    // text is required to reduce object creation. SubSequence.of() internally skips
    // creating a new object if the text is already a SubSequence. This leads to bugs
    // that are hard to track. For instance, try adding a new line break at the end
    // and then delete it. It'll result in a crash because ThematicBreakSpan keeps
    // a reference to the mutable text. When the underlying text is trimmed, the
    // bounds (start, end) become larger than the actual text.
    val immutableText = SubSequence.of(text.toString())

    val spanWriter = SpanWriter()
    val markdownRootNode = parser.parse(immutableText)
    markdownNodeTreeVisitor.visit(markdownRootNode, spanWriter)
    return spanWriter
  }

  /**
   * Called on every text change so that stale spans can
   * be removed before applying new ones.
   */
  @UiThread
  override fun removeSpans(text: Spannable) {
    val spans = text.getSpans(0, text.length, Any::class.java)
    for (span in spans) {
      if (span is WysiwygSpan) {
        text.removeSpan(span)
        span.recycle()
      }
    }
  }
}
