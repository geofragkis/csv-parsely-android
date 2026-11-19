package gio.frag.csvparsely.feature.csv_viewer

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import gio.frag.csvparsely.core.design_system.theme.CsvParselyTheme
import gio.frag.csvparsely.core.domain.error.CsvError
import gio.frag.csvparsely.core.domain.inference.toCsvTable
import gio.frag.csvparsely.core.domain.model.RawCsv
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.TimeZone

/**
 * Created by Giorgos Fragkis on 19-11-2026
 */
@RunWith(AndroidJUnit4::class)
class CsvViewerContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val originalTimeZone = TimeZone.getDefault()

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun loadingShowsTheLoadingIndicator() {
        setContent(CsvUiState(isLoading = true))

        composeTestRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertIsDisplayed()
    }

    @Test
    fun sampleFileShowsACardPerRowWithLabelsAndFormattedValues() {
        val table = RawCsv(
            header = listOf("First name", "Sur name", "Issue count", "Date of birth", "avatar"),
            rows = listOf(
                listOf("Theo", "Jansen", "5", "1978-01-02T00:00:00", "https://api.multiavatar.com/2cdf5db9b4dee297b7.png"),
                listOf("Fiona", "de Vries", "7", "1950-11-12T00:00:00", "https://api.multiavatar.com/b9339cb9e7a833cd5e.png"),
                listOf("Petra", "Boersma", "1", "2001-04-20T00:00:00", "https://api.multiavatar.com/2672c49d6099f87274.png"),
            ),
        ).toCsvTable()

        setContent(CsvUiState(isLoading = false, table = table))

        listOf("Theo", "Jansen", "5", "Jan 2, 1978, 00:00", "Fiona", "Petra", "Apr 20, 2001, 00:00").forEach { text ->
            composeTestRule.onNodeWithText(text).assertIsDisplayed()
        }
        composeTestRule.onAllNodesWithText("First name").assertCountEquals(3)
        composeTestRule.onAllNodesWithContentDescription("avatar").assertCountEquals(3)
        composeTestRule.onNodeWithText("https://api.multiavatar.com/2cdf5db9b4dee297b7.png").assertDoesNotExist()
    }

    @Test
    fun valuesAreFormattedByType() {
        val table = RawCsv(
            header = listOf("Count", "Seen", "Born", "Note", ""),
            rows = listOf(
                listOf("-3", "2020-05-01T14:30:00", "2001-04-20", "", "x"),
            ),
        ).toCsvTable()

        setContent(CsvUiState(isLoading = false, table = table))

        listOf("-3", "May 1, 2020, 14:30", "Apr 20, 2001", "-", "Column 5").forEach { text ->
            composeTestRule.onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun dateTimeWithAnOffsetIsShownInTheDeviceTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+01:00"))
        val table = RawCsv(
            header = listOf("Updated"),
            rows = listOf(listOf("2001-04-20T10:15:30+02:00")),
        ).toCsvTable()

        setContent(CsvUiState(isLoading = false, table = table))

        composeTestRule.onNodeWithText("Apr 20, 2001, 09:15").assertIsDisplayed()
    }

    @Test
    fun imageUrlsAreShownAsImagesAndOtherUrlsAsText() {
        val imageWithQuery = "https://example.com/photo.JPG?size=2"
        val report = "https://example.com/report.pdf"
        val table = RawCsv(
            header = listOf("photo", "report"),
            rows = listOf(listOf(imageWithQuery, report)),
        ).toCsvTable()

        setContent(CsvUiState(isLoading = false, table = table))

        composeTestRule.onNodeWithContentDescription("photo").assertIsDisplayed()
        composeTestRule.onNodeWithText(imageWithQuery).assertDoesNotExist()
        composeTestRule.onNodeWithText("report").assertIsDisplayed()
        composeTestRule.onNodeWithText(report).assertIsDisplayed()
    }

    @Test
    fun headerOnlyFileShowsTheEmptyState() {
        val table = RawCsv(
            header = listOf("First name", "Sur name"),
            rows = emptyList(),
        ).toCsvTable()

        setContent(CsvUiState(isLoading = false, table = table))

        composeTestRule.onNodeWithText("Nothing to show").assertIsDisplayed()
        composeTestRule.onNodeWithText("First name").assertDoesNotExist()
    }

    @Test
    fun failureBeforeAnyRowsShowsTheEmptyState() {
        setContent(CsvUiState(isLoading = false, error = CsvError.Network(cause = null)))

        composeTestRule.onNodeWithText("Nothing to show").assertIsDisplayed()
    }

    /** Shows [state] with an English (US) locale, so dates have a known format. */
    private fun setContent(state: CsvUiState) {
        composeTestRule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                setLocale(Locale.US)
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                CsvParselyTheme {
                    CsvViewerContent(state = state)
                }
            }
        }
    }
}
