package gio.frag.csvparsely

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import gio.frag.csvparsely.core.design_system.theme.CsvParselyTheme
import gio.frag.csvparsely.feature.csv_viewer.composable.CsvViewerScreen

/**
 * Created by Giorgos Fragkis on 12-11-2026
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light status bar icons, since the top bar is dark blue in both light and dark mode
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            CsvParselyTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    // The screen's own Scaffold handles the system bars
                    contentWindowInsets = WindowInsets(0),
                ) { innerPadding ->
                    CsvViewerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
