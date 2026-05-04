package me.saket.wysiwyg.benchmarks

import android.content.Intent
import android.view.KeyEvent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Run with: `./gradlew :benchmarks:connectedReleaseAndroidTest` */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class TypingBenchmark {
  @get:Rule val rule = MacrobenchmarkRule()

  @Test fun type_in_long_document_with_wysiwyg_enabled() =
    runScenario(useWysiwyg = true, assetPath = "commonmark-spec.md")

  /** Stock Compose UI over the same fixture with no markdown styling: the floor to beat. */
  @Test fun type_in_long_document_with_wysiwyg_disabled() =
    runScenario(useWysiwyg = false, assetPath = "commonmark-spec.md")

  @Test fun type_in_short_document_with_wysiwyg_enabled() =
    runScenario(useWysiwyg = true, assetPath = "short-document.md")

  @Test fun type_in_short_document_with_wysiwyg_disabled() =
    runScenario(useWysiwyg = false, assetPath = "short-document.md")

  private fun runScenario(useWysiwyg: Boolean, assetPath: String) {
    rule.measureRepeated(
      packageName = TargetPackage,
      metrics = listOf(
        FrameTimingMetric(),
        TraceSectionMetric("Wysiwyg:inputTransformation"),
        TraceSectionMetric("Wysiwyg:parse"),
        TraceSectionMetric("Wysiwyg:flexmarkParse"),
        TraceSectionMetric("Wysiwyg:convertFlexmarkAst"),
        TraceSectionMetric("Wysiwyg:edited"),
        TraceSectionMetric("Wysiwyg:transformOutput"),
        TraceSectionMetric("Wysiwyg:render"),
        TraceSectionMetric("Wysiwyg:render:addStyle"),
        TraceSectionMetric("Wysiwyg:drawBehind"),
      ),
      iterations = 5,
      startupMode = StartupMode.WARM,
      compilationMode = CompilationMode.None(),
      setupBlock = {
        // Wake the screen and dismiss the keyguard before launching. With the screen
        // off the device sits in a low-power state and frame-CPU timings get throttled.
        // KEYCODE_WAKEUP is idempotent; safe to issue every iteration.
        device.executeShellCommand("input keyevent KEYCODE_WAKEUP")
        device.executeShellCommand("wm dismiss-keyguard")

        val intent = Intent().apply {
          setClassName(TargetPackage, "${TargetPackage}.BenchmarkActivity")
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
          putExtra("use_wysiwyg", useWysiwyg)
          putExtra("asset_path", assetPath)
        }
        startActivityAndWait(intent)
      },
    ) {
      // Compose's testTagsAsResourceId exposes the bare tag ("editor") via
      // AccessibilityNodeInfo#viewIdResourceName, without a package prefix —
      // hence the single-arg By.res() form rather than By.res(pkg, id).
      val editor = device.wait(Until.findObject(By.res("editor")), 5_000)
        ?: error("Could not find editor in BenchmarkActivity")

      // Tap to focus the editor, then jump cursor to end-of-document. Compose's
      // BasicTextField binds Alt+↓ to MoveDocumentEnd, which both repositions
      // the cursor and auto-scrolls the visible region.
      editor.click()
      device.pressKeyCode(KeyEvent.KEYCODE_DPAD_DOWN, /* metaState = */ KeyEvent.META_ALT_ON)
      device.waitForIdle()

      // Inject keystrokes through the system input dispatcher. No artificial
      // inter-char delay — chars stream as fast as the dispatcher delivers them.
      //
      // Caveat: this routes through KeyEvent dispatch, not InputConnection. With
      // no soft IME visible (sample uses adjustNothing), BasicTextField handles
      // printable keys via the same TextFieldState mutation path that commitText
      // would, so the parser hot path is exercised. If the typing lags we're
      // chasing only reproduce via real IME commits, swap this for a test
      // InputMethodService driven from this scenario.
      //
      // todo: when wysiwyg is enabled, chars are dropped
      device.executeShellCommand("input text Lorem%sipsum%sdolor%ssit%samet")
      device.waitForIdle()
    }
  }

  private companion object {
    const val TargetPackage = "me.saket.wysiwyg.sample"
  }
}
