package me.saket.wysiwyg.sample

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotterknife.bindView
import me.saket.markdownrenderer.Wysiwyg
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.flexmark.FlexmarkMarkdownParser
import kotlin.math.max
import kotlin.math.min

class EditorActivity : AppCompatActivity(), OnLinkInsertListener {

  private val editorEditText: EditText by bindView(R.id.editor_editor)
  private val formatToolbarView: MarkdownFormatToolbarView by bindView(R.id.editor_format_toolbar)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_editor)

    val markdownParser = FlexmarkMarkdownParser(markdownHintTheme())
    val markdownHints = Wysiwyg(editorEditText, markdownParser)
    editorEditText.addTextChangedListener(markdownHints.textWatcher())

    formatToolbarView.onMarkdownSyntaxApplied = { syntax -> syntax.insert(editorEditText) }
    formatToolbarView.onInsertLinkClicked = {
      // selectionStart can be lesser than selectionEnd if the selection was made right-to-left.
      val selectionStart = min(editorEditText.selectionStart, editorEditText.selectionEnd)
      val selectionEnd = max(editorEditText.selectionStart, editorEditText.selectionEnd)

      // preFilledTitle will be empty when there's no text selected.
      val preFilledTitle = editorEditText.text.subSequence(selectionStart, selectionEnd)
      AddLinkDialog.showPreFilled(supportFragmentManager, preFilledTitle.toString())
    }
  }

  private fun markdownHintTheme(): WysiwygTheme {
    val color = { colorResId: Int -> ContextCompat.getColor(this, colorResId) }
    val dimensPx = { dimenResId: Int -> resources.getDimensionPixelSize(dimenResId) }

    return WysiwygTheme(
        context = this,
        syntaxColor = color(R.color.markdown_syntax),
        blockQuoteVerticalRuleColor = color(R.color.markdown_blockquote_indentation_rule),
        blockQuoteTextColor = color(R.color.markdown_blockquote_text),
        blockQuoteVerticalRuleStrokeWidth = dimensPx(R.dimen.markdown_blockquote_vertical_rule_stroke_width),
        linkUrlColor = color(R.color.markdown_link_url),
        linkTextColor = color(R.color.markdown_link_text),
        listBlockIndentationMargin = dimensPx(R.dimen.markdown_text_block_indentation_margin),
        thematicBreakColor = color(R.color.markdown_horizontal_rule),
        thematicBreakThickness = dimensPx(R.dimen.markdown_horizontal_rule_stroke_width).toFloat(),
        codeBackgroundColor = color(R.color.markdown_code_background))
  }

  override fun onLinkInsert(link: Link) {
    link.insert(editorEditText)
  }
}
