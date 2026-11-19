package gio.frag.csvparsely.feature.csv_viewer.composable

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import gio.frag.csvparsely.core.design_system.theme.CsvParselyTheme
import gio.frag.csvparsely.core.domain.error.CsvError
import gio.frag.csvparsely.core.domain.inference.toCsvTable
import gio.frag.csvparsely.core.domain.model.RawCsv
import gio.frag.csvparsely.feature.csv_viewer.model.CsvEvent
import gio.frag.csvparsely.feature.csv_viewer.model.CsvUiState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
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
        val expectedTexts = listOf("Theo", "Jansen", "5", "Jan 2, 1978, 00:00", "Fiona", "Petra", "Apr 20, 2001, 00:00")
        val expectedCardCount = 3
        val expectedHiddenUrl = "https://api.multiavatar.com/2cdf5db9b4dee297b7.png"

        setContent(
            CsvUiState(
                isLoading = false,
                table = table,
            ),
        )

        expectedTexts.forEach { text ->
            composeTestRule.onNodeWithText(text).assertIsDisplayed()
        }
        composeTestRule.onAllNodesWithText("First name").assertCountEquals(expectedCardCount)
        composeTestRule.onAllNodesWithContentDescription("avatar").assertCountEquals(expectedCardCount)
        composeTestRule.onNodeWithText(expectedHiddenUrl).assertDoesNotExist()
    }

    @Test
    fun valuesAreFormattedByType() {
        val table = RawCsv(
            header = listOf("Count", "Seen", "Born", "Note", ""),
            rows = listOf(
                listOf("-3", "2020-05-01T14:30:00", "2001-04-20", "", "x"),
            ),
        ).toCsvTable()
        val expectedTexts = listOf("-3", "May 1, 2020, 14:30", "Apr 20, 2001", "-", "Column 5")

        setContent(
            CsvUiState(
                isLoading = false,
                table = table,
            ),
        )

        expectedTexts.forEach { text ->
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
        val expectedText = "Apr 20, 2001, 09:15"

        setContent(
            CsvUiState(
                isLoading = false,
                table = table,
            ),
        )

        composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
    }

    @Test
    fun imageUrlsAreShownAsImagesAndOtherUrlsAsText() {
        val imageWithQuery = "https://example.com/photo.JPG?size=2"
        val report = "https://example.com/report.pdf"
        val table = RawCsv(
            header = listOf("photo", "report"),
            rows = listOf(listOf(imageWithQuery, report)),
        ).toCsvTable()
        val expectedImageDescription = "photo"
        val expectedTexts = listOf("report", report)

        setContent(
            CsvUiState(
                isLoading = false,
                table = table,
            ),
        )

        composeTestRule.onNodeWithContentDescription(expectedImageDescription).assertIsDisplayed()
        composeTestRule.onNodeWithText(imageWithQuery).assertDoesNotExist()
        expectedTexts.forEach { text ->
            composeTestRule.onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun headerOnlyFileShowsTheEmptyState() {
        val table = RawCsv(
            header = listOf("First name", "Sur name"),
            rows = emptyList(),
        ).toCsvTable()
        val expectedText = "Nothing to show"

        setContent(
            CsvUiState(
                isLoading = false,
                table = table,
            ),
        )

        composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
        composeTestRule.onNodeWithText("First name").assertDoesNotExist()
    }

    @Test
    fun failureBeforeAnyRowsShowsTheEmptyState() {
        val expectedText = "Nothing to show"

        setContent(
            CsvUiState(
                isLoading = false,
                error = CsvError.Network(cause = null),
            ),
        )

        composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
    }

    @Test
    fun eachErrorKindShowsItsMessage() {
        val expectedMessages = listOf(
            CsvError.Network(cause = null) to "No connection. Check your internet and try again.",
            CsvError.Http(code = 503) to "Server error (503).",
            CsvError.Storage(cause = IOException()) to "The file couldn't be saved on the phone.",
            CsvError.EmptyFile() to "The file is empty.",
            CsvError.Malformed(line = 3) to "The file is malformed: a quote opened on line 3 is never closed.",
            CsvError.FileTooLarge(cause = OutOfMemoryError()) to "The file is too large to show.",
            CsvError.Generic(cause = IllegalStateException()) to "Something went wrong.",
        )
        val state = setContent(CsvUiState(isLoading = false))

        expectedMessages.forEach { (error, expectedMessage) ->
            state.value = CsvUiState(
                isLoading = false,
                error = error,
            )

            composeTestRule.onNodeWithText(expectedMessage).assertIsDisplayed()
        }
    }

    @Test
    fun retryIsOfferedWhenRetryingCanHelpAndSendsTheEvent() {
        val events = mutableListOf<CsvEvent>()
        val expectedEvents = listOf(CsvEvent.Retry)

        setContent(
            state = CsvUiState(
                isLoading = false,
                error = CsvError.Http(code = 503),
            ),
            onEvent = { event -> events += event },
        )
        composeTestRule.onNodeWithText("Retry").performClick()

        composeTestRule.runOnIdle {
            assertEquals(expectedEvents, events)
        }
    }

    @Test
    fun noRetryWhenRetryingCannotHelp() {
        val expectedMessage = "The file is malformed: a quote opened on line 3 is never closed."

        setContent(
            CsvUiState(
                isLoading = false,
                error = CsvError.Malformed(line = 3),
            ),
        )

        composeTestRule.onNodeWithText(expectedMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun rowsStayVisibleWithAnError() {
        val table = RawCsv(
            header = listOf("First name"),
            rows = listOf(listOf("Theo")),
        ).toCsvTable()
        val expectedTexts = listOf("Theo", "No connection. Check your internet and try again.")

        setContent(
            CsvUiState(
                isLoading = false,
                table = table,
                error = CsvError.Network(cause = null),
            ),
        )

        expectedTexts.forEach { text ->
            composeTestRule.onNodeWithText(text).assertIsDisplayed()
        }
        composeTestRule.onNodeWithText("Nothing to show").assertDoesNotExist()
    }

    /**
     * Shows [state] with an English (US) locale, so dates and numbers have a known format. Returns the shown state, so
     * a test can change it.
     */
    private fun setContent(
        state: CsvUiState,
        onEvent: (CsvEvent) -> Unit = {},
    ): MutableState<CsvUiState> {
        val shownState = mutableStateOf(state)
        composeTestRule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                setLocale(Locale.US)
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                CsvParselyTheme {
                    CsvViewerContent(
                        state = shownState.value,
                        onEvent = onEvent,
                    )
                }
            }
        }
        return shownState
    }
}
