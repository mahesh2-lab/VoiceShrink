package com.voiceshrink.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings as SystemSettings
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class SortOrder(val label: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    LARGEST("Largest Size"),
    SMALLEST("Smallest Size"),
    NAME("Name (A-Z)");
}

enum class CurrentScreen {
    MAIN,
    SETTINGS,
    STATS
}

class MainActivity : ComponentActivity() {

    private val folderUriState = mutableStateOf<Uri?>(null)
    private var audioPlayer: AudioPlayer? = null
    val snackbarHostState = SnackbarHostState()

    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            Prefs.setTreeUri(this, uri)
            folderUriState.value = uri
            Scheduler.onFolderConfigured(this)
        } else {
            lifecycleScope.launch {
                snackbarHostState.showSnackbar("No folder selected. Pick a folder to start auto-compression.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        folderUriState.value = Prefs.treeUri(this)
        if (folderUriState.value != null) {
            Scheduler.schedulePeriodic(this)
            Scheduler.scanNow(this)
        }

        val dao = AppDb.get(this).dao()

        setContent {
            val scope = rememberCoroutineScope()
            val player = remember {
                AudioPlayer(this@MainActivity, scope).also { audioPlayer = it }
            }

            DisposableEffect(Unit) {
                onDispose {
                    player.release()
                }
            }

            MaterialTheme {
                MainScreen(
                    context = this@MainActivity,
                    dao = dao,
                    folderUri = folderUriState.value,
                    player = player,
                    snackbarHostState = snackbarHostState,
                    onPickFolder = { pickFolder.launch(null) },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer?.release()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    context: Context,
    dao: RecordingDao,
    folderUri: Uri?,
    player: AudioPlayer,
    snackbarHostState: SnackbarHostState,
    onPickFolder: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val recordings by dao.getAll().collectAsState(initial = emptyList())
    var currentScreen by remember { mutableStateOf(CurrentScreen.MAIN) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedSort by remember { mutableStateOf(SortOrder.NEWEST) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<Recording?>(null) }
    var selectedDetailRecording by remember { mutableStateOf<Recording?>(null) }

    val hidePhoneNumbers = remember { Prefs.hidePhoneNumbers(context) }

    val focusRequester = remember { FocusRequester() }

    // Auto-clean any corrupt false duplicates and trigger background scan on load
    LaunchedEffect(Unit) {
        dao.cleanCorruptedDuplicates()
        if (folderUri != null) {
            Scheduler.scanNow(context)
        }
    }

    // Filter, search, and sort logic
    val filteredRecordings = remember(recordings, selectedFilter, searchQuery, selectedSort) {
        val filtered = recordings.filter { rec ->
            val matchesFilter = when (selectedFilter) {
                "DONE" -> rec.status == Status.DONE
                "PENDING" -> rec.status == Status.PENDING
                "FAILED" -> rec.status == Status.FAILED
                else -> true
            }

            val parsed = parseRecordingName(rec.name)
            val q = searchQuery.trim()
            val cleanQ = q.filter { it.isDigit() }

            val matchesQuery = q.isBlank() ||
                rec.name.contains(q, ignoreCase = true) ||
                parsed.contact.contains(q, ignoreCase = true) ||
                (parsed.number != null && (
                    parsed.number.contains(q, ignoreCase = true) ||
                    (cleanQ.isNotEmpty() && parsed.number.filter { it.isDigit() }.contains(cleanQ))
                )) ||
                formatDate(rec.lastModified).contains(q, ignoreCase = true)

            matchesFilter && matchesQuery
        }

        when (selectedSort) {
            SortOrder.NEWEST -> filtered.sortedWith(compareByDescending<Recording> { it.lastModified }.thenBy { it.name })
            SortOrder.OLDEST -> filtered.sortedWith(compareBy<Recording> { it.lastModified }.thenBy { it.name })
            SortOrder.LARGEST -> filtered.sortedWith(compareByDescending<Recording> { if (it.status == Status.DONE && it.outSizeBytes > 0) it.outSizeBytes else it.sizeBytes }.thenBy { it.name })
            SortOrder.SMALLEST -> filtered.sortedWith(compareBy<Recording> { if (it.status == Status.DONE && it.outSizeBytes > 0) it.outSizeBytes else it.sizeBytes }.thenBy { it.name })
            SortOrder.NAME -> filtered.sortedBy { it.name.lowercase(Locale.getDefault()) }
        }
    }

    // Counts for chips
    val countDone = recordings.count { it.status == Status.DONE }
    val countPending = recordings.count { it.status == Status.PENDING }
    val countFailed = recordings.count { it.status == Status.FAILED }

    // Storage savings stats
    val originalBytesDone = recordings.filter { it.status == Status.DONE }.sumOf { it.sizeBytes }
    val compressedBytesDone = recordings.filter { it.status == Status.DONE }.sumOf { it.outSizeBytes }
    val savedBytes = (originalBytesDone - compressedBytesDone).coerceAtLeast(0L)
    val savedPercentage = if (originalBytesDone > 0) ((savedBytes.toDouble() / originalBytesDone) * 100).toInt() else 0

    when (currentScreen) {
        CurrentScreen.SETTINGS -> {
            SettingsScreen(
                context = context,
                folderUri = folderUri,
                savedBytes = savedBytes,
                savedPercentage = savedPercentage,
                onOpenStats = { currentScreen = CurrentScreen.STATS },
                onClose = { currentScreen = CurrentScreen.MAIN },
                onPickFolder = onPickFolder,
            )
        }

        CurrentScreen.STATS -> {
            CompressionDetailsScreen(
                recordings = recordings,
                savedBytes = savedBytes,
                savedPercentage = savedPercentage,
                originalBytesDone = originalBytesDone,
                compressedBytesDone = compressedBytesDone,
                onClose = { currentScreen = CurrentScreen.SETTINGS },
                onOpenSettings = { currentScreen = CurrentScreen.SETTINGS },
                onSelectRecording = { rec -> selectedDetailRecording = rec },
                player = player,
                onRetry = { rec ->
                    scope.launch {
                        dao.markFailed(rec.id, null)
                        Scheduler.processNow(context)
                    }
                },
            )
        }

        CurrentScreen.MAIN -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
                                val pulseScale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = if (countPending > 0) 1.08f else 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "scale"
                                )
                                Image(
                                    painter = painterResource(id = R.drawable.ic_app_logo),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .graphicsLayer(
                                            scaleX = pulseScale,
                                            scaleY = pulseScale,
                                        )
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(stringResource(R.string.title_call_recordings), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    folderUri?.let {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.Folder,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                formatFolderPath(it.toString()),
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { isSearchVisible = !isSearchVisible },
                                modifier = Modifier.size(48.dp).minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_icon_desc))
                            }
                            IconButton(
                                onClick = { currentScreen = CurrentScreen.SETTINGS },
                                modifier = Modifier.size(48.dp).minimumInteractiveComponentSize(),
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.settings_icon_desc),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                },
                bottomBar = {
                    // Sticky audio mini-player
                    AnimatedVisibility(visible = player.currentRecording != null) {
                        player.currentRecording?.let { rec ->
                            MiniPlayerBar(recording = rec, player = player)
                        }
                    }
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .widthIn(max = 840.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally),
                ) {
                    // Sleek 1-line rounded M3 Search Bar
                    AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search name, number or date...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(48.dp).minimumInteractiveComponentSize(),
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear_desc))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(28.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .focusRequester(focusRequester),
                        )
                    }

                    LaunchedEffect(isSearchVisible) {
                        if (isSearchVisible) {
                            focusRequester.requestFocus()
                        } else {
                            searchQuery = ""
                        }
                    }

                    // Filter Chips & Sort Bar
                    if (folderUri != null && recordings.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedFilter == "ALL",
                                        onClick = { selectedFilter = "ALL" },
                                        label = { Text(stringResource(R.string.filter_all, recordings.size)) },
                                        leadingIcon = if (selectedFilter == "ALL") {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = selectedFilter == "DONE",
                                        onClick = { selectedFilter = "DONE" },
                                        label = { Text(stringResource(R.string.filter_compressed, countDone)) },
                                        leadingIcon = if (selectedFilter == "DONE") {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = selectedFilter == "PENDING",
                                        onClick = { selectedFilter = "PENDING" },
                                        label = { Text(stringResource(R.string.filter_queued, countPending)) },
                                        leadingIcon = if (selectedFilter == "PENDING") {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                    )
                                }
                                if (countFailed > 0) {
                                    item {
                                        FilterChip(
                                            selected = selectedFilter == "FAILED",
                                            onClick = { selectedFilter = "FAILED" },
                                            label = { Text(stringResource(R.string.filter_failed, countFailed)) },
                                            leadingIcon = if (selectedFilter == "FAILED") {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                                                selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
                                            ),
                                        )
                                    }
                                }
                            }

                            // Sort dropdown icon button
                            Box {
                                IconButton(
                                    onClick = { isSortMenuExpanded = true },
                                    modifier = Modifier.size(48.dp).minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = stringResource(R.string.sort_icon_desc),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                DropdownMenu(
                                    expanded = isSortMenuExpanded,
                                    onDismissRequest = { isSortMenuExpanded = false },
                                ) {
                                    SortOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    order.label,
                                                    fontWeight = if (selectedSort == order) FontWeight.Bold else FontWeight.Normal,
                                                )
                                            },
                                            onClick = {
                                                selectedSort = order
                                                isSortMenuExpanded = false
                                            },
                                            leadingIcon = if (selectedSort == order) {
                                                { Icon(Icons.Default.Check, contentDescription = null) }
                                            } else null,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Content Area / Empty States / List with Pull-to-Refresh
                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            folderUri == null -> {
                                EmptyStateNoFolder(onPickFolder = onPickFolder)
                            }
                            recordings.isEmpty() -> {
                                EmptyStateNoRecordings(
                                    isScanning = isScanning,
                                    onScan = {
                                        scope.launch {
                                            isScanning = true
                                            Scheduler.scanNow(context)
                                            delay(800L)
                                            isScanning = false
                                        }
                                    },
                                    onChangeFolder = onPickFolder,
                                )
                            }
                            filteredRecordings.isEmpty() -> {
                                EmptyStateNoFilteredResults(
                                    searchQuery = searchQuery,
                                    onClearSearch = { searchQuery = ""; selectedFilter = "ALL" },
                                )
                            }
                            else -> {
                                PullToRefreshBox(
                                    isRefreshing = isRefreshing,
                                    onRefresh = {
                                        scope.launch {
                                            isRefreshing = true
                                            Scheduler.scanNow(context)
                                            delay(800L)
                                            isRefreshing = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        items(filteredRecordings, key = { it.id }) { rec ->
                                            RecordingCard(
                                                recording = rec,
                                                isPlaying = player.currentRecording?.id == rec.id && player.isPlaying,
                                                hideNumbers = hidePhoneNumbers,
                                                onPlayToggle = { player.play(rec) },
                                                onShowDetails = { selectedDetailRecording = rec },
                                                onShare = { shareRecording(context, rec) },
                                                onDelete = { deleteCandidate = rec },
                                                onRetry = {
                                                    scope.launch {
                                                        dao.markFailed(rec.id, null)
                                                        Scheduler.processNow(context)
                                                    }
                                                },
                                                onCompressNow = {
                                                    Scheduler.processNow(context)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Single Recording Details Sheet
    selectedDetailRecording?.let { rec ->
        RecordingDetailBottomSheet(
            recording = rec,
            isPlaying = player.currentRecording?.id == rec.id && player.isPlaying,
            onPlayToggle = { player.play(rec) },
            onShare = { shareRecording(context, rec) },
            onDismiss = { selectedDetailRecording = null },
        )
    }

    // Delete Confirmation Dialog with explicit delete scope text
    deleteCandidate?.let { rec ->
        val deleteMsg = if (Prefs.deleteOriginal(context)) {
            stringResource(R.string.delete_dialog_both, rec.name)
        } else {
            stringResource(R.string.delete_dialog_list_only, rec.name)
        }

        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.delete_recording_dialog_title)) },
            text = { Text(deleteMsg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (player.currentRecording?.id == rec.id) player.stop()
                            dao.deleteById(rec.id)
                            deleteCandidate = null
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
fun RecordingCard(
    recording: Recording,
    isPlaying: Boolean,
    hideNumbers: Boolean,
    onPlayToggle: () -> Unit,
    onShowDetails: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onCompressNow: () -> Unit,
) {
    val statusColors = statusColors()
    val saved = (recording.sizeBytes - recording.outSizeBytes).coerceAtLeast(0L)
    val pct = if (recording.sizeBytes > 0) ((saved.toDouble() / recording.sizeBytes) * 100).toInt() else 0

    val parsed = parseRecordingName(recording.name)
    val displayContact = parsed.contact
    val displaySubNumber = parsed.number?.let {
        if (hideNumbers) maskPhoneNumber(it) else it
    }

    val semanticText = when (recording.status) {
        Status.DONE -> stringResource(R.string.semantics_card_done, displayContact, pct)
        Status.PENDING -> stringResource(R.string.semantics_card_queued, displayContact)
        Status.FAILED -> stringResource(R.string.semantics_card_failed, displayContact)
        else -> displayContact
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = semanticText
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status Icon Container
            val (bgColor, contentColor) = when (recording.status) {
                Status.DONE -> statusColors.doneContainer to statusColors.onDoneContainer
                Status.PENDING -> statusColors.queuedContainer to statusColors.onQueuedContainer
                Status.FAILED -> statusColors.failedContainer to statusColors.onFailedContainer
                else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center,
            ) {
                when (recording.status) {
                    Status.DONE -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.status_done),
                        tint = contentColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Status.PENDING -> CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = contentColor,
                    )
                    Status.FAILED -> Icon(
                        Icons.Default.Error,
                        contentDescription = stringResource(R.string.status_failed),
                        tint = contentColor,
                        modifier = Modifier.size(24.dp),
                    )
                    else -> Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle Column (Clean 2-Line Layout)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onShowDetails() },
            ) {
                // Line 1: Primary Title (Contact / Title, 1 line, ellipsis)
                Text(
                    text = displayContact,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Line 2: Phone Number, Duration, Date, File Size
                val displaySize = if (recording.status == Status.DONE && recording.outSizeBytes > 0) {
                    recording.outSizeBytes
                } else {
                    recording.sizeBytes
                }

                val metaDetails = buildString {
                    if (displaySubNumber != null && displaySubNumber != displayContact) {
                        append(displaySubNumber)
                        append(" • ")
                    }
                    if (recording.durationMs > 0L) {
                        append(formatDuration(recording.durationMs))
                        append(" • ")
                    }
                    append(formatDate(recording.lastModified))
                    append(" • ")
                    append(formatBytes(displaySize))
                }

                Text(
                    text = metaDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Right Action Controls
            if (recording.status == Status.FAILED) {
                FilledTonalButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = statusColors.failedContainer,
                        contentColor = statusColors.onFailedContainer,
                    ),
                    modifier = Modifier.height(48.dp).minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.retry_button), style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalIconButton(
                        onClick = onPlayToggle,
                        modifier = Modifier.size(48.dp).minimumInteractiveComponentSize(),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        if (isPlaying) {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = stringResource(R.string.pause_desc, displayContact),
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.play_desc, displayContact),
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }

                    Box {
                        var isMenuExpanded by remember { mutableStateOf(false) }

                        IconButton(
                            onClick = { isMenuExpanded = true },
                            modifier = Modifier.size(48.dp).minimumInteractiveComponentSize(),
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_options_desc),
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                        ) {
                            if (recording.status == Status.PENDING) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.compress_now)) },
                                    onClick = {
                                        isMenuExpanded = false
                                        onCompressNow()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Details") },
                                onClick = {
                                    isMenuExpanded = false
                                    onShowDetails()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_share)) },
                                onClick = {
                                    isMenuExpanded = false
                                    onShare()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    isMenuExpanded = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayerBar(recording: Recording, player: AudioPlayer) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${formatDuration(player.currentPositionMs)} / ${formatDuration(player.durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = { player.seekBackward(10_000L) },
                        modifier = Modifier.size(48.dp).minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", modifier = Modifier.size(22.dp))
                    }

                    IconButton(
                        onClick = {
                            if (player.isPlaying) player.pause() else player.resume()
                        },
                        modifier = Modifier.size(48.dp).minimumInteractiveComponentSize(),
                    ) {
                        if (player.isPlaying) {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = "Pause playback",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Resume playback",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    IconButton(
                        onClick = { player.seekForward(10_000L) },
                        modifier = Modifier.size(48.dp).minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", modifier = Modifier.size(22.dp))
                    }

                    IconButton(
                        onClick = { player.stop() },
                        modifier = Modifier.size(48.dp).minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close player", modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = player.currentPositionMs.toFloat().coerceIn(0f, player.durationMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { player.seekTo(it.toLong()) },
                valueRange = 0f..player.durationMs.toFloat().coerceAtLeast(1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressionDetailsScreen(
    recordings: List<Recording>,
    savedBytes: Long,
    savedPercentage: Int,
    originalBytesDone: Long,
    compressedBytesDone: Long,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectRecording: (Recording) -> Unit,
    onRetry: (Recording) -> Unit,
    player: AudioPlayer,
) {
    val doneRecordings = remember(recordings) { recordings.filter { it.status == Status.DONE } }
    val pendingRecordings = remember(recordings) { recordings.filter { it.status == Status.PENDING } }
    val failedRecordings = remember(recordings) { recordings.filter { it.status == Status.FAILED } }
    val statusColors = statusColors()

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_compression_details), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp).minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(48.dp).minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_icon_desc),
                        )
                    }
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = player.currentRecording != null) {
                player.currentRecording?.let { rec ->
                    MiniPlayerBar(recording = rec, player = player)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Main Savings Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.saved_banner_title, formatBytes(savedBytes)),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = stringResource(R.string.saved_banner_subtitle, savedPercentage),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) {
                                Text(
                                    "-$savedPercentage%",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { if (originalBytesDone > 0) (compressedBytesDone.toFloat() / originalBytesDone.toFloat()).coerceIn(0f, 1f) else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                stringResource(R.string.original_size, formatBytes(originalBytesDone)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                            Text(
                                stringResource(R.string.compressed_size, formatBytes(compressedBytesDone)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            // Stat Metrics Grid (Equal Height Row)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(stringResource(R.string.tile_done), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${doneRecordings.size}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = statusColors.done)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(stringResource(R.string.tile_in_queue), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${pendingRecordings.size}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = statusColors.queued)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(stringResource(R.string.tile_failed), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${failedRecordings.size}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (failedRecordings.isNotEmpty()) statusColors.failed else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Waiting Section (if queued items exist)
            if (pendingRecordings.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.waiting_section_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(pendingRecordings, key = { "pending_${it.id}" }) { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = statusColors.queuedContainer.copy(alpha = 0.4f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Queued • ${formatBytes(rec.sizeBytes)}", style = MaterialTheme.typography.bodySmall, color = statusColors.queued)
                            }
                        }
                    }
                }
            }

            // Failed Section (if failed items exist)
            if (failedRecordings.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.failed_section_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = statusColors.failed)
                }
                items(failedRecordings, key = { "failed_${it.id}" }) { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = statusColors.failedContainer.copy(alpha = 0.4f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(rec.error ?: stringResource(R.string.status_failed), style = MaterialTheme.typography.bodySmall, color = statusColors.failed)
                            }
                            FilledTonalButton(
                                onClick = { onRetry(rec) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(stringResource(R.string.retry_button), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Compression History List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.compressed_recordings_list), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        pluralStringResource(R.plurals.items_count, doneRecordings.size, doneRecordings.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (doneRecordings.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(stringResource(R.string.no_recordings_compressed_yet), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(doneRecordings, key = { it.id }) { rec ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRecording(rec) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(formatDate(rec.lastModified), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val saved = (rec.sizeBytes - rec.outSizeBytes).coerceAtLeast(0L)
                            val pct = if (rec.sizeBytes > 0) ((saved.toDouble() / rec.sizeBytes) * 100).toInt() else 0
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${formatBytes(rec.sizeBytes)} → ${formatBytes(rec.outSizeBytes)} (-$pct%)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = statusColors.done)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingDetailBottomSheet(
    recording: Recording,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.recording_details), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            val parsed = parseRecordingName(recording.name)
            Text(
                parsed.contact,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            // Info rows
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow(label = stringResource(R.string.date_and_time), value = formatDate(recording.lastModified))
                if (recording.durationMs > 0L) {
                    DetailRow(label = stringResource(R.string.duration_label), value = formatDuration(recording.durationMs))
                }
                DetailRow(label = stringResource(R.string.status_label), value = if (recording.status == Status.PENDING) stringResource(R.string.status_queued) else recording.status)
                DetailRow(label = stringResource(R.string.original_size, ""), value = formatBytes(recording.sizeBytes))

                if (recording.status == Status.DONE) {
                    DetailRow(label = stringResource(R.string.compressed_size, ""), value = formatBytes(recording.outSizeBytes))
                    val saved = (recording.sizeBytes - recording.outSizeBytes).coerceAtLeast(0L)
                    val pct = if (recording.sizeBytes > 0) ((saved.toDouble() / recording.sizeBytes) * 100).toInt() else 0
                    DetailRow(label = stringResource(R.string.space_saved), value = "${formatBytes(saved)} (-$pct%)")
                    DetailRow(label = stringResource(R.string.format_label), value = stringResource(R.string.format_opus_ogg))
                }

                DetailRow(label = stringResource(R.string.original_path), value = formatFolderPath(recording.uri))
                recording.outUri?.let {
                    DetailRow(label = stringResource(R.string.compressed_path), value = formatFolderPath(it))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onPlayToggle,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPlaying) stringResource(R.string.action_pause) else stringResource(R.string.action_play))
                }

                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_share))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun EmptyStateNoFolder(onPickFolder: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp)),
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        stringResource(R.string.welcome_privacy_line),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("1", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Text(
                            stringResource(R.string.welcome_step_1),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("2", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Text(
                            stringResource(R.string.welcome_step_2),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onPickFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .minimumInteractiveComponentSize(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(stringResource(R.string.welcome_button), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun EmptyStateNoRecordings(
    isScanning: Boolean,
    onScan: () -> Unit,
    onChangeFolder: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_app_logo),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp)),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.no_recordings_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.no_recordings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onScan,
                modifier = Modifier.height(48.dp).minimumInteractiveComponentSize(),
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.scan_now_button))
            }
            OutlinedButton(
                onClick = onChangeFolder,
                modifier = Modifier.height(48.dp).minimumInteractiveComponentSize(),
            ) {
                Text(stringResource(R.string.change_folder_button))
            }
        }
    }
}

@Composable
fun EmptyStateNoFilteredResults(searchQuery: String, onClearSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            if (searchQuery.isNotEmpty()) stringResource(R.string.no_filtered_results, searchQuery) else stringResource(R.string.no_category_results),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onClearSearch) {
            Text(stringResource(R.string.clear_filter_button))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    context: Context,
    folderUri: Uri?,
    savedBytes: Long,
    savedPercentage: Int,
    onOpenStats: () -> Unit,
    onClose: () -> Unit,
    onPickFolder: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val statusColors = statusColors()
    var bitrate by remember { mutableIntStateOf(Prefs.bitrate(context)) }
    var deleteOriginal by remember { mutableStateOf(Prefs.deleteOriginal(context)) }
    var requireCharging by remember { mutableStateOf(Prefs.requireCharging(context)) }
    var hidePhoneNumbers by remember { mutableStateOf(Prefs.hidePhoneNumbers(context)) }

    val powerManager = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var isIgnoringBatteryOpt by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOpt = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp).minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Compression Statistics Navigation Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenStats() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    stringResource(R.string.title_compression_details),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    if (savedBytes > 0) {
                                        "Saved ${formatBytes(savedBytes)} ($savedPercentage% reduction)"
                                    } else {
                                        "View compression savings and stats"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            // 2. Audio Bitrate Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(stringResource(R.string.bitrate_title), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.bitrate_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        listOf(
                            16_000 to stringResource(R.string.bitrate_16),
                            24_000 to stringResource(R.string.bitrate_24),
                            32_000 to stringResource(R.string.bitrate_32),
                        ).forEach { (rate, label) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = bitrate == rate,
                                    onClick = {
                                        bitrate = rate
                                        Prefs.setBitrate(context, rate)
                                    },
                                )
                                Text(label, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 3. Delete Original After Compression (with Warning)
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.delete_original_title), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.delete_original_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = deleteOriginal,
                            onCheckedChange = {
                                deleteOriginal = it
                                Prefs.setDeleteOriginal(context, it)
                            },
                        )
                    }

                    if (deleteOriginal) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.delete_original_warning),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            }

            // 4. Require Charging Switch
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.require_charging_title), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.require_charging_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = requireCharging,
                        onCheckedChange = {
                            requireCharging = it
                            Prefs.setRequireCharging(context, it)
                        },
                    )
                }
            }

            // 5. Hide Phone Numbers Switch
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.hide_phone_numbers_title), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.hide_phone_numbers_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = hidePhoneNumbers,
                        onCheckedChange = {
                            hidePhoneNumbers = it
                            Prefs.setHidePhoneNumbers(context, it)
                        },
                    )
                }
            }

            // 6. Watched Folder Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.watched_folder_title), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            formatFolderPath(folderUri?.toString()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = { onPickFolder(); onClose() }) {
                            Text(stringResource(R.string.change_folder_button))
                        }
                    }
                }
            }

            // 7. Battery Optimization Status Card with Explicit Status Confirmation
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            runCatching {
                                val intent = Intent(SystemSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isIgnoringBatteryOpt) {
                            statusColors.doneContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                    ),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.battery_opt_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isIgnoringBatteryOpt) statusColors.doneContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (isIgnoringBatteryOpt) statusColors.onDoneContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        if (isIgnoringBatteryOpt) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isIgnoringBatteryOpt) "Unrestricted" else "Optimized",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            if (isIgnoringBatteryOpt) {
                                "✓ Background compression is unrestricted. App can process recordings reliably."
                            } else {
                                "Configure battery optimization settings to prevent OS background limits from pausing compression."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 8. Maintenance Options
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.maintenance_title), fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                Scheduler.scanNow(context)
                                onClose()
                            },
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.rescan_folder_button), fontSize = 13.sp)
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch {
                                    AppDb.get(context).dao().cleanCorruptedDuplicates()
                                    Scheduler.scanNow(context)
                                }
                            },
                        ) {
                            Text(stringResource(R.string.clean_duplicates_button), fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

fun shareRecording(context: Context, rec: Recording) {
    val targetUri = rec.outUri?.let(Uri::parse) ?: Uri.parse(rec.uri)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, targetUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share recording"))
}
