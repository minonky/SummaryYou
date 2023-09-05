package com.example.summaryyoupython

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.WindowCompat
import com.chaquo.python.*
import com.chaquo.python.android.AndroidPlatform
import com.example.summaryyoupython.ui.theme.SummaryYouPythonTheme
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        // This will lay out our app behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            SummaryYouPythonTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Test()
                }
            }
        }
    }
}

@Preview
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun Test(modifier: Modifier = Modifier) {
    // Zustand für das Ergebnis des Transkript-Abrufs
    var transcriptResult by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf<String?>(null) }
    var author by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current // Zugriff auf den Context
    val haptics = LocalHapticFeedback.current //Vibration bei kopieren von Zusammenfassung
    val focusManager = LocalFocusManager.current //Cursor ausblenden

    val clipboardManager = ContextCompat.getSystemService(
        context,
        ClipboardManager::class.java
    ) as ClipboardManager

    val py = Python.getInstance()
    val module = py.getModule("youtube")

    Box() {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = { /*TODO*/ }, modifier = modifier.padding(start = 8.dp, top = 55.dp)) {
                Icon(Icons.Outlined.Settings, contentDescription = "Localized description")
            }
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 20.dp)
            ) {
                Text(
                    text = "Summary You",
                    style = MaterialTheme.typography.headlineLarge
                )
                // Anzeige des Ergebnisses oder Ladeanzeige
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp)
                    )
                } else {
                    Spacer(modifier = modifier.height(height = 9.dp))
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Videolink") },
                    singleLine = true,
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                )
                if (!transcriptResult.isNullOrEmpty()) {
                    Card(
                        modifier = modifier
                            .padding(top = 15.dp, bottom = 15.dp)
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Inhalt der Karte in die Zwischenablage kopieren
                                    clipboardManager.setPrimaryClip(
                                        ClipData.newPlainText("Transkript", transcriptResult)
                                    )
                                }
                            )
                    ) {
                        Text(
                            text = title ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = modifier
                                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                        )
                        Text(
                            text = author ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = modifier
                                .padding(top = 4.dp, start = 12.dp, end = 12.dp)
                        )
                        Text(
                            text = transcriptResult ?: "Transkript nicht gefunden",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = modifier
                                .padding(12.dp)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .padding(top = 15.dp, bottom = 40.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                isLoading = true // Starte den Abruf
                                transcriptResult = null //Textfeld und Regenerate ausblenden
                                scope.launch {
                                    title = getTitel(url)
                                    author = getAuthor(url)
                                    transcriptResult = summarize(url)
                                    isLoading = false // Setze isLoading auf false, wenn der Abruf abgeschlossen ist
                                }
                            },
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Regenerate")
                        }
                    }
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .imePadding()
            .imeNestedScroll()
            .fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        // Überprüfe, ob etwas in der Zwischenablage ist
                        val clipData = clipboardManager.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val clipItem = clipData.getItemAt(0)
                            url = clipItem.text.toString()
                        }
                    }
                },
                modifier = modifier.padding(bottom = 20.dp, end = 15.dp)
            ) {
                Icon(painter = painterResource(id = com.example.summaryyoupython.R.drawable.outline_content_paste_24), "Localized description")
            }
            FloatingActionButton(
                onClick = {
                    focusManager.clearFocus()
                    isLoading = true // Starte den Abruf
                    scope.launch {
                        title = getTitel(url)
                        author = getAuthor(url)
                        transcriptResult = summarize(url)
                        isLoading = false // Setze isLoading auf false, wenn der Abruf abgeschlossen ist
                    }
                },
                modifier = modifier.padding(bottom = 70.dp, end = 15.dp)
            ) {
                Icon(Icons.Filled.Check, "Localized description")
            }
        }
    }
}

suspend fun summarize(url: String): String {
    val py = Python.getInstance()
    val module = py.getModule("youtube")
    val dotenv = dotenv {
        directory = "/assets"
        filename = "env" // instead of '.env', use 'env'
    }
    val key = dotenv["OPEN_AI_KEY"]

    try {
        val result = withContext(Dispatchers.IO) {
            module.callAttr("summarize_youtube_video", url, key).toString()
        }
        return result
    } catch (e: Exception) {
        // Fehlerbehandlung
        return "Fehler beim Abrufen der Zusammenfassung"
    }
}

suspend fun getAuthor(url: String): String {
    val py = Python.getInstance()
    val module = py.getModule("youtube")

    try {
        val result = withContext(Dispatchers.IO) {
            module.callAttr("get_youtube_video_author", url).toString()
        }
        return result
    } catch (e: Exception) {
        // Fehlerbehandlung
        return "Fehler beim Abrufen des Authors"
    }
}

suspend fun getTitel(url: String): String {
    val py = Python.getInstance()
    val module = py.getModule("youtube")

    try {
        val result = withContext(Dispatchers.IO) {
            module.callAttr("get_youtube_video_title", url).toString()
        }
        return result
    } catch (e: Exception) {
        // Fehlerbehandlung
        return "Fehler beim Abrufen des Titels"
    }
}
