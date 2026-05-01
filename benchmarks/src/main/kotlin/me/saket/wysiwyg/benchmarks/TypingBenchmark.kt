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

  @Test fun type_with_wysiwyg_enabled() = runScenario(useWysiwyg = true)

  /** Stock Compose UI over the same fixture with no markdown styling: the floor to beat. */
  @Test fun type_with_wysiwyg_disabled() = runScenario(useWysiwyg = false)

  private fun runScenario(useWysiwyg: Boolean) {
    rule.measureRepeated(
      packageName = TargetPackage,
      metrics = listOf(
        FrameTimingMetric(),
        TraceSectionMetric("Wysiwyg:parse", mode = TraceSectionMetric.Mode.Sum),
        TraceSectionMetric("Wysiwyg:edited", mode = TraceSectionMetric.Mode.Sum),
        TraceSectionMetric("Wysiwyg:transformOutput", mode = TraceSectionMetric.Mode.Sum),
        TraceSectionMetric("Wysiwyg:buildAnnotatedString", mode = TraceSectionMetric.Mode.Sum),
        TraceSectionMetric("Wysiwyg:applySpans", mode = TraceSectionMetric.Mode.Sum),
        TraceSectionMetric("Wysiwyg:drawBehind", mode = TraceSectionMetric.Mode.Sum),
      ),
      iterations = 5,
      startupMode = StartupMode.WARM,
      compilationMode = CompilationMode.None(),
      setupBlock = {
        val intent = Intent().apply {
          setClassName(TargetPackage, "${TargetPackage}.BenchmarkActivity")
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
          putExtra("use_wysiwyg", useWysiwyg)
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
      device.executeShellCommand("input text 'Lorem ipsum dolor sit amet'")
      device.waitForIdle()
    }
  }

  private companion object {
    const val TargetPackage = "me.saket.wysiwyg.sample"
  }
}
