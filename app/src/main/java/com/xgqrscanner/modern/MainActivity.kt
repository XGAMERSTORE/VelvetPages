package com.xgqrscanner.modern

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date

private const val PREFS_NAME = "qr_scanner_prefs"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val language = prefs.getString("language", "").orEmpty()
        AppCompatDelegate.setApplicationLocales(
            if (language.isBlank()) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(language)
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QRScannerRoot()
        }
    }
}

private enum class AppScreen(val labelRes: Int, val icon: ImageVector) {
    SCAN(R.string.scan_tab, Icons.Rounded.QrCodeScanner),
    CREATE(R.string.create_tab, Icons.Rounded.QrCode2),
    HISTORY(R.string.history_tab, Icons.Rounded.History),
    SETTINGS(R.string.settings_tab, Icons.Rounded.Settings)
}

@Composable
private fun QRScannerRoot() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var themeMode by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
    var dynamicColors by remember { mutableStateOf(prefs.getBoolean("dynamic_colors", true)) }
    var screen by rememberSaveable { mutableStateOf(AppScreen.SCAN) }
    val history = remember {
        mutableStateListOf<ScanRecord>().apply { addAll(HistoryStore.load(context)) }
    }

    QRScannerTheme(themeMode = themeMode, dynamicColors = dynamicColors) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                NavigationBar {
                    AppScreen.entries.forEach { item ->
                        NavigationBarItem(
                            selected = screen == item,
                            onClick = { screen = item },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (screen) {
                    AppScreen.SCAN -> ScanScreen(
                        onRecord = { record ->
                            history.clear()
                            history.addAll(HistoryStore.add(context, record))
                        }
                    )

                    AppScreen.CREATE -> CreateScreen()

                    AppScreen.HISTORY -> HistoryScreen(
                        records = history,
                        onClear = {
                            HistoryStore.clear(context)
                            history.clear()
                        }
                    )

                    AppScreen.SETTINGS -> SettingsScreen(
                        themeMode = themeMode,
                        dynamicColors = dynamicColors,
                        onThemeMode = {
                            themeMode = it
                            prefs.edit().putString("theme", it).apply()
                        },
                        onDynamicColors = {
                            dynamicColors = it
                            prefs.edit().putBoolean("dynamic_colors", it).apply()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScanScreen(onRecord: (ScanRecord) -> Unit) {
    val context = LocalContext.current
    var result by remember { mutableStateOf<ScanRecord?>(null) }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    fun handleResult(value: String, format: Int) {
        val record = ScanRecord(value, barcodeFormatName(format), System.currentTimeMillis())
        onRecord(record)
        playScanFeedback(context)
        result = record
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { cameraGranted = it }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scanImageUri(
                context = context,
                uri = uri,
                onResult = ::handleResult,
                onEmpty = {
                    Toast.makeText(context, context.getString(R.string.no_code_found), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(
            title = stringResource(R.string.scan_title),
            subtitle = stringResource(R.string.scan_subtitle)
        )

        if (cameraGranted) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .heightIn(min = 360.dp, max = 520.dp),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                ScannerCamera(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp)),
                    onResult = ::handleResult,
                    onError = {
                        Toast.makeText(context, context.getString(R.string.scanner_error), Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Text(
                text = stringResource(R.string.point_camera),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(22.dp)) {
                    Icon(
                        Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.camera_permission_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.camera_permission_body),
                        modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                    )
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text(stringResource(R.string.allow_camera))
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.scan_from_gallery))
            }
        }
    }

    result?.let {
        ResultDialog(record = it, onDismiss = { result = null })
    }
}

@Composable
private fun ResultDialog(record: ScanRecord, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val canOpen = remember(record.value) { Patterns.WEB_URL.matcher(record.value).matches() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.result_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    SelectionContainer {
                        Text(
                            text = record.value,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.code_type) + ": " + record.format,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("code", record.value))
                            Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.copy))
                    }

                    FilledTonalButton(
                        onClick = { shareText(context, record.value) }
                    ) {
                        Icon(Icons.Rounded.Share, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.share))
                    }
                }
                if (canOpen) {
                    Button(
                        onClick = { openUrl(context, record.value) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.OpenInNew, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.open_link))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateScreen() {
    val context = LocalContext.current
    var data by rememberSaveable { mutableStateOf("") }
    var format by remember { mutableStateOf(GenerateFormat.QR) }
    var expanded by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PageHeader(
            title = stringResource(R.string.create_title),
            subtitle = stringResource(R.string.create_subtitle)
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = data,
                onValueChange = {
                    data = it
                    bitmap = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.content_label)) },
                placeholder = { Text(stringResource(R.string.content_hint)) },
                minLines = 3,
                shape = RoundedCornerShape(20.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = format.label,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = { Text(stringResource(R.string.format_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    shape = RoundedCornerShape(20.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    GenerateFormat.entries.forEach { option ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                format = option
                                bitmap = null
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (data.isBlank()) return@Button
                    bitmap = runCatching { generateCodeBitmap(data, format) }
                        .onFailure {
                            Toast.makeText(context, context.getString(R.string.invalid_data), Toast.LENGTH_SHORT).show()
                        }
                        .getOrNull()
                },
                enabled = data.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Rounded.QrCode2, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.generate))
            }

            bitmap?.let { codeBitmap ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (format.square) Modifier.aspectRatio(1f)
                                    else Modifier.aspectRatio(2.15f)
                                ),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White
                        ) {
                            Image(
                                bitmap = codeBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.padding(18.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    val ok = saveBitmapToGallery(context, codeBitmap)
                                    Toast.makeText(
                                        context,
                                        context.getString(if (ok) R.string.saved else R.string.save_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Save, null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.save_image))
                            }

                            FilledTonalButton(
                                onClick = { shareBitmap(context, codeBitmap) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Share, null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.share_image))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HistoryScreen(records: List<ScanRecord>, onClear: () -> Unit) {
    var selected by remember { mutableStateOf<ScanRecord?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.history_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (records.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Rounded.DeleteSweep, stringResource(R.string.clear_history))
                }
            }
        }

        if (records.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.padding(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.History,
                        null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.history_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.history_empty_body),
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(records, key = { it.timestamp.toString() + it.value.hashCode() }) { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = record },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                                        Icons.Rounded.QrCode2,
                                        null,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        record.value,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        record.format + " • " +
                                            DateFormat.getDateTimeInstance(
                                                DateFormat.SHORT,
                                                DateFormat.SHORT
                                            ).format(Date(record.timestamp)),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { ResultDialog(it) { selected = null } }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_history)) },
            text = { Text(stringResource(R.string.clear_history_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClear()
                        confirmClear = false
                    }
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsScreen(
    themeMode: String,
    dynamicColors: Boolean,
    onThemeMode: (String) -> Unit,
    onDynamicColors: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var sound by remember { mutableStateOf(prefs.getBoolean("sound", true)) }
    var haptics by remember { mutableStateOf(prefs.getBoolean("haptics", true)) }
    var showLanguages by remember { mutableStateOf(false) }
    val currentLanguage = prefs.getString("language", "").orEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PageHeader(title = stringResource(R.string.settings_title))

        SettingsSection(
            icon = Icons.Rounded.Palette,
            title = stringResource(R.string.appearance)
        ) {
            Text(
                stringResource(R.string.theme),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "system" to stringResource(R.string.system_default),
                    "light" to stringResource(R.string.light),
                    "dark" to stringResource(R.string.dark)
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = themeMode == value,
                        onClick = { onThemeMode(value) },
                        label = { Text(label) },
                        leadingIcon = if (themeMode == value) {
                            { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SettingSwitch(
                title = stringResource(R.string.dynamic_colors),
                subtitle = stringResource(R.string.dynamic_colors_desc),
                checked = dynamicColors,
                onCheckedChange = onDynamicColors
            )
        }

        SettingsSection(
            icon = Icons.Rounded.Translate,
            title = stringResource(R.string.language)
        ) {
            ListItem(
                headlineContent = { Text(languageName(currentLanguage, context)) },
                supportingContent = { Text(stringResource(R.string.choose_language)) },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showLanguages = true }
            )
        }

        SettingsSection(
            icon = Icons.Rounded.VolumeUp,
            title = stringResource(R.string.feedback)
        ) {
            SettingSwitch(
                title = stringResource(R.string.sound),
                checked = sound,
                onCheckedChange = {
                    sound = it
                    prefs.edit().putBoolean("sound", it).apply()
                }
            )
            HorizontalDivider()
            SettingSwitch(
                title = stringResource(R.string.haptics),
                checked = haptics,
                icon = Icons.Rounded.Vibration,
                onCheckedChange = {
                    haptics = it
                    prefs.edit().putBoolean("haptics", it).apply()
                }
            )
        }

        SettingsSection(
            icon = Icons.Rounded.Info,
            title = stringResource(R.string.privacy)
        ) {
            Text(
                stringResource(R.string.privacy_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsSection(
            icon = Icons.Rounded.QrCode2,
            title = stringResource(R.string.about)
        ) {
            Text(stringResource(R.string.version))
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showLanguages) {
        LanguageDialog(
            current = currentLanguage,
            onDismiss = { showLanguages = false },
            onSelected = { code ->
                prefs.edit().putString("language", code).apply()
                AppCompatDelegate.setApplicationLocales(
                    if (code.isBlank()) LocaleListCompat.getEmptyLocaleList()
                    else LocaleListCompat.forLanguageTags(code)
                )
                showLanguages = false
            }
        )
    }
}

@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 7.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    icon: ImageVector? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.padding(end = 10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LanguageDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    val languages = listOf(
        "" to stringResource(R.string.system_language),
        "cs" to "Čeština",
        "en" to "English",
        "sk" to "Slovenčina",
        "pl" to "Polski",
        "de" to "Deutsch",
        "es" to "Español"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_language)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(languages) { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = current == code,
                                onClick = { onSelected(code) }
                            )
                            .padding(vertical = 13.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, Modifier.weight(1f))
                        if (current == code) {
                            Icon(Icons.Rounded.Check, null)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

private fun languageName(code: String, context: Context): String = when (code) {
    "cs" -> "Čeština"
    "en" -> "English"
    "sk" -> "Slovenčina"
    "pl" -> "Polski"
    "de" -> "Deutsch"
    "es" -> "Español"
    else -> context.getString(R.string.system_language)
}

private fun playScanFeedback(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    if (prefs.getBoolean("sound", true)) {
        runCatching {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
                .startTone(ToneGenerator.TONE_PROP_BEEP, 110)
        }
    }

    if (prefs.getBoolean("haptics", true)) {
        runCatching {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    55L,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
    }
}

private fun shareText(context: Context, value: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, value)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
}

private fun openUrl(context: Context, value: String) {
    val url = if (
        value.startsWith("http://", true) ||
        value.startsWith("https://", true)
    ) value else "https://" + value

    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private fun generateCodeBitmap(data: String, format: GenerateFormat): Bitmap {
    val width = if (format.square) 1200 else 1500
    val height = if (format.square) 1200 else 650
    val matrix = MultiFormatWriter().encode(
        data,
        format.zxing,
        width,
        height,
        mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
    )
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            pixels[offset + x] = if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
        }
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
    return runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "QR_" + System.currentTimeMillis() + ".png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/QR Scanner")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return false

        context.contentResolver.openOutputStream(uri)?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        } ?: return false

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
        true
    }.getOrDefault(false)
}

private fun shareBitmap(context: Context, bitmap: Bitmap) {
    runCatching {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "qr_code.png")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.share_image))
        )
    }
}
