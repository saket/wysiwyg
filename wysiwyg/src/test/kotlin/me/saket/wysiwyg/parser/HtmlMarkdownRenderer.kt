@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package me.saket.wysiwyg.parser

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.internal.AnnotatedStringMarkdownRenderer
import org.robolectric.RuntimeEnvironment

internal fun MarkdownDocument.renderHtml(
  source: String,
  includeMonospaceTags: Boolean = false,
): String {
  val buffer = TextFieldState(source).toTextFieldBuffer()
  with(AnnotatedStringMarkdownRenderer(buffer, source, changes)) {
    FakeMarkdownRenderScope.render(this@renderHtml)
  }
  return HtmlMarkdownRenderer(
    source = source,
    annotations = buffer.outputTransformationAnnotations.orEmpty(),
    includeMonospaceTags = includeMonospaceTags,
  ).toHtml()
}

internal class HtmlMarkdownRenderer(
  private val source: String,
  annotations: List<AnnotatedString.Range<AnnotatedString.Annotation>>,
  includeMonospaceTags: Boolean,
) {
  private val tags = mutableListOf<TagInsertion>()
  private var sequence = 0

  init {
    val spanStyles = annotations.mapNotNull { annotation ->
      (annotation.item as? SpanStyle)?.let {
        StyleRange(style = it, range = TextRange(annotation.start, annotation.end))
      }
    }
    val paragraphStyles = annotations.mapNotNull { annotation ->
      (annotation.item as? ParagraphStyle)?.let {
        StyleRange(style = it, range = TextRange(annotation.start, annotation.end))
      }
    }

    emitParagraphTags(paragraphStyles)
    emitLinkTags(spanStyles)
    emitHeadingTags(spanStyles)
    emitBoldTags(spanStyles)
    emitStrikeThroughTags(spanStyles)
    if (includeMonospaceTags) {
      emitListMarkerTags(spanStyles, paragraphStyles)
    }
  }

  fun toHtml(): String {
    val sortedTags = tags.sortedWith(
      compareBy<TagInsertion> { it.offset }
        .thenBy { it.priority }
        .thenBy { it.sequence }
    )
    return buildString {
      var cursor = 0
      for ((offset, tag) in sortedTags) {
        append(source, cursor, offset)
        append(tag)
        cursor = offset
      }
      append(source, cursor, source.length)
    }
  }

  private fun emitParagraphTags(styles: List<StyleRange<ParagraphStyle>>) {
    val blockQuoteRanges = styles
      .filter { it.style.textIndent?.firstLine == TestWysiwygTheme.blockQuoteLeadingPadding }
      .map { it.range }
      .mergeAdjacentParagraphs()

    val listRanges = styles
      .filter { it.style.textIndent?.firstLine == TestWysiwygTheme.listBlockLeadingPadding }
      .map { it.range }
      .mergeAdjacentParagraphs()

    for (range in blockQuoteRanges) {
      emitTag("blockquote", range, isParagraph = true)
    }
    for (range in listRanges) {
      emitTag("list", range, isParagraph = true)
    }
  }

  private fun emitLinkTags(styles: List<StyleRange<SpanStyle>>) {
    val linkTextRanges = styles.filter { it.style.color == TestWysiwygTheme.linkTextColor }
    val linkUrlRanges = styles.filter { it.style.color == TestWysiwygTheme.linkUrlColor }

    for (textRange in linkTextRanges) {
      val urlRange = linkUrlRanges.firstOrNull { it.range.start > textRange.range.end }
        ?: continue

      // Production emits link text/url styles only after every link marker resolves. The snapshot
      // only needs the outer marker ranges to wrap the same visible span as the old readable tag.
      val textOpeningMarker = styles.markerColorRangeEndingAt(textRange.range.start) ?: continue
      val urlClosingMarker = styles.markerColorRangeStartingAt(urlRange.range.end) ?: continue

      emitTag(
        tag = "link",
        range = TextRange(textOpeningMarker.start, urlClosingMarker.end),
      )
    }
  }

  private fun List<StyleRange<SpanStyle>>.markerColorRangeStartingAt(offset: Int): TextRange? {
    for (style in this) {
      if (
        style.style.color == TestWysiwygTheme.markerColor &&
        style.range.start == offset
      ) {
        return style.range
      }
    }
    return null
  }

  private fun List<StyleRange<SpanStyle>>.markerColorRangeEndingAt(offset: Int): TextRange? {
    for (style in this) {
      if (
        style.style.color == TestWysiwygTheme.markerColor &&
        style.range.end == offset
      ) {
        return style.range
      }
    }
    return null
  }

  private fun emitHeadingTags(styles: List<StyleRange<SpanStyle>>) {
    for (style in styles) {
      val level = when (style.style.fontSize) {
        1.em * TestWysiwygTheme.headingFontSizes.h1 -> 1
        1.em * TestWysiwygTheme.headingFontSizes.h2 -> 2
        1.em * TestWysiwygTheme.headingFontSizes.h3 -> 3
        1.em * TestWysiwygTheme.headingFontSizes.h4 -> 4
        1.em * TestWysiwygTheme.headingFontSizes.h5 -> 5
        1.em * TestWysiwygTheme.headingFontSizes.h6 -> 6
        else -> null
      }
      if (
        level != null &&
        style.style.color == TestWysiwygTheme.headingColor
      ) {
        emitTag("h$level", style.range)
      }
    }
  }

  private fun emitBoldTags(styles: List<StyleRange<SpanStyle>>) {
    for (style in styles) {
      if (
        style.style.fontWeight == FontWeight.Bold &&
        style.style.color == Color.Unspecified &&
        style.style.fontSize == TextUnit.Unspecified
      ) {
        emitTag("b", style.range)
      }
    }
  }

  private fun emitStrikeThroughTags(styles: List<StyleRange<SpanStyle>>) {
    for (style in styles) {
      if (style.style.textDecoration == TextDecoration.LineThrough) {
        emitTag("s", style.range)
      }
    }
  }

  private fun emitListMarkerTags(
    spanStyles: List<StyleRange<SpanStyle>>,
    paragraphStyles: List<StyleRange<ParagraphStyle>>,
  ) {
    val listStarts = paragraphStyles
      .filter { it.style.textIndent?.firstLine == TestWysiwygTheme.listBlockLeadingPadding }
      .mapTo(mutableSetOf()) { it.range.start }

    for (style in spanStyles) {
      if (
        style.style.fontFamily == FontFamily.Monospace &&
        style.range.start in listStarts
      ) {
        emitTag("monospace", style.range)
      }
    }
  }

  private fun emitTag(
    tag: String,
    range: TextRange,
    isParagraph: Boolean = false,
  ) {
    val priority = if (isParagraph) 0 else 10
    tags += TagInsertion(range.start, "<$tag>", priority, sequence++)
    tags += TagInsertion(range.end, "</$tag>", priority, sequence++)
  }

  /**
   * Production emits paragraph styles per rendered paragraph/list item. The text snapshots use one
   * tag for a contiguous styled block so list and blockquote assertions stay readable. This only
   * merges already-emitted adjacent paragraph style ranges; it does not infer markdown structure.
   *
   * Example:
   *
   * ```
   * - first
   * - second
   * ```
   *
   * Production list styling is roughly two paragraph style ranges: one for `- first`, and one for
   * `- second`. Without merging, the snapshot would likely become:
   *
   * ```
   * <list>- first</list>
   * <list>- second</list>
   * ```
   *
   * With merging, it stays closer to the old readable assertion:
   *
   * ```
   * <list>- first
   * - second
   * </list>
   * ```
   */
  private fun List<TextRange>.mergeAdjacentParagraphs(): List<TextRange> {
    if (isEmpty()) return emptyList()

    val sorted = sortedBy { it.start }
    val merged = mutableListOf<TextRange>()
    var current = sorted.first()
    for (next in sorted.drop(1)) {
      if (next.start <= current.end + 1) {
        current = TextRange(current.start, maxOf(current.end, next.end))
      } else {
        merged += current
        current = next
      }
    }
    merged += current
    return merged
  }

}

