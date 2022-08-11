package me.saket.wysiwyg.sample

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

class SampleActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // For animated IME insets.
    WindowCompat.setDecorFitsSystemWindows(window, false)

    setContent {
      PreviewScaffold {
        WysiwygEditor()
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PreviewScaffold(content: @Composable () -> Unit) {
  val darkTheme = isSystemInDarkTheme()
  val colorScheme = when {
    SDK_INT >= 31 -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> darkColorScheme()
    else -> lightColorScheme()
  }
  MaterialTheme(colorScheme) {
    Scaffold { insetPadding ->
      Box(
        Modifier
          .fillMaxSize()
          .padding(insetPadding)
          .systemBarsPadding()
          .imePadding()
      ) {
        content()
      }
    }
  }
}
