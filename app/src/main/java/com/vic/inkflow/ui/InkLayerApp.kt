package com.vic.inkflow.ui

import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Article
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import com.vic.inkflow.ui.theme.InkFlowTheme
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.graphics.BitmapFactory
import com.vic.inkflow.data.AppDatabase
import com.vic.inkflow.data.ImageAnnotationEntity
import com.vic.inkflow.data.StrokeWithPoints
import com.vic.inkflow.data.TextAnnotationEntity
import com.vic.inkflow.util.PdfManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


// --- 1. App Navigation ---
@Composable
fun InkLayerApp(db: AppDatabase) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("inkflow_settings", 0) }
    var isDarkTheme by rememberSaveable { mutableStateOf(prefs.getBoolean("isDarkTheme", false)) }
    InkFlowTheme(darkTheme = isDarkTheme) {
        val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(350)) + fadeIn(animationSpec = tween(350)) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350)) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(350)) + fadeIn(animationSpec = tween(350)) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350)) }
    ) {
        composable("home") {
            DocumentLibraryScreen(
                navController = navController,
                db = db,
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = {
                    val newValue = !isDarkTheme
                    isDarkTheme = newValue
                    prefs.edit().putBoolean("isDarkTheme", newValue).apply()
                }
            )
        }
        composable(
            route = "editor/{pdfUri}",
            arguments = listOf(navArgument("pdfUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val pdfUri = backStackEntry.arguments?.getString("pdfUri")
                ?.let { Uri.parse(it) }

            if (pdfUri != null) {
                TabletEditorScreen(
                    navController = navController,
                    uri = pdfUri,
                    db = db
                )
            }
        }
    }
    }
}

// --- 2. Home Screen ---

/**
 * Full-screen flowing gradient that always covers the entire component.
 *
 * Instead of moving start/end coordinates (which leaves gaps), this keeps
 * the gradient anchored at Offset.Zero → Offset.Infinite (auto-expands to
 * actual layout bounds) and cycles the *colour values* themselves through
 * the palette using linear interpolation.
 *
 * Dark mode  → very dark navy/indigo/purple.
 * Light mode → soft lavender/sky/violet pastels.
 */
