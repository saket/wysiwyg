package me.saket.wysiwyg

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotterknife.bindView
import me.saket.markdownrenderer.MarkdownHintOptions
import me.saket.markdownrenderer.MarkdownHints
import me.saket.markdownrenderer.MarkdownSpanPool
import me.saket.wysiwyg.toolbar.OnLinkInsertListener
import me.saket.wysiwyg.toolbar.TextFormatToolbarView

class EditorActivity : AppCompatActivity(), OnLinkInsertListener {

  private val editorEditText: EditText by bindView(R.id.editor_editor)
  private val formatToolbarView: TextFormatToolbarView by bindView(R.id.editor_format_toolbar)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_editor)

    val markdownHintOptions = markdownHintOptions()
    val markdownSpanPool = MarkdownSpanPool(this, markdownHintOptions)
    val markdownHints = MarkdownHints(editorEditText, markdownHintOptions, markdownSpanPool)
    editorEditText.addTextChangedListener(markdownHints)

    formatToolbarView.editorEditText = editorEditText
  }

  private fun markdownHintOptions(): MarkdownHintOptions {
    val color = { colorResId: Int -> ContextCompat.getColor(this, colorResId) }
    val dimensPx = { dimenResId: Int -> resources.getDimensionPixelSize(dimenResId) }

    return MarkdownHintOptions(
        syntaxColor = color(R.color.markdown_syntax),
        blockQuoteIndentationRuleColor = color(R.color.markdown_blockquote_indentation_rule),
        blockQuoteTextColor = color(R.color.markdown_blockquote_text),
        blockQuoteVerticalRuleStrokeWidth = dimensPx(R.dimen.markdown_blockquote_vertical_rule_stroke_width),
        linkUrlColor = color(R.color.markdown_link_url),
        linkTextColor = color(R.color.markdown_link_text),
        listBlockIndentationMargin = dimensPx(R.dimen.markdown_text_block_indentation_margin),
        horizontalRuleColor = color(R.color.markdown_horizontal_rule),
        horizontalRuleStrokeWidth = dimensPx(R.dimen.markdown_horizontal_rule_stroke_width),
        inlineCodeBackgroundColor = color(R.color.markdown_inline_code_background))
  }

  override fun onLinkInsert(title: String, url: String) {
    val selectionStart = Math.min(editorEditText.selectionStart, editorEditText.selectionEnd)
    val selectionEnd = Math.max(editorEditText.selectionStart, editorEditText.selectionEnd)

    val linkMarkdown = when {
      title.isEmpty() -> url
      else -> String.format("[%s](%s)", title, url)
    }
    editorEditText.text.replace(selectionStart, selectionEnd, linkMarkdown)
  }

  //  override fun onClickAction(buttonView: View, markdownAction: MarkdownAction) {
  //    when (markdownAction) {
  //      INSERT_LINK -> {
  //        // preFilledTitle will be empty when there's no text selected.
  //        val selectionStart = Math.min(replyField.getSelectionStart(), replyField.getSelectionEnd())
  //        val selectionEnd = Math.max(replyField.getSelectionStart(), replyField.getSelectionEnd())
  //        val preFilledTitle = replyField.getText().subSequence(selectionStart, selectionEnd)
  //        AddLinkDialog.showPreFilled(supportFragmentManager, preFilledTitle.toString())
  //      }
  //
  //      QUOTE, HEADING -> {
  //        insertQuoteOrHeadingMarkdownSyntax(markdownBlock)
  //      }
  //
  //      else -> insertMarkdownSyntax(markdownBlock)
  //    }
  //  }
}
