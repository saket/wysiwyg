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

  @Test fun type_with_wysiwyg_enabled() =
    runScenario(useWysiwyg = true, assetPath = "commonmark-spec.md")

  /** Stock Compose UI over the same fixture with no markdown styling: the floor to beat. */
  @Test fun type_with_wysiwyg_disabled() =
    runScenario(useWysiwyg = false, assetPath = "commonmark-spec.md")

  /** Mirrors the everyday "user is composing a short note" path against a tiny fixture. */
  @Test fun type_in_short_note_with_wysiwyg_enabled() =
    runScenario(useWysiwyg = true, assetPath = "short-note.md")

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
        TraceSectionMetric("Wysiwyg:render:walk"),
        TraceSectionMetric("Wysiwyg:render:addStyle"),
        TraceSectionMetric("Wysiwyg:drawBehind"),
        TraceSectionMetric("Wysiwyg:drawBehind:computeViewport"),
        TraceSectionMetric("Wysiwyg:drawBehind:paintVisible"),
        TraceSectionMetric("Wysiwyg:drawBehind:paint"),
      ),
      iterations = 5,
      startupMode = StartupMode.WARM,
      compilationMode = CompilationMode.None(),
      setupBlock = {
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
      // todo: dropped chars. On the long-doc fixture the full "Lorem ipsum dolor
      // sit amet" doesn't always land in the editor — when transformOutput is
      // slow enough, some keystrokes are coalesced or waitForIdle returns
      // before the burst finishes committing. Trace counts (transformOutput
      // calls/iteration) stay stable across runs so the comparison still works,
      // but "26 chars" is aspirational. Fix later by either driving input via
      // an InputConnection from a test IME, or asserting the editor's text
      // length matches the injected string before exiting the iteration.
      device.executeShellCommand("input text 'Lorem ipsum dolor sit amet'")
      device.waitForIdle()
    }
  }

  private companion object {
    const val TargetPackage = "me.saket.wysiwyg.sample"
  }
}