@Composable
private fun rememberFlowingBrandBrush(isDarkTheme: Boolean): Brush {
    val palette = if (isDarkTheme) listOf(
        Color(0xFF08061C),   // near-black purple (base)
        Color(0xFF130D42),   // muted dark purple
        Color(0xFF07101E),   // deep navy
        Color(0xFF1A0E52),   // muted indigo
        Color(0xFF0C051E),   // dark violet (return to base)
    ) else listOf(
        Color(0xFFE8EEFF),   // indigo 100
        Color(0xFFD9E2FF),   // indigo 200
        Color(0xFFF3F0FF),   // violet 50
        Color(0xFFD9EEFF),   // blue 100
        Color(0xFFE2EEFF),   // light blue 100
    )
    val n = palette.size

    val transition = rememberInfiniteTransition(label = "BrandGradientFlow")
    // offset travels 0 → n over 7 s, then wraps — driving the colour cycle
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = n.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GradientOffset"
    )

    // Build 5 animated stops by reading palette positions rotated by `offset`
    val animatedColors = (0 until n).map { i ->
        val pos = (offset + i) % n
        val lo = pos.toInt() % n
        val hi = (lo + 1) % n
        lerp(palette[lo], palette[hi], pos - pos.toInt())
    }

    // Offset.Zero → Offset.Infinite: Compose expands to the full component size,
    // so the gradient always fills corner-to-corner regardless of screen dimensions.
    return Brush.linearGradient(
        colors = animatedColors,
        start = Offset.Zero,
        end = Offset.Infinite
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentLibraryScreen(
    navController: NavController,
    db: AppDatabase,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: () -> Unit = {}
) {
    val context = LocalContext.current
    val docViewModel: DocumentViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = DocumentViewModelFactory(db.documentDao(), db.strokeDao(), db)
    )
    val documents by docViewModel.documents.collectAsState()
    val scope = rememberCoroutineScope()
    var showFabMenu by remember { mutableStateOf(false) }

    // Migrate stale display names saved before the ContentResolver fix
    LaunchedEffect(Unit) {
        docViewModel.fixStaleNames(context)
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                val name = context.contentResolver.query(
                    uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: uri.lastPathSegment ?: "Untitled"
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val copiedUri = PdfManager.copyPdfToAppDir(context, uri)
                    if (copiedUri != null) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            docViewModel.recordOpened(copiedUri.toString(), name)
                            val encodedUri = URLEncoder.encode(copiedUri.toString(), StandardCharsets.UTF_8.toString())
                            navController.navigate("editor/$encodedUri")
                        }
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "無法複製 PDF，請重試", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    )

    // Vibrant brand gradient used only for the FAB / logo (vivid purple)
    val brandGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        listOf(androidx.compose.ui.graphics.Color(0xFF6366F1), androidx.compose.ui.graphics.Color(0xFFA855F7))
    )
    // Full-screen flowing background gradient (dark or light palette)
    val flowingBgBrush = rememberFlowingBrandBrush(isDarkTheme)
    // Vivid flowing gradient for accent elements (FAB, logo)
    val flowingBrandBrush = rememberFlowingBrandBrush(isDarkTheme = false)
        .let { brandGradient }   // always vivid for accents

    var selectedNavIndex by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(flowingBgBrush)
            .statusBarsPadding()
    ) {

    Row(modifier = Modifier.fillMaxSize()) {
        // Navigation Rail — transparent so the full-screen gradient shows through
        androidx.compose.material3.NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Spacer(Modifier.height(8.dp))
            // Brand logo — vivid flowing gradient circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(brandGradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("I", style = MaterialTheme.typography.titleMedium.copy(
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                ))
            }
            Spacer(Modifier.height(16.dp))
            // Nav items
            NavigationRailItem(
                selected = selectedNavIndex == 0,
                onClick = { selectedNavIndex = 0 },
                icon = { Icon(Icons.Default.Home, contentDescription = "首頁") },
                label = { Text("首頁") }
            )
            NavigationRailItem(
                selected = selectedNavIndex == 1,
                onClick = { selectedNavIndex = 1 },
                icon = { Icon(Icons.Default.Folder, contentDescription = "資料夾") },
                label = { Text("資料夾") }
            )
            NavigationRailItem(
                selected = selectedNavIndex == 2,
                onClick = { selectedNavIndex = 2 },
                icon = { Icon(Icons.Default.Bookmark, contentDescription = "收藏") },
                label = { Text("收藏") }
            )
            Spacer(Modifier.weight(1f))
            // Dark / Light mode toggle
            IconButton(onClick = onToggleDarkTheme) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = "切換主題"
                )
            }
            // Settings
            IconButton(onClick = { /* TODO: Settings */ }) {
                Icon(Icons.Default.Settings, contentDescription = "設定")
            }
            Spacer(Modifier.height(8.dp))
        }

        // Main content with TopAppBar + search
        var searchQuery by remember { mutableStateOf("") }
        val filteredDocs = documents.filter { it.displayName.contains(searchQuery, ignoreCase = true) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                androidx.compose.material3.TopAppBar(
                    title = {
                        androidx.compose.material3.OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜尋筆記...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            shape = CircleShape
                        )
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                Box {
                    val fabInteractionSource = remember { MutableInteractionSource() }
                    val isFabPressed by fabInteractionSource.collectIsPressedAsState()
                    val fabScale by animateFloatAsState(
                        targetValue = if (isFabPressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "FabScale"
                    )
                    Box(
                        modifier = Modifier
                            .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                            .background(flowingBrandBrush, shape = RoundedCornerShape(16.dp))
                            .clickable(interactionSource = fabInteractionSource, indication = null) { showFabMenu = true }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("新增筆記", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("開啟 PDF") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                            onClick = {
                                showFabMenu = false
                                pdfLauncher.launch(arrayOf("application/pdf"))
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("空白筆記") },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            onClick = {
                                showFabMenu = false
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val newUri = PdfManager.createBlankPdf(context)
                                    if (newUri != null) {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            docViewModel.recordOpened(newUri.toString(), "未命名筆記")
                                            val encodedUri = URLEncoder.encode(newUri.toString(), StandardCharsets.UTF_8.toString())
                                            navController.navigate("editor/$encodedUri")
                                        }
                                    } else {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            android.widget.Toast.makeText(context, "無法建立空白筆記", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                if (filteredDocs.isEmpty()) {
                    // Empty state
                    val emptyStateFloat by rememberInfiniteTransition(label = "EmptyIconFloat")
                        .animateFloat(
                            initialValue = 0f,
                            targetValue = -14f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 2000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "EmptyFloatY"
                        )
                    val emptyStateAlpha by rememberInfiniteTransition(label = "EmptyIconPulse")
                        .animateFloat(
                            initialValue = 0.3f,
                            targetValue = 0.6f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 2000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "EmptyAlpha"
                        )
                    var emptyVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(80); emptyVisible = true }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = emptyVisible,
                        enter = fadeIn(tween(500)) + scaleIn(tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialScale = 0.85f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileUpload,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .graphicsLayer { translationY = emptyStateFloat },
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = emptyStateAlpha)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (searchQuery.isEmpty()) "靈感畫布已準備就緒" else "找不到符合的筆記",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (searchQuery.isEmpty()) "點擊右下角按鈕開啟第一份 PDF 筆記" else "試試其他搜尋關鍵字",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 180.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredDocs.size) { index ->
                            val doc = filteredDocs[index]
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                delay(index * 50L)
                                visible = true
                            }
                            androidx.compose.animation.AnimatedVisibility(
                                visible = visible,
                                enter = slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec = tween(300)
                                ) + fadeIn(animationSpec = tween(300))
                            ) {
                                DocumentCard(
                                    document = doc,
                                    onClick = {
                                        val encodedUri = URLEncoder.encode(doc.uri, StandardCharsets.UTF_8.toString())
                                        navController.navigate("editor/$encodedUri")
                                    },
                                    onDelete = { docViewModel.delete(doc.uri) },
                                    onRename = { newName -> docViewModel.rename(doc.uri, newName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }   // close inner Row
    }   // close outer Box (background + gradient overlay)
}

@Composable
private fun DocumentCard(
    document: com.vic.inkflow.data.DocumentEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit = {}
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember(document.displayName) { mutableStateOf(document.displayName) }

    if (showRenameDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重新命名") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    label = { Text("筆記名稱") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onRename(renameInput)
                        showRenameDialog = false
                    },
                    enabled = renameInput.isNotBlank()
                ) { Text("確認") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    val brandGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        listOf(
            androidx.compose.ui.graphics.Color(0xFF6366F1).copy(alpha = 0.10f),
            androidx.compose.ui.graphics.Color(0xFFA855F7).copy(alpha = 0.10f)
        )
    )
    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "CardScale"
    )
    var showMenu by remember { mutableStateOf(false) }
    val dateStr = remember(document.lastOpenedAt) {
        java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
            .format(java.util.Date(document.lastOpenedAt))
    }

    // Load first-page thumbnail with strokes overlay
    val context = LocalContext.current
    var coverBitmap by remember(document.uri, document.lastOpenedAt) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(document.uri, document.lastOpenedAt) {
        val cacheKey = "${document.uri}_${document.lastOpenedAt}"
        val cached = com.vic.inkflow.util.ThumbnailCacheManager.get(cacheKey)
        if (cached != null) {
            coverBitmap = cached
            return@LaunchedEffect
        }
        val bmp = withContext(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(document.uri)
                val pfd = PdfManager.openPdfFileDescriptor(context, uri) ?: return@withContext null
                val renderer = android.graphics.pdf.PdfRenderer(pfd)
                val page = renderer.openPage(0)
                val pdfPageW = page.width.toFloat()
                val pdfPageH = page.height.toFloat()
                val bmpW = (pdfPageW * 0.5f).toInt().coerceAtLeast(1)
                val bmpH = (pdfPageH * 0.5f).toInt().coerceAtLeast(1)
                val bitmap = android.graphics.Bitmap.createBitmap(bmpW, bmpH, android.graphics.Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                try {
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                } finally {
                    page.close()
                }
                renderer.close()
                pfd.close()

                // Overlay page 0: images (below strokes) → strokes → text (top)
                val db = AppDatabase.getDatabase(context)
                val strokes = db.strokeDao().getStrokesForPage(document.uri, 0).first()
                val imageAnns = db.imageAnnotationDao().getForPage(document.uri, 0).first()
                val textAnns  = db.textAnnotationDao().getForPage(document.uri, 0).first()
                val canvas = android.graphics.Canvas(bitmap)
                // Use the actual PDF page dimensions as model space (matches EditorViewModel.modelWidth/Height)
                val sx = bmpW.toFloat() / pdfPageW
                val sy = bmpH.toFloat() / pdfPageH

                // --- Image annotations (below strokes) ---
                imageAnns.forEach { ann ->
                    try {
                        val imgBmp = context.contentResolver.openInputStream(android.net.Uri.parse(ann.uri))?.use { stream ->
                            android.graphics.BitmapFactory.decodeStream(stream)
                        }
                        if (imgBmp != null) {
                            val dst = android.graphics.RectF(
                                ann.modelX * sx, ann.modelY * sy,
                                (ann.modelX + ann.modelWidth) * sx, (ann.modelY + ann.modelHeight) * sy
                            )
                            canvas.drawBitmap(imgBmp, null, dst, null)
                        }
                    } catch (_: Exception) { }
                }

                // --- Strokes (above images) ---
                if (strokes.isNotEmpty()) {
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                    }
                    strokes.forEach { swp ->
                        val stroke = swp.stroke
                        paint.color = stroke.color
                        paint.strokeWidth = stroke.strokeWidth * sx
                        paint.alpha = if (stroke.isHighlighter) (255 * 0.4f).toInt() else 255
                        if (stroke.isHighlighter) {
                            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY)
                            paint.strokeWidth = stroke.strokeWidth * sx * 3f
                        } else {
                            paint.xfermode = null
                        }
                        when (stroke.shapeType) {
                            "RECT" -> canvas.drawRect(
                                stroke.boundsLeft * sx, stroke.boundsTop * sy,
                                stroke.boundsRight * sx, stroke.boundsBottom * sy, paint
                            )
                            "CIRCLE" -> canvas.drawOval(
                                android.graphics.RectF(
                                    stroke.boundsLeft * sx, stroke.boundsTop * sy,
                                    stroke.boundsRight * sx, stroke.boundsBottom * sy
                                ), paint
                            )
                            "LINE" -> if (swp.points.size >= 2) {
                                val p0 = swp.points.first(); val p1 = swp.points.last()
                                canvas.drawLine(p0.x * sx, p0.y * sy, p1.x * sx, p1.y * sy, paint)
                            }
                            "ARROW" -> if (swp.points.size >= 2) {
                                val p0 = swp.points.first(); val p1 = swp.points.last()
                                canvas.drawLine(p0.x * sx, p0.y * sy, p1.x * sx, p1.y * sy, paint)
                                val headSize = paint.strokeWidth * 5f + 10f
                                val angle = Math.atan2((p1.y - p0.y).toDouble(), (p1.x - p0.x).toDouble())
                                val la = angle + Math.PI * 0.75; val ra = angle - Math.PI * 0.75
                                canvas.drawLine(p1.x * sx, p1.y * sy,
                                    p1.x * sx + (headSize * Math.cos(la)).toFloat(),
                                    p1.y * sy + (headSize * Math.sin(la)).toFloat(), paint)
                                canvas.drawLine(p1.x * sx, p1.y * sy,
                                    p1.x * sx + (headSize * Math.cos(ra)).toFloat(),
                                    p1.y * sy + (headSize * Math.sin(ra)).toFloat(), paint)
                            }
                            null -> {
                                val pts = swp.points
                                if (pts.size >= 2) {
                                    val path = android.graphics.Path()
                                    path.moveTo(pts.first().x * sx, pts.first().y * sy)
                                    for (i in 1 until pts.size) {
                                        val p1 = pts[i - 1]; val p2 = pts[i]
                                        path.quadTo(
                                            p1.x * sx, p1.y * sy,
                                            (p1.x + p2.x) / 2f * sx, (p1.y + p2.y) / 2f * sy
                                        )
                                    }
                                    pts.lastOrNull()?.let { path.lineTo(it.x * sx, it.y * sy) }
                                    canvas.drawPath(path, paint)
                                }
                            }
                        }
                        paint.xfermode = null
                    }
                }

                // --- Text annotations (on top of everything) ---
                if (textAnns.isNotEmpty()) {
                    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    textAnns.forEach { ann ->
                        textPaint.textSize = ann.fontSize * sy
                        textPaint.color = ann.colorArgb
                        canvas.drawText(ann.text, ann.modelX * sx, ann.modelY * sy, textPaint)
                    }
                }

                if (bitmap != null) {
                    com.vic.inkflow.util.ThumbnailCacheManager.put(cacheKey, bitmap)
                }
                bitmap
            } catch (_: Exception) { null }
        }
        if (bmp != null) coverBitmap = bmp
    }

    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f).graphicsLayer { scaleX = cardScale; scaleY = cardScale },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
        interactionSource = cardInteractionSource
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Cover 60%
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(brandGradient),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = coverBitmap,
                    animationSpec = tween(400),
                    label = "CardCoverFade"
                ) { bitmap ->
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Shimmer skeleton while loading
                        val shimmerOffsetX by rememberInfiniteTransition(label = "CardShimmer")
                            .animateFloat(
                                initialValue = -1f,
                                targetValue = 2f,
                                animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
                                label = "ShimmerX"
                            )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.25f),
                                            Color.Transparent
                                        ),
                                        start = Offset(shimmerOffsetX * 400f, 0f),
                                        end = Offset((shimmerOffsetX + 0.5f) * 400f, 200f)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = androidx.compose.ui.graphics.Color(0xFF6366F1).copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            // Info 40%
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = document.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp))
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("重新命名") },
                                leadingIcon = { Icon(Icons.Default.Create, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    renameInput = document.displayName
                                    showRenameDialog = true
                                }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("刪除") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}


