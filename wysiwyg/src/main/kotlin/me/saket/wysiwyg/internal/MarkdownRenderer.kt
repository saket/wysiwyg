package me.saket.wysiwyg.internal

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import me.saket.wysiwyg.MarkdownSpanPainter
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.parser.LocalTextRange
import me.saket.wysiwyg.parser.MarkdownAnnotatedString
import me.saket.wysiwyg.parser.MarkdownChildNode
import me.saket.wysiwyg.parser.MarkdownDocument
import me.saket.wysiwyg.parser.TextChangeListSnapshot
import me.saket.wysiwyg.parser.editsOverlap
import me.saket.wysiwyg.parser.rebased

internal class MarkdownRenderer(
  private val theme: WysiwygTheme,
  private val useTestTags: Boolean = false,
) {
  fun buildAnnotatedString(
    text: AnnotatedString,
    document: MarkdownDocument
  ): MarkdownAnnotatedString {
    val scope = RealMarkdownNodeRenderScope(
      theme = theme,
      unstyledText = text,
      changes = document.changes,
      offsetInRoot = 0,
      useTestTags = useTestTags,
      spanPainters = mutableListOf(),
    )
    return MarkdownAnnotatedString(
      text = buildAnnotatedString {
        // Discard any previous styles that may have gotten restored after a config change.
        // This is slightly unfortunate because any spans added by user will also be discarded.
        this.append(text.text)
        with(document) {
          scope.render(text = this@buildAnnotatedString)
        }
      },
      extraSpanPainters = scope.spanPainters,
    )
  }
}

// todo: kdoc
interface MarkdownNodeRenderScope {
  val theme: WysiwygTheme
  val unstyledText: AnnotatedString

  // todo: kdoc
  val changes: List<TextChangeListSnapshot>

  // todo: kdoc
  val offsetInRoot: Int

  /** Whether [addTestTag] should record annotations. Tests opt in; production leaves this off. */
  val useTestTags: Boolean

  // todo: kdoc
  fun childScope(child: MarkdownChildNode): MarkdownNodeRenderScope

  /**
   * Resolves this local range to its absolute position in the rendered text.
   *
   * When [dropOnEdit] is true, returns `null` if any edit overlaps this range. The caller's
   * `?: return` then drops the node's styling for one frame until the reparse arrives. Use
   * it for fixed-shape markers (emphasis's `**`, a link's `]`, a list item's `-`, a
   * blockquote's `>`) where any edit invalidates the syntax. Skip it for repeatable markers
   * like a heading's `#`s.
   */
  fun LocalTextRange.resolve(dropOnEdit: Boolean = false): TextRange? {
    val rangeInRoot = TextRange(
      start = textRange.start + offsetInRoot,
      end = textRange.end + offsetInRoot,
    )
    return if (dropOnEdit && changes.editsOverlap(rangeInRoot)) {
      null
    } else {
      rangeInRoot.rebased(changes)
    }
  }

  // todo: kdoc
  fun MarkdownChildNode.render(text: AnnotatedString.Builder) {
    val childScope = childScope(this)
    with(node) {
      childScope.render(text)
    }
  }

  fun AnnotatedString.Builder.addStyle(style: SpanStyle, range: TextRange) {
    addStyle(
      style = style,
      start = range.start.coerceAtMost(unstyledText.lastIndex),
      end = range.end.coerceAtMost(length),
    )
  }

  fun AnnotatedString.Builder.addStyle(
    style: ParagraphStyle,
    range: TextRange,
    trimVerticalPadding: Boolean,
  ) {
    addStyle(
      style = style,
      start = range.start.coerceAtMost(unstyledText.lastIndex),
      end = range.end.coerceAtMost(length),
    )
    // Compose adds one extra empty line at every ParagraphStyle slice boundary on top of
    // whatever the source markdown already produces. Shrink the boundary newline's own
    // line height to nearly zero so the styled block sits flush against its neighbour,
    // matching what a plain TextField would render. Author-intended blank-line separators
    // are preserved because they contribute a *second* `\n` that we don't touch.
    // https://issuetracker.google.com/u/1/issues/241426911
    if (trimVerticalPadding) {
      if (unstyledText.getOrNull(range.start - 1) == '\n') {
        addStyle(TinyParagraphStyle, start = range.start - 1, end = range.start)
      }
      if (unstyledText.getOrNull(range.end) == '\n') {
        addStyle(TinyParagraphStyle, start = range.end, end = range.end + 1)
      }
    }
  }

  // todo: kdoc.
  fun addSpanPainter(painter: MarkdownSpanPainter)

  /** Stores a test tag that is only used by tests. */
  fun AnnotatedString.Builder.addTestTag(tag: String, range: TextRange) {
    if (useTestTags) {
      addStringAnnotation(
        tag = "test-tag",
        annotation = tag,
        start = range.start,
        end = range.end,
      )
    }
  }

  companion object {
    private val TinyParagraphStyle = ParagraphStyle(
      lineHeight = 0.sp,
      lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
        mode = LineHeightStyle.Mode.Tight,
      ),
    )
  }
}

private data class RealMarkdownNodeRenderScope(
  override val theme: WysiwygTheme,
  override val unstyledText: AnnotatedString,
  override val changes: List<TextChangeListSnapshot>,
  override val offsetInRoot: Int,
  override val useTestTags: Boolean,
  val spanPainters: MutableList<MarkdownSpanPainter>,
) : MarkdownNodeRenderScope {

  override fun childScope(child: MarkdownChildNode): MarkdownNodeRenderScope {
    return copy(
      offsetInRoot = offsetInRoot + child.offsetInParent,
    )
  }

  override fun addSpanPainter(painter: MarkdownSpanPainter) {
    spanPainters.add(painter)
  }
}
