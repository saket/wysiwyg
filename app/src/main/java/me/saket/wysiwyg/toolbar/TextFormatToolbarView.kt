package me.saket.wysiwyg.toolbar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import me.saket.wysiwyg.R
import me.saket.wysiwyg.toolbar.MarkdownAction.BOLD
import me.saket.wysiwyg.toolbar.MarkdownAction.HEADING
import me.saket.wysiwyg.toolbar.MarkdownAction.INLINE_CODE
import me.saket.wysiwyg.toolbar.MarkdownAction.INSERT_LINK
import me.saket.wysiwyg.toolbar.MarkdownAction.ITALIC
import me.saket.wysiwyg.toolbar.MarkdownAction.QUOTE
import me.saket.wysiwyg.toolbar.MarkdownAction.STRIKE_THROUGH

class TextFormatToolbarView(context: Context, attrs: AttributeSet) : HorizontalScrollView(context, attrs) {

  lateinit var actionClickListener: ActionClickListener

  interface ActionClickListener {
    /**
     * @param markdownBlock Nullable for insert-link, insert-text-emoji and insert-image.
     */
    fun onClickAction(buttonView: View, markdownAction: MarkdownAction)
  }

  init {
    LayoutInflater.from(context).inflate(R.layout.custom_text_formatting_toolbar, this, true)

    val actions = mapOf(
        R.id.textformattoolbar_insert_link to this::onClickInsertLink,
        R.id.textformattoolbar_bold to this::onClickBold,
        R.id.textformattoolbar_italic to this::onClickItalic,
        R.id.textformattoolbar_strikethrough to this::onClickStrikeThrough,
        R.id.textformattoolbar_quote to this::onClickQuote,
        R.id.textformattoolbar_inline_code to this::onClickInlineCode,
        R.id.textformattoolbar_header to this::onClickHeader
    )

    actions.entries.forEach { (buttonId, clickListener) ->
      findViewById<View>(buttonId).setOnClickListener(clickListener)
    }
  }

  private fun onClickInsertLink(view: View) {
    actionClickListener.onClickAction(view, INSERT_LINK)
  }

  private fun onClickBold(view: View) {
    actionClickListener.onClickAction(view, BOLD)
  }

  private fun onClickItalic(view: View) {
    actionClickListener.onClickAction(view, ITALIC)
  }

  private fun onClickStrikeThrough(view: View) {
    actionClickListener.onClickAction(view, STRIKE_THROUGH)
  }

  private fun onClickQuote(view: View) {
    actionClickListener.onClickAction(view, QUOTE)
  }

  private fun onClickInlineCode(view: View) {
    actionClickListener.onClickAction(view, INLINE_CODE)
  }

  private fun onClickHeader(view: View) {
    actionClickListener.onClickAction(view, HEADING)
  }
}