// --- 3. Editor Screen ---
enum class SidebarMode { COLLAPSED, NORMAL, FULLSCREEN }

@Composable
fun TabletEditorScreen(navController: NavController, uri: Uri, db: AppDatabase) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsRepository = remember(db) {
        EditorSettingsRepository(
            db = db,
            documentPreferenceDao = db.documentPreferenceDao()
        )
    }
    val viewModel: EditorViewModel = viewModel(
        factory = EditorViewModelFactory(db, uri.toString(), settingsRepository)
    )
    val pdfViewModel: PdfViewModel = viewModel()
    val strokes by viewModel.currentStrokes.collectAsState()
    val scope = rememberCoroutineScope()

    val docViewModel: DocumentViewModel = viewModel(
        factory = DocumentViewModelFactory(db.documentDao(), db.strokeDao(), db)
    )
    var sidebarMode by rememberSaveable { mutableStateOf(SidebarMode.NORMAL) }
    val activeTool by viewModel.selectedTool.collectAsState()
    var currentPageIndex by rememberSaveable { mutableIntStateOf(0) }
    var showStrokeWidthSlider by rememberSaveable { mutableStateOf(false) }
    // Guards against overwriting the DB value before we've read it on first open
    var initialPageRestored by rememberSaveable { mutableStateOf(false) }
    val sidebarListState = rememberLazyListState()

    // Restore the last-viewed page from DB on first open; rememberSaveable keeps it
    // true across config changes so we don't reset the page on rotation.
    androidx.compose.runtime.LaunchedEffect(uri) {
        if (!initialPageRestored) {
            val stored = docViewModel.getLastPageIndex(uri.toString())
            if (stored > 0) {
                currentPageIndex = stored
                viewModel.setActivePage(stored)
                sidebarListState.scrollToCenter(stored)
            }
            initialPageRestored = true
        }
    }

    // Persist current page and sync sidebar scroll whenever the user navigates
    androidx.compose.runtime.LaunchedEffect(currentPageIndex) {
        if (initialPageRestored) {
            docViewModel.updateLastPage(uri.toString(), currentPageIndex)
            sidebarListState.animateScrollToCenter(currentPageIndex)
        }
    }

    // Resolve the human-readable file name from the documents DB record.
    // ContentResolver does not work with file:// URIs (copied PDFs use UUID filenames),
    // so we look up the displayName that was saved at import time.
    val documents by docViewModel.documents.collectAsState()
    val documentTitle = remember(uri, documents) {
        documents.find { it.uri == uri.toString() }?.displayName
            ?: uri.lastPathSegment ?: "Untitled"
    }

    // Open PDF once; PdfViewModel survives config changes
    androidx.compose.runtime.LaunchedEffect(uri) {
        pdfViewModel.openPdf(uri)
    }
    val pageCount by pdfViewModel.pageCount.collectAsState()

    // When the PDF first loads, initialize the EditorViewModel's model space to match the first page.
    val firstPageSize by pdfViewModel.firstPageSize.collectAsState()
    androidx.compose.runtime.LaunchedEffect(firstPageSize) {
        firstPageSize?.let { (w, h) -> viewModel.initializePaperSize(w, h) }
    }

    // Paper style dialog state
    var showPaperSettingsDialog by remember { mutableStateOf(false) }
    val paperStyle by viewModel.paperStyle.collectAsState()
    if (showPaperSettingsDialog) {
        PaperStyleDialog(
            currentStyle = paperStyle,
            hasExistingStrokes = strokes.isNotEmpty(),
            onDismiss = { showPaperSettingsDialog = false },
            onConfirm = { newStyle ->
                viewModel.setPaperStyle(newStyle)
                showPaperSettingsDialog = false
            }
        )
    }

    // Derive current page aspect ratio from the ViewModel's paper style
    val pageAspectRatio = paperStyle.aspectRatio

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        TabletEditorTopBar(
            documentTitle = documentTitle,
            onBack = { navController.popBackStack() },
            viewModel = viewModel,
            showStrokeWidthSlider = showStrokeWidthSlider,
            onToggleStrokeWidthSlider = { showStrokeWidthSlider = !showStrokeWidthSlider },
            onHideStrokeWidthSlider = { showStrokeWidthSlider = false },
            onExport = {
                scope.launch {
                    val allStrokes = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        db.strokeDao().getAllStrokesForDocument(uri.toString())
                    }
                    val allTextAnnotations = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        db.textAnnotationDao().getAllForDocument(uri.toString())
                    }
                    val allImageAnnotations = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        db.imageAnnotationDao().getAllForDocument(uri.toString())
                    }
                    com.vic.inkflow.util.PdfExporter.export(
                        originalPdfUri   = uri,
                        strokes          = allStrokes,
                        textAnnotations  = allTextAnnotations,
                        imageAnnotations = allImageAnnotations,
                        context          = context,
                        fileName         = "InkFlow_${System.currentTimeMillis()}.pdf",
                        modelW           = viewModel.modelWidth,
                        modelH           = viewModel.modelHeight
                    )
                }
            },
            onPaperSettings = { showPaperSettingsDialog = true }
        )

        AnimatedVisibility(
            visible = showStrokeWidthSlider && (activeTool == Tool.PEN || activeTool == Tool.HIGHLIGHTER),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            StrokeWidthSlider(viewModel = viewModel)
        }

        // Auto-navigate to the newly inserted page
        val lastInsertedPage by pdfViewModel.lastInsertedPageIndex.collectAsState()
        androidx.compose.runtime.LaunchedEffect(lastInsertedPage) {
            val idx = lastInsertedPage ?: return@LaunchedEffect
            if (initialPageRestored) {
                currentPageIndex = idx
                viewModel.setActivePage(idx)
                sidebarListState.animateScrollToCenter(idx)
                pdfViewModel.consumeInsertedPageEvent()
            }
        }

        // Adjust currentPageIndex after a page is deleted
        val lastDeletedPage by pdfViewModel.lastDeletedPageIndex.collectAsState()
        androidx.compose.runtime.LaunchedEffect(lastDeletedPage) {
            val idx = lastDeletedPage ?: return@LaunchedEffect
            val newPage = when {
                currentPageIndex > idx -> (currentPageIndex - 1).coerceAtLeast(0)
                currentPageIndex == idx -> (idx - 1).coerceAtLeast(0)
                else -> currentPageIndex
            }
            currentPageIndex = newPage
            viewModel.setActivePage(newPage)
            sidebarListState.animateScrollToCenter(newPage)
            pdfViewModel.consumeDeletedPageEvent()
        }

        Row(Modifier.fillMaxSize()) {
            val isFullscreen = sidebarMode == SidebarMode.FULLSCREEN
            Sidebar(
                sidebarMode = sidebarMode,
                onModeChange = { sidebarMode = it },
                pdfViewModel = pdfViewModel,
                pageCount = pageCount,
                currentPageIndex = currentPageIndex,
                db = db,
                documentUri = uri.toString(),
                onPageSelected = { index ->
                    currentPageIndex = index
                    viewModel.setActivePage(index)
                },
                onAddPage = { afterIndex ->
                    scope.launch { pdfViewModel.insertBlankPage(context, afterIndex) }
                },
                onDeletePage = { index ->
                    // DB 清理與 PDF 刪除平行執行，縮短總等待時間
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        db.strokeDao().clearPage(uri.toString(), index)
                        db.strokeDao().shiftPageIndicesDown(uri.toString(), index)
                        db.imageAnnotationDao().deleteForPage(uri.toString(), index)
                        db.imageAnnotationDao().shiftPageIndicesDown(uri.toString(), index)
                        db.textAnnotationDao().deleteForPage(uri.toString(), index)
                        db.textAnnotationDao().shiftPageIndicesDown(uri.toString(), index)
                    }
                    pdfViewModel.deletePage(index)
                },
                listState = sidebarListState,
                modifier = if (isFullscreen) Modifier.fillMaxHeight().weight(1f) else Modifier.fillMaxHeight(),
                pageAspectRatio = pageAspectRatio
            )

            if (!isFullscreen) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Workspace(
                        pageIndex = currentPageIndex,
                        pdfViewModel = pdfViewModel,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                        pageAspectRatio = pageAspectRatio
                    )
                    SidebarFloatingControls(
                        sidebarMode = sidebarMode,
                        onModeChange = { sidebarMode = it },
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
                    )
                    
                    // Selected region bubble (Extract button) over Workspace
                    val lassoPolygon by viewModel.lassoPolygon.collectAsState()
                    var isExtracting by remember { mutableStateOf(false) }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = lassoPolygon.isNotEmpty() && !isExtracting,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "已選取區域",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                androidx.compose.material3.TextButton(
                                    enabled = !isExtracting,
                                    onClick = {
                                        if (isExtracting) return@TextButton
                                        isExtracting = true
                                        scope.launch {
                                            try {
                                                // Snapshot source page first; page index may auto-switch after insertion.
                                                val sourcePageIndex = currentPageIndex
                                                val sourceBitmap = kotlinx.coroutines.withTimeoutOrNull(1200) {
                                                    pdfViewModel.getPageBitmap(sourcePageIndex)
                                                        .filterNotNull()
                                                        .first()
                                                } ?: pdfViewModel.getPageBitmap(sourcePageIndex).value

                                                // Insert blank page right after source page.
                                                val newPageIndex = sourcePageIndex + 1
                                                pdfViewModel.insertBlankPage(context, sourcePageIndex)

                                                // Extract source region and place it onto the new page.
                                                viewModel.extractRegionToNewPage(
                                                    context = context,
                                                    sourcePageIndex = sourcePageIndex,
                                                    targetPageIndex = newPageIndex,
                                                    pdfPageBitmap = sourceBitmap
                                                )
                                            } finally {
                                                isExtracting = false
                                            }
                                        }
                                    }
                                ) {
                                    Text("提取到新頁面")
                                }
                                androidx.compose.material3.IconButton(
                                    onClick = { viewModel.clearSelection() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "取消選取",
                                        modifier = Modifier.size(16.dp)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Sidebar(
    sidebarMode: SidebarMode,
    onModeChange: (SidebarMode) -> Unit,
    pdfViewModel: PdfViewModel,
    pageCount: Int,
    currentPageIndex: Int,
    db: AppDatabase,
    documentUri: String,
    onPageSelected: (Int) -> Unit,
    onAddPage: (afterIndex: Int) -> Unit,
    onDeletePage: (Int) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
    pageAspectRatio: Float = 1f / 1.414f
) {
    // Index of the page pending delete confirmation; null = no dialog shown
    var deleteConfirmIndex by remember { mutableStateOf<Int?>(null) }
    val thumbnailVersion by pdfViewModel.thumbnailVersion.collectAsState()
    val isPageOperationInProgress by pdfViewModel.isPageOperationInProgress.collectAsState()
    if (deleteConfirmIndex != null) {
        val idx = deleteConfirmIndex!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteConfirmIndex = null },
            title = { Text("刪除頁面") },
            text = { Text("確定要刪除第 ${idx + 1} 頁？此操作無法復原。") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        deleteConfirmIndex = null
                        onDeletePage(idx)
                    },
                    enabled = !isPageOperationInProgress
                ) { Text("刪除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteConfirmIndex = null }) {
                    Text("取消")
                }
            }
        )
    }
    val animatedWidth by animateDpAsState(
        targetValue = if (sidebarMode == SidebarMode.COLLAPSED) 64.dp else 200.dp,
        animationSpec = tween(300),
        label = "SidebarWidthAnimation"
    )

    Surface(
        modifier = if (sidebarMode == SidebarMode.FULLSCREEN) modifier else modifier.width(animatedWidth),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 4.dp
    ) {
        if (sidebarMode == SidebarMode.FULLSCREEN) {
            // Fullscreen: 4-column page grid with back button
            val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
            LaunchedEffect(Unit) {
                // Scroll so the current page is visible when the grid opens
                val row = currentPageIndex / 4
                gridState.scrollToItem(index = (row * 4).coerceAtLeast(0))
            }
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onModeChange(SidebarMode.NORMAL) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit grid view")
                    }
                    Text(
                        text = "所有頁面",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(pageCount) { index ->
                        val thumbFlow = androidx.compose.runtime.remember(index, thumbnailVersion) {
                            pdfViewModel.getPageThumbnail(index)
                        }
                        val thumb by thumbFlow.collectAsState()
                        val strokesFlow = androidx.compose.runtime.remember(index) {
                            db.strokeDao().getStrokesForPage(documentUri, index)
                        }
                        val strokes by strokesFlow.collectAsState(initial = emptyList())
                        val imagesFlow = androidx.compose.runtime.remember(index) {
                            db.imageAnnotationDao().getForPage(documentUri, index)
                        }
                        val images by imagesFlow.collectAsState(initial = emptyList())
                        val textsFlow = androidx.compose.runtime.remember(index) {
                            db.textAnnotationDao().getForPage(documentUri, index)
                        }
                        val texts by textsFlow.collectAsState(initial = emptyList())
                        Box(modifier = Modifier.combinedClickable(
                            onClick = {
                                onPageSelected(index)
                                onModeChange(SidebarMode.NORMAL)
                            },
                            onLongClick = { if (pageCount > 1 && !isPageOperationInProgress) deleteConfirmIndex = index }
                        )) {
                            PageThumbnail(
                                pageIndex = index,
                                bitmap = thumb,
                                strokes = strokes,
                                imageAnnotations = images,
                                textAnnotations = texts,
                                isSelected = index == currentPageIndex,
                                boxModifier = Modifier.fillMaxWidth().aspectRatio(pageAspectRatio)
                            )
                        }
                    }
                }
            }
        } else {
            // Normal / Collapsed — thumbnail list + pinned add-page footer
            Column(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(pageCount, key = { it }) { index ->
                        val thumbFlow = androidx.compose.runtime.remember(index, thumbnailVersion) {
                            pdfViewModel.getPageThumbnail(index)
                        }
                        val thumb by thumbFlow.collectAsState()
                        val strokesFlow = androidx.compose.runtime.remember(index) {
                            db.strokeDao().getStrokesForPage(documentUri, index)
                        }
                        val strokes by strokesFlow.collectAsState(initial = emptyList())
                        val imagesFlow = androidx.compose.runtime.remember(index) {
                            db.imageAnnotationDao().getForPage(documentUri, index)
                        }
                        val images by imagesFlow.collectAsState(initial = emptyList())
                        val textsFlow = androidx.compose.runtime.remember(index) {
                            db.textAnnotationDao().getForPage(documentUri, index)
                        }
                        val texts by textsFlow.collectAsState(initial = emptyList())
                        Box(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .combinedClickable(
                                    onClick = { onPageSelected(index) },
                                    onLongClick = { if (pageCount > 1 && !isPageOperationInProgress) deleteConfirmIndex = index }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (sidebarMode == SidebarMode.NORMAL) {
                                PageThumbnail(
                                    pageIndex = index,
                                    bitmap = thumb,
                                    strokes = strokes,
                                    imageAnnotations = images,
                                    textAnnotations = texts,
                                    isSelected = index == currentPageIndex,
                                    boxModifier = Modifier.width(100.dp).aspectRatio(pageAspectRatio)
                                )
                            } else {
                                PageIcon(
                                    pageIndex = index,
                                    isSelected = index == currentPageIndex
                                )
                            }
                        }
                    }
                }
                // 固定在底部的新增頁面按鈕
                if (sidebarMode == SidebarMode.NORMAL) {
                    androidx.compose.material3.HorizontalDivider()
                    // 頁面操作進行中時顯示細長進度條，給予使用者視覺回饋
                    if (isPageOperationInProgress) {
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp)
                        )
                    }
                    androidx.compose.material3.TextButton(
                        onClick = { onAddPage(currentPageIndex) },
                        enabled = !isPageOperationInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("新增頁面", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PageThumbnail(
    pageIndex: Int,
    bitmap: android.graphics.Bitmap?,
    strokes: List<StrokeWithPoints>,
    imageAnnotations: List<ImageAnnotationEntity>,
    textAnnotations: List<TextAnnotationEntity>,
    isSelected: Boolean,
    boxModifier: Modifier = Modifier.width(100.dp).aspectRatio(1f / 1.414f)
) {
    val context = LocalContext.current
    // Cache decoded bitmaps keyed by URI string
    val loadedImages = remember { mutableStateMapOf<String, android.graphics.Bitmap?>() }
    LaunchedEffect(imageAnnotations) {
        imageAnnotations.forEach { ann ->
            if (ann.uri !in loadedImages) {
                loadedImages[ann.uri] = null
                withContext(Dispatchers.IO) {
                    try {
                        val bmp = context.contentResolver.openInputStream(Uri.parse(ann.uri))?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                        loadedImages[ann.uri] = bmp
                    } catch (_: Exception) { }
                }
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val animatedBorderWidth by animateDpAsState(
            targetValue = if (isSelected) 2.dp else 1.dp,
            animationSpec = tween(200),
            label = "ThumbBorderWidth"
        )
        val animatedBorderColor by animateColorAsState(
            targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.outlineVariant,
            animationSpec = tween(250),
            label = "ThumbBorderColor"
        )
        val thumbScale by animateFloatAsState(
            targetValue = if (isSelected) 1.04f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "ThumbScale"
        )
        Box(
            modifier = boxModifier
                .graphicsLayer { scaleX = thumbScale; scaleY = thumbScale }
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small)
                .border(
                    width = animatedBorderWidth,
                    color = animatedBorderColor,
                    shape = MaterialTheme.shapes.small
                )
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.Center),
                    strokeWidth = 2.dp
                )
            }
            // Unified ink + image + text overlay
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val modelW = 595f
                val modelH = 842f
                val sx = size.width / modelW
                val sy = size.height / modelH

                // --- Strokes (freehand + shapes) ---
                strokes.forEach { swp ->
                    val stroke = swp.stroke
                    val strokeColor = Color(stroke.color)
                    val alpha = if (stroke.isHighlighter) 0.4f else 1f
                    val widthPx = stroke.strokeWidth * (if (stroke.isHighlighter) 3f else 1f) * sx
                    val paintStyle = Stroke(width = widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)

                    if (stroke.shapeType != null) {
                        val r = Rect(
                            stroke.boundsLeft * sx, stroke.boundsTop * sy,
                            stroke.boundsRight * sx, stroke.boundsBottom * sy
                        )
                        when (stroke.shapeType) {
                            "RECT" -> drawRect(
                                color = strokeColor.copy(alpha = alpha),
                                topLeft = Offset(r.left, r.top),
                                size = Size(r.width, r.height),
                                style = paintStyle
                            )
                            "CIRCLE" -> drawOval(
                                color = strokeColor.copy(alpha = alpha),
                                topLeft = Offset(r.left, r.top),
                                size = Size(r.width, r.height),
                                style = paintStyle
                            )
                            "LINE" -> if (swp.points.size >= 2) {
                                drawLine(
                                    color = strokeColor.copy(alpha = alpha),
                                    start = Offset(swp.points.first().x * sx, swp.points.first().y * sy),
                                    end = Offset(swp.points.last().x * sx, swp.points.last().y * sy),
                                    strokeWidth = widthPx,
                                    cap = StrokeCap.Round
                                )
                            }
                            "ARROW" -> if (swp.points.size >= 2) {
                                val p0 = Offset(swp.points.first().x * sx, swp.points.first().y * sy)
                                val p1 = Offset(swp.points.last().x * sx, swp.points.last().y * sy)
                                drawLine(
                                    color = strokeColor.copy(alpha = alpha),
                                    start = p0, end = p1,
                                    strokeWidth = widthPx, cap = StrokeCap.Round
                                )
                                thumbnailDrawArrowHead(
                                    drawScope = this,
                                    color = strokeColor.copy(alpha = alpha),
                                    start = p0, end = p1, sw = widthPx
                                )
                            }
                        }
                    } else {
                        val pts = swp.points
                        if (pts.size >= 2) {
                            val path = androidx.compose.ui.graphics.Path()
                            path.moveTo(pts.first().x * sx, pts.first().y * sy)
                            for (i in 1 until pts.size) {
                                val p1 = pts[i - 1]; val p2 = pts[i]
                                path.quadraticTo(
                                    p1.x * sx, p1.y * sy,
                                    (p1.x + p2.x) / 2f * sx, (p1.y + p2.y) / 2f * sy
                                )
                            }
                            pts.lastOrNull()?.let { path.lineTo(it.x * sx, it.y * sy) }
                            drawPath(path, strokeColor.copy(alpha = alpha), style = paintStyle)
                        }
                    }
                }

                // --- Image annotations ---
                imageAnnotations.forEach { ann ->
                    val bmp = loadedImages[ann.uri]
                    if (bmp != null) {
                        drawImage(
                            image = bmp.asImageBitmap(),
                            dstOffset = IntOffset((ann.modelX * sx).toInt(), (ann.modelY * sy).toInt()),
                            dstSize = IntSize(
                                (ann.modelWidth * sx).toInt().coerceAtLeast(1),
                                (ann.modelHeight * sy).toInt().coerceAtLeast(1)
                            )
                        )
                    }
                }

                // --- Text annotations ---
                if (textAnnotations.isNotEmpty()) {
                    drawIntoCanvas { composeCanvas ->
                        textAnnotations.forEach { ann ->
                            val paint = android.graphics.Paint().apply {
                                textSize    = ann.fontSize * sy
                                color       = ann.colorArgb
                                isAntiAlias = true
                                typeface    = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            composeCanvas.nativeCanvas.drawText(
                                ann.text, ann.modelX * sx, ann.modelY * sy, paint
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Page ${pageIndex + 1}", style = MaterialTheme.typography.labelSmall)
    }
}

/** Draws an arrowhead at [end] pointing away from [start] in a thumbnail DrawScope. */
private fun thumbnailDrawArrowHead(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    color: Color,
    start: Offset,
    end: Offset,
    sw: Float
) {
    val headSize   = sw * 5f + 6f
    val angle      = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val leftAngle  = angle + Math.PI * 0.75
    val rightAngle = angle - Math.PI * 0.75
    val lp = Offset(end.x + (headSize * cos(leftAngle)).toFloat(), end.y + (headSize * sin(leftAngle)).toFloat())
    val rp = Offset(end.x + (headSize * cos(rightAngle)).toFloat(), end.y + (headSize * sin(rightAngle)).toFloat())
    with(drawScope) {
        drawLine(color, end, lp, strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, end, rp, strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
private fun PageIcon(pageIndex: Int, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center
    ) {
        Text("${pageIndex + 1}", style = MaterialTheme.typography.labelSmall)
    }
}

/** 懸浮在畫布左緣中央的控制按鈕：收合側欄 + 全螢幕頁面格（閒置 3 秒後自動淡出） */
@Composable
private fun SidebarFloatingControls(
    sidebarMode: SidebarMode,
    onModeChange: (SidebarMode) -> Unit,
    modifier: Modifier = Modifier
) {
    // Every time activationCount changes, reset the 3-second idle timer
    var activationCount by remember { mutableStateOf(0) }
    var isActive by remember { mutableStateOf(true) }
    val controlsAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.3f,
        animationSpec = tween(500),
        label = "ControlsAlpha"
    )
    // Animate container colour on dark/light switch
    val btnContainerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        animationSpec = tween(400),
        label = "FloatingBtnColor"
    )
    LaunchedEffect(activationCount) {
        isActive = true
        delay(3000)
        isActive = false
    }

    Column(
        modifier = modifier.graphicsLayer { alpha = controlsAlpha },
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 收合 / 展開
        androidx.compose.material3.SmallFloatingActionButton(
            onClick = {
                activationCount++
                onModeChange(
                    if (sidebarMode == SidebarMode.NORMAL) SidebarMode.COLLAPSED else SidebarMode.NORMAL
                )
            },
            containerColor = btnContainerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(2.dp, 2.dp)
        ) {
            Icon(
                imageVector = if (sidebarMode == SidebarMode.NORMAL) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                contentDescription = if (sidebarMode == SidebarMode.NORMAL) "收合側欄" else "展開側欄",
                modifier = Modifier.size(18.dp)
            )
        }
        // 頁面格（只在 NORMAL 顯示）
        androidx.compose.animation.AnimatedVisibility(
            visible = sidebarMode == SidebarMode.NORMAL,
            enter = fadeIn(tween(200)) + scaleIn(tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialScale = 0.7f),
            exit = fadeOut(tween(150)) + androidx.compose.animation.scaleOut(tween(150), targetScale = 0.7f)
        ) {
            androidx.compose.material3.SmallFloatingActionButton(
                onClick = {
                    activationCount++
                    onModeChange(SidebarMode.FULLSCREEN)
                },
                containerColor = btnContainerColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(2.dp, 2.dp)
            ) {
                Icon(
                    Icons.Default.Apps,
                    contentDescription = "全螢幕頁面格",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


@Composable
private fun Workspace(
    pageIndex: Int,
    pdfViewModel: PdfViewModel,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier,
    pageAspectRatio: Float = 1f / 1.414f
) {
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }

    // Track container dimensions (pixels) to compute the fit-to-page minimum scale
    var containerWidth by remember { mutableIntStateOf(0) }
    var containerHeight by remember { mutableIntStateOf(0) }
    val minScale = remember(containerWidth, containerHeight, pageAspectRatio) {
        if (containerWidth <= 0 || containerHeight <= 0) return@remember 1f
        val cW = containerWidth.toFloat()
        val cH = containerHeight.toFloat()
        // Compute page dimensions (height-constrained for portrait, width-constrained for landscape)
        val pageH = cH * 0.9f
        val pageW = pageH * pageAspectRatio
        // Minimum scale = page occupies 85% of viewport (full page visible with comfortable margins)
        minOf(cW * 0.85f / pageW, cH * 0.85f / pageH)
    }

    // When container first appears (or resizes), clamp saved scale to the new minimum
    LaunchedEffect(minScale) {
        if (scale < minScale) scale = minScale
    }

    // pdfViewModel and LaunchedEffect(uri) are owned by TabletEditorScreen
    val pageCount by pdfViewModel.pageCount.collectAsState()
    val pageBitmapFlow = androidx.compose.runtime.remember(pageIndex, pageCount) {
        if (pageCount > 0) pdfViewModel.getPageBitmap(pageIndex)
        else kotlinx.coroutines.flow.MutableStateFlow(null)
    }
    val pageBitmap by pageBitmapFlow.collectAsState()

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .onSizeChanged { size ->
                containerWidth = size.width
                containerHeight = size.height
            }
            .pointerInput(minScale) {
                detectTransformGestures {
                    centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (scale * zoom).coerceIn(minScale, 5f)
                        if (newScale <= minScale) {
                            // At minimum zoom, snap page to centre so the full page is always visible
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            // Correct pivot-based zoom: graphicsLayer scales around the Surface's
                            // centre, which sits at (containerWidth/2, containerHeight/2) in Box
                            // coordinates. Adjusting centroid by that pivot keeps the pinch
                            // focal point locked under the fingers.
                            val r = newScale / oldScale
                            offsetX = offsetX * r + (centroid.x - containerWidth / 2f) * (1 - r) + pan.x
                            offsetY = offsetY * r + (centroid.y - containerHeight / 2f) * (1 - r) + pan.y
                        }
                        scale = newScale
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(0.9f)
                .aspectRatio(pageAspectRatio, matchHeightConstraintsFirst = true)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            shadowElevation = 8.dp,
            color = Color.White
        ) {
            // PDF static layer (bottom) — crossfade between page bitmaps
            Crossfade(
                targetState = pageBitmap,
                animationSpec = tween(200),
                label = "PageBitmapFade"
            ) { bitmap ->
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "PDF Page $pageIndex",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
            }
            // Background pattern layer (ruled / grid lines)
            val paperStyle by viewModel.paperStyle.collectAsState()
            if (paperStyle.background != PageBackground.BLANK) {
                val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val modelW = viewModel.modelWidth
                    val modelH = viewModel.modelHeight
                    val sx = size.width / modelW
                    val sy = size.height / modelH
                    val step = when (paperStyle.background) {
                        PageBackground.NARROW_RULED -> 18f  // ~6.35mm
                        PageBackground.WIDE_RULED   -> 42f  // ~15mm
                        else                        -> 28f  // ~1cm
                    }
                    val drawHLines = paperStyle.background == PageBackground.RULED ||
                        paperStyle.background == PageBackground.NARROW_RULED ||
                        paperStyle.background == PageBackground.WIDE_RULED ||
                        paperStyle.background == PageBackground.GRID
                    if (drawHLines) {
                        var y = step
                        while (y < modelH) {
                            drawLine(
                                color = lineColor,
                                start = androidx.compose.ui.geometry.Offset(0f, y * sy),
                                end = androidx.compose.ui.geometry.Offset(size.width, y * sy),
                                strokeWidth = 1f
                            )
                            y += step
                        }
                    }
                    if (paperStyle.background == PageBackground.GRID) {
                        var x = step
                        while (x < modelW) {
                            drawLine(
                                color = lineColor,
                                start = androidx.compose.ui.geometry.Offset(x * sx, 0f),
                                end = androidx.compose.ui.geometry.Offset(x * sx, size.height),
                                strokeWidth = 1f
                            )
                            x += step
                        }
                    }
                    if (paperStyle.background == PageBackground.DOT_GRID) {
                        var y = step
                        while (y < modelH) {
                            var x = step
                            while (x < modelW) {
                                drawCircle(
                                    color = lineColor,
                                    radius = 1.5f,
                                    center = androidx.compose.ui.geometry.Offset(x * sx, y * sy)
                                )
                                x += step
                            }
                            y += step
                        }
                    }
                }
            }
            // Ink active layer (top)
            InkCanvas(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel
            )
        }
    }
}


@Composable
fun TabletEditorTopBar(
    documentTitle: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel,
    showStrokeWidthSlider: Boolean = false,
    onToggleStrokeWidthSlider: () -> Unit = {},
    onHideStrokeWidthSlider: () -> Unit = {},
    onExport: () -> Unit = {},
    onPaperSettings: () -> Unit = {}
) {
    val activeTool by viewModel.selectedTool.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val recentColors by viewModel.recentColors.collectAsState()
    val toolColors = listOf(Color.Black, Color(0xFF_FFC700), Color(0xFF_F44336), Color(0xFF_4CAF50))
    val shownRecentColors = recentColors.filterNot { it in toolColors }.take(8)
    var showColorPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showColorPicker) {
        ColorPickerDialog(
            onColorSelected = { viewModel.onColorSelected(it) },
            onDismiss = { showColorPicker = false }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Zone
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Library")
                }
                Text(
                    text = documentTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 200.dp)
                )
            }

            // Center Zone
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val toolButtonSize = 40.dp
                val selectedShapeSubType by viewModel.selectedShapeSubType.collectAsState()

                // --- Drawing tools (PEN / HIGHLIGHTER / ERASER / LASSO) ---
                val drawingTools = listOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.LASSO)
                val drawingActiveIdx = drawingTools.indexOf(activeTool).let { if (it < 0) -1 else it }
                val drawingHighlightOffset by animateDpAsState(
                    targetValue = if (drawingActiveIdx >= 0) toolButtonSize * drawingActiveIdx else 0.dp,
                    animationSpec = tween(durationMillis = 200),
                    label = "DrawingToolHighlight"
                )
                Box {
                    if (drawingActiveIdx >= 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = drawingHighlightOffset)
                                .size(toolButtonSize)
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small)
                        )
                    }
                    Row {
                        EditorIconButton(
                            onClick = {
                                if (activeTool == Tool.PEN) onToggleStrokeWidthSlider()
                                else { viewModel.onToolSelected(Tool.PEN); onHideStrokeWidthSlider() }
                            },
                            isActive = activeTool == Tool.PEN,
                            icon = Icons.Default.Create,
                            contentDescription = "Pen Tool"
                        )
                        EditorIconButton(
                            onClick = {
                                if (activeTool == Tool.HIGHLIGHTER) onToggleStrokeWidthSlider()
                                else { viewModel.onToolSelected(Tool.HIGHLIGHTER); onHideStrokeWidthSlider() }
                            },
                            isActive = activeTool == Tool.HIGHLIGHTER,
                            icon = Icons.Rounded.Brush,
                            contentDescription = "Highlighter Tool"
                        )
                        EditorIconButton(
                            onClick = { viewModel.onToolSelected(Tool.ERASER); onHideStrokeWidthSlider() },
                            isActive = activeTool == Tool.ERASER,
                            icon = Icons.Rounded.Delete,
                            contentDescription = "Eraser Tool"
                        )
                        EditorIconButton(
                            onClick = { viewModel.onToolSelected(Tool.LASSO); onHideStrokeWidthSlider() },
                            isActive = activeTool == Tool.LASSO,
                            icon = Icons.Rounded.Gesture,
                            contentDescription = "Lasso Select Tool"
                        )
                    }
                }

                // Lasso sub-type pills (visible only when LASSO is active)
                AnimatedVisibility(
                    visible = activeTool == Tool.LASSO,
                    enter = fadeIn(tween(200)) + androidx.compose.animation.expandHorizontally(tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
                    exit = fadeOut(tween(180)) + androidx.compose.animation.shrinkHorizontally(tween(200))
                ) {
                    val selectedLassoSubType by viewModel.selectedLassoSubType.collectAsState()
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LassoSubType.entries.forEach { subType ->
                            val isSelected = selectedLassoSubType == subType
                            TextButton(
                                onClick = { viewModel.onLassoSubTypeSelected(subType) },
                                modifier = Modifier.height(32.dp),
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor   = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    text = when (subType) {
                                        LassoSubType.FREEFORM -> "隨機圖形"
                                        LassoSubType.RECT     -> "方形"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                VerticalDivider(Modifier.height(28.dp).padding(horizontal = 6.dp))

                // --- Annotation tools (SHAPE / TEXT / STAMP) ---
                val annotationTools = listOf(Tool.SHAPE, Tool.TEXT, Tool.IMAGE, Tool.STAMP)
                val annotActiveIdx = annotationTools.indexOf(activeTool).let { if (it < 0) -1 else it }
                val annotHighlightOffset by animateDpAsState(
                    targetValue = if (annotActiveIdx >= 0) toolButtonSize * annotActiveIdx else 0.dp,
                    animationSpec = tween(durationMillis = 200),
                    label = "AnnotToolHighlight"
                )
                Box {
                    if (annotActiveIdx >= 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = annotHighlightOffset)
                                .size(toolButtonSize)
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small)
                        )
                    }
                    Row {
                        EditorIconButton(
                            onClick = { viewModel.onToolSelected(Tool.SHAPE); onHideStrokeWidthSlider() },
                            isActive = activeTool == Tool.SHAPE,
                            icon = Icons.Default.CropSquare,
                            contentDescription = "Shape Tool"
                        )
                        EditorIconButton(
                            onClick = { viewModel.onToolSelected(Tool.TEXT); onHideStrokeWidthSlider() },
                            isActive = activeTool == Tool.TEXT,
                            icon = Icons.Default.Title,
                            contentDescription = "Text Tool"
                        )
                        EditorIconButton(
                            onClick = { viewModel.onToolSelected(Tool.IMAGE); onHideStrokeWidthSlider() },
                            isActive = activeTool == Tool.IMAGE,
                            icon = Icons.Default.Image,
                            contentDescription = "Image Tool"
                        )
                        EditorIconButton(
                            onClick = { viewModel.onToolSelected(Tool.STAMP); onHideStrokeWidthSlider() },
                            isActive = activeTool == Tool.STAMP,
                            icon = Icons.Default.Star,
                            contentDescription = "Stamp Tool"
                        )
                    }
                }

                // Shape sub-type pills (visible only when SHAPE is active)
                AnimatedVisibility(
                    visible = activeTool == Tool.SHAPE,
                    enter = fadeIn(tween(200)) + androidx.compose.animation.expandHorizontally(tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
                    exit = fadeOut(tween(180)) + androidx.compose.animation.shrinkHorizontally(tween(200))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShapeSubType.entries.forEach { subType ->
                            val isSelected = selectedShapeSubType == subType
                            TextButton(
                                onClick = { viewModel.onShapeSubTypeSelected(subType) },
                                modifier = Modifier.height(32.dp),
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor   = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    text = when (subType) {
                                        ShapeSubType.RECT   -> "□"
                                        ShapeSubType.CIRCLE -> "○"
                                        ShapeSubType.LINE   -> "—"
                                        ShapeSubType.ARROW  -> "→"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                VerticalDivider(Modifier.height(28.dp).padding(horizontal = 6.dp))

                // Recent colors (persisted)
                shownRecentColors.forEach { color ->
                    ColorChip(color = color, isSelected = selectedColor == color) {
                        viewModel.onColorSelected(color)
                    }
                }
                if (shownRecentColors.isNotEmpty()) {
                    VerticalDivider(Modifier.height(28.dp).padding(horizontal = 6.dp))
                }

                // Color chips
                toolColors.forEach { color ->
                    ColorChip(color = color, isSelected = selectedColor == color) {
                        viewModel.onColorSelected(color)
                    }
                }

                // Rainbow gradient circle → opens ColorPickerDialog
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color.Red, Color.Yellow, Color.Green,
                                    Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                )
                            )
                        )
                        .clickable { showColorPicker = true }
                )
            }

            // Right Zone
            Row(verticalAlignment = Alignment.CenterVertically) {
                val canUndo by viewModel.canUndo.collectAsState()
                val canRedo by viewModel.canRedo.collectAsState()

                val undoScale by animateFloatAsState(
                    targetValue = if (canUndo) 1f else 0.8f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "UndoScale"
                )
                val redoScale by animateFloatAsState(
                    targetValue = if (canRedo) 1f else 0.8f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "RedoScale"
                )

                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = canUndo,
                    modifier = Modifier.graphicsLayer { scaleX = undoScale; scaleY = undoScale }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                }
                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = canRedo,
                    modifier = Modifier.graphicsLayer { scaleX = redoScale; scaleY = redoScale }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                }
                IconButton(onClick = onExport) {
                    Icon(Icons.Outlined.FileUpload, contentDescription = "Export PDF")
                }

                // Paper style settings button
                val paperStyle by viewModel.paperStyle.collectAsState()
                IconButton(onClick = onPaperSettings) {
                    Icon(
                        imageVector = Icons.Default.Article,
                        contentDescription = "Paper Settings",
                        tint = if (paperStyle.background != PageBackground.BLANK)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                val inputMode by viewModel.inputMode.collectAsState()
                IconButton(onClick = { viewModel.cycleInputMode() }) {
                    Icon(
                        imageVector = when (inputMode) {
                            InputMode.FREE -> Icons.Filled.TouchApp
                            InputMode.PALM_REJECTION -> Icons.Filled.BackHand
                            InputMode.STYLUS_ONLY -> Icons.Filled.Create
                        },
                        contentDescription = when (inputMode) {
                            InputMode.FREE -> "Free (no filter)"
                            InputMode.PALM_REJECTION -> "Palm Rejection"
                            InputMode.STYLUS_ONLY -> "Stylus Only"
                        },
                        tint = when (inputMode) {
                            InputMode.FREE -> MaterialTheme.colorScheme.onSurface
                            InputMode.PALM_REJECTION -> MaterialTheme.colorScheme.tertiary
                            InputMode.STYLUS_ONLY -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaperStyleDialog(
    currentStyle: PaperStyle,
    hasExistingStrokes: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (PaperStyle) -> Unit
) {
    var selectedWidth by remember { mutableFloatStateOf(currentStyle.widthPt) }
    var selectedHeight by remember { mutableFloatStateOf(currentStyle.heightPt) }
    var selectedBackground by remember { mutableStateOf(currentStyle.background) }
    var showSizeWarning by remember { mutableStateOf(false) }

    // Secondary confirmation when a size change would shift existing strokes
    if (showSizeWarning) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSizeWarning = false },
            title = { Text("確認切換紙張大小") },
            text = { Text("切換紙張大小後，本頁現有筆劃的位置將會偏移，此操作無法還原。\n確定要繼續嗎？") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showSizeWarning = false
                    onConfirm(PaperStyle(selectedWidth, selectedHeight, selectedBackground))
                }) { Text("繼續") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showSizeWarning = false }) { Text("取消") }
            }
        )
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("紙張樣式設定") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Paper size ────────────────────────────────────────────────────────────────────
                Text("\u7d19\u5f35\u5927\u5c0f", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                val paperPresets = remember {
                    listOf(
                        Triple("A3",     842f,  1191f),
                        Triple("A4",     595f,   842f),
                        Triple("A5",     420f,   595f),
                        Triple("B5",     499f,   709f),
                        Triple("Letter", 612f,   792f),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    paperPresets.forEach { (label, w, h) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val isPortrait  = selectedWidth == w && selectedHeight == h
                            val isLandscape = selectedWidth == h && selectedHeight == w
                            FilterChip(
                                selected = isPortrait,
                                onClick = { selectedWidth = w; selectedHeight = h },
                                label = { Text("$label \u76f4\u5411") },
                                leadingIcon = if (isPortrait) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                            FilterChip(
                                selected = isLandscape,
                                onClick = { selectedWidth = h; selectedHeight = w },
                                label = { Text("$label \u6a6b\u5411") },
                                leadingIcon = if (isLandscape) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
                // ── Page background ──────────────────────────────────────────────────────────
                Text("\u9801\u9762\u80cc\u666f", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val bgOptions = listOf(
                        PageBackground.BLANK        to "\u7a7a\u767d",
                        PageBackground.RULED        to "\u6a6b\u7dda",
                        PageBackground.NARROW_RULED to "\u5bc6\u884c",
                        PageBackground.WIDE_RULED   to "\u5bec\u884c",
                        PageBackground.GRID         to "\u65b9\u683c",
                        PageBackground.DOT_GRID     to "\u9ede\u683c",
                    )
                    bgOptions.forEach { (bg, label) ->
                        FilterChip(
                            selected = selectedBackground == bg,
                            onClick = { selectedBackground = bg },
                            label = { Text(label) },
                            leadingIcon = if (selectedBackground == bg) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                val sizeChanged = selectedWidth != currentStyle.widthPt || selectedHeight != currentStyle.heightPt
                if (sizeChanged && hasExistingStrokes) {
                    showSizeWarning = true
                } else {
                    onConfirm(PaperStyle(selectedWidth, selectedHeight, selectedBackground))
                }
            }) { Text("確認") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// 新增: 顏色選擇的 Chip 元件，帶有選擇狀態
@Composable
private fun ColorChip(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val animatedBorder by animateDpAsState(if (isSelected) 2.5.dp else 0.dp, label = "ColorChipBorder")
    val chipScale by animateFloatAsState(
        targetValue = if (isSelected) 1.22f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "ColorChipScale"
    )
    Box(
        Modifier
            .size(40.dp)
            .graphicsLayer { scaleX = chipScale; scaleY = chipScale }
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .border(animatedBorder, MaterialTheme.colorScheme.primary, CircleShape)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun StrokeWidthSlider(viewModel: EditorViewModel) {
    val strokeWidth by viewModel.strokeWidth.collectAsState()
    val activeTool by viewModel.selectedTool.collectAsState()
    val range = if (activeTool == Tool.HIGHLIGHTER) 5f..40f else 1f..20f
    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "線寬",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(28.dp)
            )
            Slider(
                value = strokeWidth.coerceIn(range.start, range.endInclusive),
                onValueChange = { viewModel.onStrokeWidthChanged(it) },
                valueRange = range,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${strokeWidth.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(24.dp)
            )
        }
    }
}

@Composable
private fun EditorIconButton(
    onClick: () -> Unit,
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String
) {
    val animatedTintColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        label = "EditorButtonTint"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isActive) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "EditorIconScale"
    )

    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = animatedTintColor,
            modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }
        )
    }
}

/** Instantly scroll so [index] is vertically centered in the sidebar. */
private suspend fun LazyListState.scrollToCenter(index: Int) {
    scrollToItem(index)
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    val delta = (itemInfo.offset + itemInfo.size / 2 - viewportHeight / 2).toFloat()
    scroll { scrollBy(delta) }
}

/** Animated scroll so [index] is vertically centered in the sidebar. */
private suspend fun LazyListState.animateScrollToCenter(index: Int) {
    scrollToItem(index)
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    val delta = (itemInfo.offset + itemInfo.size / 2 - viewportHeight / 2).toFloat()
    animateScrollBy(delta)
}
