package com.folio.viewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.folio.viewer.ui.App
import com.folio.viewer.ui.FolioTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {

    private var state by mutableStateOf<UiState>(UiState.Idle)
    private var external = false
    private var cacheFile: File? = null
    private var loadJob: Job? = null

    private val mimes = arrayOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        cleanOldCache()

        extractUri(intent)?.let {
            external = true
            open(it)
        }

        setContent {
            FolioTheme {
                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri -> if (uri != null) open(uri) }

                BackHandler(enabled = state !is UiState.Idle && !external) { close() }

                App(
                    state = state,
                    onPick = { picker.launch(mimes) },
                    onClose = { if (external) finish() else close() }
                )
            }
        }
    }

    private fun extractUri(i: Intent?): Uri? {
        if (i == null) return null
        return when (i.action) {
            Intent.ACTION_VIEW -> i.data
            Intent.ACTION_SEND ->
                IntentCompat.getParcelableExtra(i, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
    }

    private fun open(uri: Uri) {
        loadJob?.cancel()
        closeDoc()
        val name = displayName(uri)
        state = UiState.Loading(name)
        loadJob = lifecycleScope.launch {
            state = try {
                val loaded = withContext(Dispatchers.IO) {
                    val f = File(cacheDir, "open_${System.nanoTime()}")
                    cacheFile = f
                    contentResolver.openInputStream(uri)?.use { input ->
                        f.outputStream().use { out -> input.copyTo(out) }
                    } ?: throw IOException("Couldn't read this file")
                    Loader.load(f)
                }
                UiState.Ready(name, loaded.kind, loaded.doc)
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnsupportedException) {
                UiState.Failed(name, e.message ?: "Unsupported file")
            } catch (e: Exception) {
                UiState.Failed(
                    name,
                    "This file couldn't be read. It may be damaged or password-protected."
                )
            }
        }
    }

    private fun close() {
        loadJob?.cancel()
        closeDoc()
        state = UiState.Idle
    }

    private fun closeDoc() {
        (state as? UiState.Ready)?.let { (it.doc as? Doc.Pdf)?.source?.close() }
        cacheFile?.delete()
        cacheFile = null
    }

    private fun displayName(uri: Uri): String {
        runCatching {
            contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (i >= 0) c.getString(i)?.let { return it }
                }
            }
        }
        return uri.lastPathSegment ?: "Document"
    }

    private fun cleanOldCache() {
        val dayAgo = System.currentTimeMillis() - 24L * 3600 * 1000
        cacheDir.listFiles { f -> f.name.startsWith("open_") && f.lastModified() < dayAgo }
            ?.forEach { it.delete() }
    }

    override fun onDestroy() {
        closeDoc()
        super.onDestroy()
    }
}
