package me.saket.markdownrenderer.flexmark

import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import com.vladsch.flexmark.Extension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.sequence.SubSequence
import me.saket.markdownrenderer.MarkdownHintStyles
import me.saket.markdownrenderer.MarkdownHintsSpanWriter
import me.saket.markdownrenderer.MarkdownParser
import me.saket.markdownrenderer.MarkdownSpanPool
import me.saket.markdownrenderer.spans.HeadingSpanWithLevel
import me.saket.markdownrenderer.spans.HorizontalRuleSpan
import me.saket.markdownrenderer.spans.IndentedCodeBlockSpan
import me.saket.markdownrenderer.spans.InlineCodeSpan
import ru.noties.markwon.core.spans.BlockQuoteSpan
import java.util.HashSet

/**
 * Usage:
 * FlexmarkMarkdownParser(MarkdownHintStyles(context), MarkdownSpanPool())
 */
class FlexmarkMarkdownParser(
    styles: MarkdownHintStyles,
    private val spanPool: MarkdownSpanPool
) : MarkdownParser {

  private val markdownNodeTreeVisitor = MarkdownNodeTreeVisitor(spanPool, styles)

  private val parser: Parser = Parser.builder()
      .extensions(listOf<Extension>(StrikethroughExtension.create()))
      .build()

  override fun parseSpans(text: Spannable): MarkdownHintsSpanWriter {
    // Instead of creating immutable CharSequences, Flexmark uses SubSequence that
    // maintains a mutable text and changes its visible window whenever a new
    // text is required to reduce object creation. SubSequence.of() internally skips
    // creating a new object if the text is already a SubSequence. This leads to bugs
    // that are hard to track. For instance, try adding a new line break at the end
    // and then delete it. It'll result in a crash because HorizontalRuleSpan keeps
    // a reference to the mutable text. When the underlying text is trimmed, the
    // bounds (start, end) become larger than the actual text.
    val immutableText = SubSequence.of(text.toString())

    val spanWriter = MarkdownHintsSpanWriter()
    val markdownRootNode = parser.parse(immutableText)
    markdownNodeTreeVisitor.visit(markdownRootNode, spanWriter)
    return spanWriter
  }

  /**
   * Called on every text change so that stale spans can
   * be removed before applying new ones.
   */
  override fun removeSpans(text: Spannable) {
    val spans = text.getSpans(0, text.length, Any::class.java)
    for (span in spans) {
      if (span.javaClass in FlexmarkMarkdownParser.SUPPORTED_MARKDOWN_SPANS) {
        text.removeSpan(span)
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
  }
}
