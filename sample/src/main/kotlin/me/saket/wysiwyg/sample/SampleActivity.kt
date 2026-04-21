package me.saket.wysiwyg.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

class SampleActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    setContent {
      AppTheme {
        Surface {
          WysiwygEditor(
            Modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
  val colorScheme = if (isSystemInDarkTheme()) {
    dynamicDarkColorScheme(LocalContext.current)
  } else {
    dynamicLightColorScheme(LocalContext.current)
  }
  MaterialTheme(colorScheme) {
    content()
  }
}
