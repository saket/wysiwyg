package me.saket.wysiwyg.toolbar

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager.LayoutParams
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotterknife.bindView
import me.saket.wysiwyg.R

class AddLinkDialog : DialogFragment() {

  private val titleEditText: EditText by bindView(R.id.addlinkdialog_title)
  private val urlEditText: EditText by bindView(R.id.addlinkdialog_url)
  private val cancelButton: Button by bindView(R.id.addlinkdialog_cancel)
  private val insertButton: Button by bindView(R.id.addlinkdialog_insert)

  override fun onAttach(context: Context?) {
    super.onAttach(context)

    if (requireActivity() !is OnLinkInsertListener) {
      throw AssertionError()
    }
  }

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    super.onCreateDialog(savedInstanceState)

    val dialogLayout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_link, null)
    val dialog = AlertDialog.Builder(requireContext())
        .setView(dialogLayout)
        .create()

    // Show keyboard automatically on start. Doesn't happen on its own.
    dialog.window!!.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    return dialog
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    if (savedInstanceState == null && arguments!!.containsKey(KEY_PRE_FILLED_TITLE)) {
      val preFilledTitle = arguments!!.getString(KEY_PRE_FILLED_TITLE)
      titleEditText.setTextWithCursor(preFilledTitle)
    }

    cancelButton.setOnClickListener { dismiss() }
    insertButton.setOnClickListener { validateLinkAndSendCallback() }
  }

  private fun validateLinkAndSendCallback() {
    val title = titleEditText.text.toString().trim()
    val url = urlEditText.text.toString().trim()

    titleEditText.error = when {
      title.isNotBlank() -> getString(R.string.composereply_addlink_error_empty_field)
      else -> null
    }
    urlEditText.error = when {
      url.isNotBlank() -> getString(R.string.composereply_addlink_error_empty_field)
      else -> null
    }

    if (title.isNotBlank() && url.isNotBlank()) {
      (requireActivity() as OnLinkInsertListener).onLinkInsert(title, url)
      dismiss()
    }
  }

  private fun EditText.setTextWithCursor(string: CharSequence?) {
    setText(string)
    setSelection(text.length)
  }

  companion object {

    private const val KEY_PRE_FILLED_TITLE = "preFilledTitle"
    private const val TAG = "AddLinkDialog"

    fun show(fragmentManager: FragmentManager) {
      showPreFilled(fragmentManager, null)
    }

    fun showPreFilled(fragmentManager: FragmentManager, preFilledTitle: String?) {
      var dialog = fragmentManager.findFragmentByTag(TAG) as AddLinkDialog?
      dialog?.dismiss()

      val arguments = Bundle()
      if (preFilledTitle != null) {
        arguments.putString(KEY_PRE_FILLED_TITLE, preFilledTitle)
      }
      dialog = AddLinkDialog()
      dialog.arguments = arguments
      dialog.show(fragmentManager, TAG)
    }
  }
}
