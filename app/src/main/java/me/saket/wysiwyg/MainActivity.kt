package me.saket.wysiwyg

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotterknife.bindView

class MainActivity : AppCompatActivity() {

  private val editorEditText by bindView<EditText>(R.id.main_editor)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
  }
}
