package com.estrongs.android.pop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.estrongs.android.pop.data.model.FileItem
import com.estrongs.android.pop.ui.theme.ESBlue
import com.estrongs.android.pop.ui.theme.ESDocBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class EditorTheme(val label: String, val bg: Color, val text: Color) {
    DEFAULT("ES Light", Color(0xFFF8F9FA), Color(0xFF1E293B)),
    DARK("Dark Matrix", Color(0xFF121824), Color(0xFFE2E8F0)),
    HACKER("Hacker Green", Color(0xFF0F172A), Color(0xFF4ADE80)),
    SEPIA("Warm Sepia", Color(0xFFFBF0D9), Color(0xFF433422))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ESNoteEditorDialog(
    file: FileItem?,
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onSaveAs: (String, String) -> Unit = { _, _ -> }
) {
    var contentValue by remember(initialContent) { mutableStateOf(TextFieldValue(initialContent)) }
    val history = remember { mutableStateListOf<String>() }
    var historyIndex by remember { mutableIntStateOf(-1) }

    // Track unsaved modifications
    val isModified = contentValue.text != initialContent

    // Editor Settings
    var isReadOnly by remember { mutableStateOf(false) }
    var showLineNumbers by remember { mutableStateOf(true) }
    var wordWrap by remember { mutableStateOf(true) }
    var fontSizeSp by remember { mutableFloatStateOf(14f) }
    var currentEncoding by remember { mutableStateOf("UTF-8") }
    var currentTheme by remember { mutableStateOf(EditorTheme.DEFAULT) }

    // Search & Replace state
    var showSearchReplace by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var matchCase by remember { mutableStateOf(false) }
    var searchMatches by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentMatchIndex by remember { mutableIntStateOf(-1) }

    // Go to line dialog state
    var showGoToLineDialog by remember { mutableStateOf(false) }
    var targetLineInput by remember { mutableStateOf("") }

    // Save As dialog state
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var saveAsFilename by remember { mutableStateOf(file?.name ?: "untitled.txt") }

    // Show encoding picker menu
    var showEncodingMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    // Compute line count, word count, character count
    val lines = remember(contentValue.text) {
        val split = contentValue.text.split("\n")
        if (split.isEmpty()) listOf("") else split
    }
    val lineCount = lines.size
    val charCount = contentValue.text.length
    val wordCount = remember(contentValue.text) {
        if (contentValue.text.isBlank()) 0
        else contentValue.text.trim().split("\\s+".toRegex()).size
    }

    // Initialize history
    LaunchedEffect(Unit) {
        history.add(initialContent)
        historyIndex = 0
    }

    // Helper to push history
    fun updateContentWithHistory(newText: String) {
        if (newText != contentValue.text) {
            // Cut future history if we were in the middle
            while (history.size > historyIndex + 1) {
                history.removeLast()
            }
            history.add(newText)
            if (history.size > 50) {
                history.removeAt(0)
            } else {
                historyIndex++
            }
            contentValue = TextFieldValue(newText)
        }
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            val prev = history[historyIndex]
            contentValue = TextFieldValue(prev)
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            val next = history[historyIndex]
            contentValue = TextFieldValue(next)
        }
    }

    // Search calculation
    LaunchedEffect(searchQuery, contentValue.text, matchCase) {
        if (searchQuery.isNotEmpty()) {
            val fullText = contentValue.text
            val matches = mutableListOf<Int>()
            var startIndex = 0
            val target = if (matchCase) searchQuery else searchQuery.lowercase()
            val source = if (matchCase) fullText else fullText.lowercase()

            while (true) {
                val index = source.indexOf(target, startIndex)
                if (index == -1) break
                matches.add(index)
                startIndex = index + target.length
            }
            searchMatches = matches
            currentMatchIndex = if (matches.isNotEmpty()) 0 else -1
        } else {
            searchMatches = emptyList()
            currentMatchIndex = -1
        }
    }

    fun findNext() {
        if (searchMatches.isNotEmpty()) {
            currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size
            val matchPos = searchMatches[currentMatchIndex]
            contentValue = contentValue.copy(
                selection = androidx.compose.ui.text.TextRange(matchPos, matchPos + searchQuery.length)
            )
        }
    }

    fun findPrev() {
        if (searchMatches.isNotEmpty()) {
            currentMatchIndex = if (currentMatchIndex <= 0) searchMatches.size - 1 else currentMatchIndex - 1
            val matchPos = searchMatches[currentMatchIndex]
            contentValue = contentValue.copy(
                selection = androidx.compose.ui.text.TextRange(matchPos, matchPos + searchQuery.length)
            )
        }
    }

    fun replaceCurrent() {
        if (searchMatches.isNotEmpty() && currentMatchIndex in searchMatches.indices) {
            val matchPos = searchMatches[currentMatchIndex]
            val newText = StringBuilder(contentValue.text).apply {
                replace(matchPos, matchPos + searchQuery.length, replaceQuery)
            }.toString()
            updateContentWithHistory(newText)
        }
    }

    fun replaceAll() {
        if (searchQuery.isNotEmpty()) {
            val regex = if (matchCase) Regex.fromLiteral(searchQuery) else Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE)
            val newText = contentValue.text.replace(regex, replaceQuery)
            updateContentWithHistory(newText)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .testTag("es_note_editor_container"),
            shape = RoundedCornerShape(16.dp),
            color = currentTheme.bg,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    color = ESBlue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = file?.name ?: "ES Note Editor",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                        if (isModified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFEF4444)
                                            ) {
                                                Text(
                                                    text = "MODIFIED",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (isReadOnly) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF64748B)
                                            ) {
                                                Text(
                                                    text = "READ-ONLY",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = file?.path ?: "In-memory document",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f),
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.testTag("editor_close_btn")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                                Button(
                                    onClick = { onSave(contentValue.text) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.testTag("editor_save_button"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Toolstrip Actions Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Undo / Redo
                        IconButton(
                            onClick = { undo() },
                            enabled = historyIndex > 0,
                            modifier = Modifier.size(36.dp).testTag("editor_undo_btn")
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { redo() },
                            enabled = historyIndex < history.size - 1,
                            modifier = Modifier.size(36.dp).testTag("editor_redo_btn")
                        ) {
                            Icon(Icons.Default.Redo, contentDescription = "Redo", modifier = Modifier.size(18.dp))
                        }

                        VerticalDivider(modifier = Modifier.height(20.dp))

                        // Find & Replace
                        FilterChip(
                            selected = showSearchReplace,
                            onClick = { showSearchReplace = !showSearchReplace },
                            label = { Text("Find & Replace", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("editor_search_chip")
                        )

                        // Line numbers toggle
                        FilterChip(
                            selected = showLineNumbers,
                            onClick = { showLineNumbers = !showLineNumbers },
                            label = { Text("Lines", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("editor_lines_chip")
                        )

                        // Word wrap toggle
                        FilterChip(
                            selected = wordWrap,
                            onClick = { wordWrap = !wordWrap },
                            label = { Text("Wrap", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.WrapText, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("editor_wrap_chip")
                        )

                        // Read-only / Edit Toggle
                        FilterChip(
                            selected = !isReadOnly,
                            onClick = { isReadOnly = !isReadOnly },
                            label = { Text(if (isReadOnly) "Read Only" else "Edit Mode", fontSize = 11.sp) },
                            leadingIcon = { Icon(if (isReadOnly) Icons.Default.Lock else Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("editor_readonly_chip")
                        )

                        // Font Size Zoom
                        IconButton(
                            onClick = { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(8f) },
                            modifier = Modifier.size(36.dp).testTag("editor_font_minus")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                        }
                        Text("${fontSizeSp.toInt()}sp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(32f) },
                            modifier = Modifier.size(36.dp).testTag("editor_font_plus")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                        }

                        VerticalDivider(modifier = Modifier.height(20.dp))

                        // Go to line
                        OutlinedButton(
                            onClick = { showGoToLineDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("editor_goto_line_btn")
                        ) {
                            Icon(Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Go to Line", fontSize = 11.sp)
                        }

                        // Insert Date & Time
                        OutlinedButton(
                            onClick = {
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                val timestamp = "\n[${sdf.format(Date())}]\n"
                                val newText = contentValue.text + timestamp
                                updateContentWithHistory(newText)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("editor_insert_time_btn")
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Insert Time", fontSize = 11.sp)
                        }

                        // Copy All
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(contentValue.text))
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("editor_copy_all_btn")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy All", fontSize = 11.sp)
                        }

                        // Save As
                        OutlinedButton(
                            onClick = { showSaveAsDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("editor_save_as_btn")
                        ) {
                            Icon(Icons.Default.SaveAs, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save As...", fontSize = 11.sp)
                        }

                        // Themes menu box
                        Box {
                            OutlinedButton(
                                onClick = { showThemeMenu = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("editor_theme_btn")
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(currentTheme.label, fontSize = 11.sp)
                            }
                            DropdownMenu(
                                expanded = showThemeMenu,
                                onDismissRequest = { showThemeMenu = false }
                            ) {
                                EditorTheme.values().forEach { theme ->
                                    DropdownMenuItem(
                                        text = { Text(theme.label) },
                                        onClick = {
                                            currentTheme = theme
                                            showThemeMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Encoding Box
                        Box {
                            OutlinedButton(
                                onClick = { showEncodingMenu = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("editor_encoding_btn")
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(currentEncoding, fontSize = 11.sp)
                            }
                            DropdownMenu(
                                expanded = showEncodingMenu,
                                onDismissRequest = { showEncodingMenu = false }
                            ) {
                                listOf("UTF-8", "GBK", "ISO-8859-1", "UTF-16", "US-ASCII", "Windows-1252").forEach { enc ->
                                    DropdownMenuItem(
                                        text = { Text(enc) },
                                        onClick = {
                                            currentEncoding = enc
                                            showEncodingMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Search & Replace Expandable Panel
                if (showSearchReplace) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Find text...", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("editor_find_input"),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { findNext() })
                                )

                                Text(
                                    text = if (searchMatches.isEmpty()) "0/0" else "${currentMatchIndex + 1}/${searchMatches.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (searchMatches.isNotEmpty()) ESBlue else Color.Gray
                                )

                                IconButton(onClick = { findPrev() }, modifier = Modifier.size(36.dp).testTag("editor_find_prev")) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous")
                                }
                                IconButton(onClick = { findNext() }, modifier = Modifier.size(36.dp).testTag("editor_find_next")) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                                }
                                IconButton(onClick = { showSearchReplace = false }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }

                            if (!isReadOnly) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = replaceQuery,
                                        onValueChange = { replaceQuery = it },
                                        placeholder = { Text("Replace with...", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("editor_replace_input")
                                    )

                                    Button(
                                        onClick = { replaceCurrent() },
                                        enabled = searchMatches.isNotEmpty(),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("editor_replace_one_btn")
                                    ) {
                                        Text("Replace", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = { replaceAll() },
                                        enabled = searchMatches.isNotEmpty(),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("editor_replace_all_btn")
                                    ) {
                                        Text("All", fontSize = 11.sp)
                                    }

                                    FilterChip(
                                        selected = matchCase,
                                        onClick = { matchCase = !matchCase },
                                        label = { Text("Aa", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }

                // Main Text Area with Line Numbers Column
                val scrollState = rememberScrollState()
                val horizontalScrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(currentTheme.bg)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Line Numbers column
                        if (showLineNumbers) {
                            Column(
                                modifier = Modifier
                                    .width(44.dp)
                                    .fillMaxHeight()
                                    .background(currentTheme.bg.copy(alpha = 0.8f))
                                    .verticalScroll(scrollState)
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                for (i in 1..lineCount) {
                                    Text(
                                        text = "$i",
                                        color = Color(0xFF94A3B8),
                                        fontSize = fontSizeSp.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = (fontSizeSp * 1.4f).sp
                                    )
                                }
                            }
                            VerticalDivider(color = Color(0xFFCBD5E1).copy(alpha = 0.5f))
                        }

                        // Editable or Read-only content field
                        val editorModifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .then(if (!wordWrap) Modifier.horizontalScroll(horizontalScrollState) else Modifier)
                            .verticalScroll(scrollState)
                            .testTag("editor_main_text_field")

                        TextField(
                            value = contentValue,
                            onValueChange = { newValue ->
                                if (!isReadOnly) {
                                    updateContentWithHistory(newValue.text)
                                    contentValue = newValue
                                }
                            },
                            readOnly = isReadOnly,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSizeSp.sp,
                                color = currentTheme.text,
                                lineHeight = (fontSizeSp * 1.4f).sp
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = editorModifier,
                            placeholder = { Text("Type note or script code here...", color = Color.Gray) }
                        )
                    }
                }

                // Footer Status Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Lines: $lineCount", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("Words: $wordCount", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("Chars: $charCount", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(currentEncoding, fontSize = 11.sp, color = ESDocBlue, fontWeight = FontWeight.Bold)
                            Text(if (wordWrap) "Wrapped" else "No-Wrap", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    // Go to line sub-dialog
    if (showGoToLineDialog) {
        AlertDialog(
            onDismissRequest = { showGoToLineDialog = false },
            title = { Text("Go to Line") },
            text = {
                OutlinedTextField(
                    value = targetLineInput,
                    onValueChange = { targetLineInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Line number (1 - $lineCount)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goto_line_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetLine = targetLineInput.toIntOrNull()
                        if (targetLine != null && targetLine in 1..lineCount) {
                            var charOffset = 0
                            for (i in 0 until (targetLine - 1)) {
                                charOffset += lines[i].length + 1
                            }
                            contentValue = contentValue.copy(
                                selection = androidx.compose.ui.text.TextRange(charOffset, charOffset)
                            )
                        }
                        showGoToLineDialog = false
                    },
                    modifier = Modifier.testTag("goto_line_confirm")
                ) {
                    Text("Jump")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoToLineDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Save As sub-dialog
    if (showSaveAsDialog) {
        AlertDialog(
            onDismissRequest = { showSaveAsDialog = false },
            title = { Text("Save Document As") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter new file name with extension:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = saveAsFilename,
                        onValueChange = { saveAsFilename = it },
                        label = { Text("File Name (e.g. script.py, log.txt)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("save_as_filename_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (saveAsFilename.isNotBlank()) {
                            onSaveAs(saveAsFilename.trim(), contentValue.text)
                            showSaveAsDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_as_confirm_btn")
                ) {
                    Text("Save As")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveAsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