private data class StyleRange<T>(
  val style: T,
  val range: TextRange,
)

private data class TagInsertion(
  val offset: Int,
  val tag: String,
  val priority: Int,
  val sequence: Int,
)

private val TestWysiwygTheme = WysiwygTheme(
  markerColor = Color(0xFF101011),
  linkTextColor = Color(0xFF202022),
  linkUrlColor = Color(0xFF303033),
  struckThroughTextColor = Color(0xFF404044),
  codeBackground = Color(0xFF505055),
  codeBlockLeadingPadding = 101.sp,
  blockQuoteTextColor = Color(0xFF606066),
  blockQuoteLeadingPadding = 102.sp,
  listBlockLeadingPadding = 103.sp,
  headingColor = Color(0xFF707077),
  headingFontSizes = WysiwygTheme.HeadingFontSizeMultipliers(
    h1 = 6f,
    h2 = 5f,
    h3 = 4f,
    h4 = 3f,
    h5 = 2f,
    h6 = 1f,
  )
)

private object FakeMarkdownRenderScope : MarkdownRenderScope, Density by Density(1f) {
  override val theme: WysiwygTheme = TestWysiwygTheme
  override val textStyle: TextStyle = TextStyle.Default
  override val textMeasurer: TextMeasurer by lazy {
    TextMeasurer(
      defaultFontFamilyResolver = createFontFamilyResolver(RuntimeEnvironment.getApplication()),
      defaultDensity = Density(1f),
      defaultLayoutDirection = LayoutDirection.Ltr,
      cacheSize = 0,
    )
  }

  override fun isInsideViewport(range: TextRange): Boolean = true
}
