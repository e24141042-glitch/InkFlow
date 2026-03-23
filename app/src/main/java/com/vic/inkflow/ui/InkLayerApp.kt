package com.vic.inkflow.ui

import com.vic.inkflow.util.reorderable
import com.vic.inkflow.util.reorderableItem

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vic.inkflow.data.AppDatabase
import com.vic.inkflow.data.DocumentEntity
import com.vic.inkflow.data.ImageAnnotationEntity
import com.vic.inkflow.data.StrokeWithPoints
import com.vic.inkflow.data.TextAnnotationEntity
import com.vic.inkflow.ui.theme.BrandIndigo
import com.vic.inkflow.ui.theme.BrandPurple
import com.vic.inkflow.ui.theme.InkFlowTheme
import com.vic.inkflow.ui.theme.PaperDark
import com.vic.inkflow.ui.theme.PaperLight
import com.vic.inkflow.ui.theme.Slate50
import com.vic.inkflow.ui.theme.Slate100
import com.vic.inkflow.ui.theme.Slate900
import com.vic.inkflow.ui.theme.ToolbarGlassDark
import com.vic.inkflow.ui.theme.ToolbarGlassLight
import com.vic.inkflow.ui.theme.WorkspaceDeskDark
import com.vic.inkflow.ui.theme.WorkspaceDeskLight
import com.vic.inkflow.util.PdfManager
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
        Slate900,
        WorkspaceDeskDark,
        BrandIndigo.copy(alpha = 0.32f),
        BrandPurple.copy(alpha = 0.28f),
        Slate900,
    ) else listOf(
        Color(0xFFEAF0FF),
        Slate50,
        Color(0xFFF8F3FF),
        Color(0xFFEAF6FF),
        Slate100,
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

/**
 * Draws the animated flowing gradient as an isolated composable so its
 * recompositions (driven by the infinite animation) are scoped here only
 * and never propagate up to DocumentLibraryScreen.
 */
@Composable
private fun AnimatedGradientBackground(isDarkTheme: Boolean, modifier: Modifier = Modifier) {
    val brush = rememberFlowingBrandBrush(isDarkTheme)
    Box(modifier = modifier.background(brush))
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
    var showNewDocSizeDialog by remember { mutableStateOf(false) }

    if (showNewDocSizeDialog) {
        NewDocPaperSizeDialog(
            onDismiss = { showNewDocSizeDialog = false },
            onCreate = { widthPt, heightPt ->
                showNewDocSizeDialog = false
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val newUri = PdfManager.createBlankPdf(context, widthPt, heightPt)
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

    // Vivid brand gradient for the FAB / logo (never animated — static)
    val brandGradient = remember {
        androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(androidx.compose.ui.graphics.Color(0xFF6366F1), androidx.compose.ui.graphics.Color(0xFFA855F7))
        )
    }

    var selectedNavIndex by remember { mutableStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val normalizedQuery = searchQuery.trim()
    val filteredDocs = remember(documents, normalizedQuery, selectedNavIndex) {
        documents.filter { doc ->
            val matchesQuery = normalizedQuery.isEmpty() || doc.displayName.contains(normalizedQuery, ignoreCase = true)
            val matchesTab = when (selectedNavIndex) {
                1 -> doc.folderName != null // Assuming Folders tab shows documents that have folder
                2 -> doc.isFavorite
                else -> true // Home
            }
            matchesQuery && matchesTab
        }
    }
    var isGridView by rememberSaveable { mutableStateOf(true) }

    // Outer Box does NOT read any animated State, so it never recomposes at 60 fps.
    // The animated gradient is drawn by the isolated AnimatedGradientBackground child.
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedGradientBackground(isDarkTheme, Modifier.fillMaxSize())

    Row(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
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
                icon = { Icon(Icons.Default.Star, contentDescription = "收藏") },
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

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                LibraryHeroPanel(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    totalDocuments = documents.size,
                    visibleDocuments = filteredDocs.size,
                    isDarkTheme = isDarkTheme,
                    isGridView = isGridView,
                    onToggleGridView = { isGridView = !isGridView }
                )
            },
            floatingActionButton = {
                DocumentLibraryFab(
                    brandGradient = brandGradient,
                    showFabMenu = showFabMenu,
                    onToggleMenu = { showFabMenu = !showFabMenu },
                    onDismissMenu = { showFabMenu = false },
                    onOpenPdf = {
                        showFabMenu = false
                        pdfLauncher.launch(arrayOf("application/pdf"))
                    },
                    onCreateBlank = {
                        showFabMenu = false
                        showNewDocSizeDialog = true
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
            ) {
                AnimatedVisibility(
                    visible = normalizedQuery.isNotEmpty(),
                    enter = fadeIn(tween(220)) + expandVertically(tween(240)),
                    exit = fadeOut(tween(180)) + shrinkVertically(tween(200))
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "搜尋結果",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "\"$normalizedQuery\" 對應 ${filteredDocs.size} 份筆記",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { searchQuery = "" }) {
                                Text("清除搜尋")
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                if (filteredDocs.isEmpty()) {
                    LibraryEmptyState(
                        modifier = Modifier.fillMaxSize(),
                        brandGradient = brandGradient,
                        isSearchActive = normalizedQuery.isNotEmpty(),
                        searchQuery = normalizedQuery,
                        onClearSearch = { searchQuery = "" },
                        onOpenPdf = { pdfLauncher.launch(arrayOf("application/pdf")) },
                        onCreateBlank = { showNewDocSizeDialog = true }
                    )
                } else {
                    if (isGridView) {
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 180.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filteredDocs.size, key = { filteredDocs[it].uri }) { index ->
                                val doc = filteredDocs[index]
                                val coverBitmap by docViewModel.getDocumentThumbnail(context, doc.uri).collectAsState()
                                var visible by remember(doc.uri) { mutableStateOf(index >= 6) }
                                LaunchedEffect(doc.uri) {
                                    if (index < 6) {
                                        delay(index * 40L)
                                        visible = true
                                    }
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
                                        coverBitmap = coverBitmap,
                                        onClick = {
                                            val encodedUri = URLEncoder.encode(doc.uri, StandardCharsets.UTF_8.toString())
                                            navController.navigate("editor/$encodedUri")
                                        },
                                        onDelete = { docViewModel.delete(doc.uri) },
                                        onFavoriteToggle = { isFav -> docViewModel.toggleFavorite(doc.uri, isFav) },
                                        onRename = { newName -> docViewModel.rename(doc.uri, newName) }
                                    )
                                }
                            }
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredDocs.size, key = { filteredDocs[it].uri }) { index ->
                                val doc = filteredDocs[index]
                                val coverBitmap by docViewModel.getDocumentThumbnail(context, doc.uri).collectAsState()
                                var visible by remember(doc.uri) { mutableStateOf(index >= 8) }
                                LaunchedEffect(doc.uri) {
                                    if (index < 8) {
                                        delay(index * 30L)
                                        visible = true
                                    }
                                }
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = visible,
                                    enter = slideInVertically(
                                        initialOffsetY = { it / 2 },
                                        animationSpec = tween(300)
                                    ) + fadeIn(animationSpec = tween(300))
                                ) {
                                    DocumentListRow(
                                        document = doc,
                                        coverBitmap = coverBitmap,
                                        onClick = {
                                            val encodedUri = URLEncoder.encode(doc.uri, StandardCharsets.UTF_8.toString())
                                            navController.navigate("editor/$encodedUri")
                                        },
                                        onDelete = { docViewModel.delete(doc.uri) },
                                        onFavoriteToggle = { isFav -> docViewModel.toggleFavorite(doc.uri, isFav) },
                                        onRename = { newName -> docViewModel.rename(doc.uri, newName) }
                                    )
                                }
                            }
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
private fun LibraryHeroPanel(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    totalDocuments: Int,
    visibleDocuments: Int,
    isDarkTheme: Boolean,
    isGridView: Boolean,
    onToggleGridView: () -> Unit
) {
    val heroSurfaceColor = if (isDarkTheme) {
        ToolbarGlassDark.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.96f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = heroSurfaceColor,
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "InkFlow Studio",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "把今天的 PDF、草圖與註記集中在同一個工作台",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "首頁現在更像創作桌面：搜尋、開檔與新建入口都集中在這裡。"
                            } else {
                                "你正在檢視 \"${searchQuery.trim()}\" 的搜尋結果。"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(totalDocuments.toString(), style = MaterialTheme.typography.titleLarge)
                            Text("筆記庫", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            AnimatedVisibility(visible = searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "清除搜尋")
                                }
                            }
                        },
                        placeholder = { Text("搜尋標題、文件名稱或近期開啟的筆記") },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onToggleGridView,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.Apps,
                            contentDescription = "切換檢視",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LibraryStatPill(
                        title = "目前顯示",
                        value = "$visibleDocuments 份"
                    )
                    LibraryStatPill(
                        title = if (searchQuery.isBlank()) "狀態" else "搜尋模式",
                        value = if (searchQuery.isBlank()) "工作台待命" else "已套用關鍵字"
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryStatPill(title: String, value: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun DocumentLibraryFab(
    brandGradient: Brush,
    showFabMenu: Boolean,
    onToggleMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onOpenPdf: () -> Unit,
    onCreateBlank: () -> Unit
) {
    Box {
        val fabInteractionSource = remember { MutableInteractionSource() }
        val isFabPressed by fabInteractionSource.collectIsPressedAsState()
        val fabScale by animateFloatAsState(
            targetValue = if (isFabPressed) 0.95f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "FabScale"
        )

        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                .clip(RoundedCornerShape(24.dp))
                .background(brandGradient)
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                .clickable(interactionSource = fabInteractionSource, indication = androidx.compose.foundation.LocalIndication.current, onClick = onToggleMenu)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                }
                Column {
                    Text("新增筆記", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    Text("空白頁或匯入 PDF", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        androidx.compose.material3.DropdownMenu(
            expanded = showFabMenu,
            onDismissRequest = onDismissMenu
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("開啟 PDF") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                onClick = onOpenPdf
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("空白筆記") },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = onCreateBlank
            )
        }
    }
}

@Composable
private fun LibraryEmptyState(
    modifier: Modifier = Modifier,
    brandGradient: Brush,
    isSearchActive: Boolean,
    searchQuery: String,
    onClearSearch: () -> Unit,
    onOpenPdf: () -> Unit,
    onCreateBlank: () -> Unit
) {
    val emptyStateFloat by rememberInfiniteTransition(label = "EmptyIconFloat")
        .animateFloat(
            initialValue = 0f,
            targetValue = -14f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "EmptyFloatY"
        )
    val emptyStateAlpha by rememberInfiniteTransition(label = "EmptyIconPulse")
        .animateFloat(
            initialValue = 0.28f,
            targetValue = 0.58f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "EmptyAlpha"
        )
    var emptyVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(90); emptyVisible = true }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = emptyVisible,
            enter = fadeIn(tween(500)) + scaleIn(tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialScale = 0.88f)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.96f),
                shape = RoundedCornerShape(30.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .graphicsLayer { translationY = emptyStateFloat }
                            .clip(CircleShape)
                            .background(brandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Search else Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = null,
                            modifier = Modifier.size(38.dp),
                            tint = Color.White.copy(alpha = emptyStateAlpha + 0.34f)
                        )
                    }

                    Text(
                        text = if (isSearchActive) "沒有找到符合 \"$searchQuery\" 的筆記" else "靈感工作台已經就位",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isSearchActive) {
                            "可以清除搜尋條件，或直接建立新的草稿頁，把這組想法先存下來。"
                        } else {
                            "從這裡開啟第一份 PDF，或直接建立一張空白頁，開始你的註記流程。"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSearchActive) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.clickable(onClick = onClearSearch)
                            ) {
                                Text(
                                    text = "清除搜尋",
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(brandGradient)
                                    .clickable(onClick = onOpenPdf)
                            ) {
                                Text(
                                    text = "開啟 PDF",
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable(onClick = onCreateBlank)
                        ) {
                            Text(
                                text = "建立空白筆記",
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(
    document: DocumentEntity,
    coverBitmap: Bitmap?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit = {},
    onFavoriteToggle: (Boolean) -> Unit = {}
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameInput by remember(document.displayName) { mutableStateOf(document.displayName) }
    val isDarkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardShellColor = if (isDarkSurface) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.98f)
    }
    val cardCoverColor = if (isDarkSurface) PaperDark else PaperLight

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

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("刪除筆記本") },
            text = {
                Text("確定要刪除「${document.displayName}」嗎？此操作會一併移除筆跡與標註，且無法復原。")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    val brandGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        listOf(
            BrandIndigo.copy(alpha = 0.12f),
            BrandPurple.copy(alpha = 0.10f)
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

    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f).graphicsLayer { scaleX = cardScale; scaleY = cardScale },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = cardShellColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 6.dp),
        interactionSource = cardInteractionSource
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Cover 60%
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(cardCoverColor),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(brandGradient)
                )
                Crossfade(
                    targetState = coverBitmap,
                    animationSpec = tween(400),
                    label = "CardCoverFade"
                ) { bitmap ->
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = BrandIndigo.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                // Favorite Button
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = { onFavoriteToggle(!document.isFavorite) },
                        modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = if (document.isFavorite) Color.Yellow else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
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
                        style = MaterialTheme.typography.titleSmall,
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
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

    var showAiPanel by rememberSaveable { mutableStateOf(false) }
    var aiFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
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
    val isPageOperationInProgress by pdfViewModel.isPageOperationInProgress.collectAsState()

    val insertPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedUri ->
        if (selectedUri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                selectedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        pdfViewModel.insertPdfPages(
            context = context,
            documentUri = uri.toString(),
            sourceUri = selectedUri,
            afterIndex = currentPageIndex
        )
    }

    // When the PDF first loads, initialize the EditorViewModel's model space to match the first page.
    val firstPageSize by pdfViewModel.firstPageSize.collectAsState()
    androidx.compose.runtime.LaunchedEffect(firstPageSize) {
        firstPageSize?.let { (w, h) -> viewModel.initializePaperSize(w, h) }
    }

    // Document settings dialog state
    var showDocumentSettingsDialog by remember { mutableStateOf(false) }
    val paperStyle by viewModel.paperStyle.collectAsState()
    if (showDocumentSettingsDialog) {
        DocumentSettingsDialog(
            documentTitle = documentTitle,
            pageCount = pageCount,
            currentPageIndex = currentPageIndex,
            isPageOperationInProgress = isPageOperationInProgress,
            currentStyle = paperStyle,
            onDismiss = { showDocumentSettingsDialog = false },
            onConfirmStyle = { newStyle ->
                viewModel.setPaperStyle(newStyle)
            },
            onInsertPdf = {
                showDocumentSettingsDialog = false
                insertPdfLauncher.launch(arrayOf("application/pdf"))
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
            onDocumentSettings = { showDocumentSettingsDialog = true }
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
                    scope.launch {
                        pdfViewModel.insertBlankPage(
                            context,
                            uri.toString(),
                            afterIndex,
                            pageWidthPt = paperStyle.widthPt,
                            pageHeightPt = paperStyle.heightPt
                        )
                    }
                },
                onDeletePages = { indices ->
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        // 必須從大到小刪除，才不會影響前面頁面的 index
                        val sortedIndices = indices.sortedDescending()
                        sortedIndices.forEach { index ->
                            db.strokeDao().clearPage(uri.toString(), index)
                            db.strokeDao().shiftPageIndicesDown(uri.toString(), index)
                            db.imageAnnotationDao().deleteForPage(uri.toString(), index)
                            db.imageAnnotationDao().shiftPageIndicesDown(uri.toString(), index)
                            db.textAnnotationDao().deleteForPage(uri.toString(), index)
                            db.textAnnotationDao().shiftPageIndicesDown(uri.toString(), index)
                        }
                    }
                    pdfViewModel.deletePages(uri.toString(), indices)
                },
                listState = sidebarListState,
                modifier = if (isFullscreen) Modifier.fillMaxHeight().weight(1f) else Modifier.fillMaxHeight()
            )

            if (!isFullscreen) {
                // Left Panel: AI Parser View
                androidx.compose.animation.AnimatedVisibility(
                    visible = showAiPanel,
                    modifier = Modifier.weight(0.4f).fillMaxHeight()
                ) {
                    AiWebPanel(
                        fileUri = aiFileUri,
                        onClose = {
                            showAiPanel = false
                            aiFileUri = null
                        }
                    )
                }
                if (showAiPanel) {
                    androidx.compose.material3.VerticalDivider(Modifier.fillMaxHeight().width(1.dp))
                }

                // Main Workspace
                val workspaceWeight = if (showAiPanel) 0.6f else 1f
                Box(Modifier.weight(workspaceWeight).fillMaxHeight()) {
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
                                                pdfViewModel.insertBlankPage(context, uri.toString(), sourcePageIndex)

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
                                TextButton(
                                    onClick = {
                                        isExtracting = true
                                        scope.launch {
                                            try {
                                                val sourcePageIndex = currentPageIndex
                                                val sourceBitmap = kotlinx.coroutines.withTimeoutOrNull(1200) {
                                                    pdfViewModel.getPageBitmap(sourcePageIndex).filterNotNull().first()
                                                } ?: pdfViewModel.getPageBitmap(sourcePageIndex).value

                                                val file = viewModel.extractRegionToShareFile(
                                                    context = context,
                                                    sourcePageIndex = sourcePageIndex,
                                                    pdfPageBitmap = sourceBitmap
                                                )
                                                if (file != null) {
                                                    val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        file
                                                    )
                                                    aiFileUri = fileUri
                                                    showAiPanel = true
                                                }
                                            } finally {
                                                isExtracting = false
                                            }
                                        }
                                    }
                                ) {
                                    Text("AI 解析")
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
    onDeletePages: (List<Int>) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    var deleteConfirmIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedPages by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showOnlyBookmarked by remember { mutableStateOf(false) }
    val bookmarkedPages by pdfViewModel.getBookmarkedPages(documentUri).collectAsState(initial = emptyList())
    val visibleIndices = remember(pageCount, showOnlyBookmarked, bookmarkedPages, sidebarMode) {
        if (showOnlyBookmarked && sidebarMode == SidebarMode.FULLSCREEN) {
            (0 until pageCount).filter { it in bookmarkedPages }
        } else {
            (0 until pageCount).toList()
        }
    }
    val thumbnailVersion by pdfViewModel.thumbnailVersion.collectAsState()
    val isPageOperationInProgress by pdfViewModel.isPageOperationInProgress.collectAsState()
    if (deleteConfirmIndices.isNotEmpty()) {
        val indices = deleteConfirmIndices
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteConfirmIndices = emptyList() },
            title = { Text("刪除頁面") },
            text = { Text(if (indices.size == 1) "確定要刪除第 ${indices[0] + 1} 頁？此操作無法復原。" else "確定要刪除這 ${indices.size} 頁？此操作無法復原。") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        deleteConfirmIndices = emptyList()
                        onDeletePages(indices)
                        isSelectionMode = false
                        selectedPages = emptySet()
                    },
                    enabled = !isPageOperationInProgress
                ) { Text("刪除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteConfirmIndices = emptyList() }) {
                    Text("取消")
                }
            }
        )
    }
    val animatedWidth by animateDpAsState(
        targetValue = if (sidebarMode == SidebarMode.COLLAPSED) 68.dp else 204.dp,
        animationSpec = tween(300),
        label = "SidebarWidthAnimation"
    )

    val isDarkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val sidebarColor = if (isDarkSurface) {
        ToolbarGlassDark.copy(alpha = 0.94f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f)
    }

    Surface(
        modifier = if (sidebarMode == SidebarMode.FULLSCREEN) modifier else modifier.width(animatedWidth),
        color = sidebarColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        if (sidebarMode == SidebarMode.FULLSCREEN) {
            // Fullscreen: 4-column page grid with back button
            val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
            var dragOrder by remember(visibleIndices, thumbnailVersion) { mutableStateOf(visibleIndices) }
            val reorderState = com.vic.inkflow.util.rememberReorderableLazyGridState(
                gridState = gridState,
                onMove = { from, to ->
                    val newOrder = dragOrder.toMutableList()
                    val item = newOrder.removeAt(from)
                    newOrder.add(to, item)
                    dragOrder = newOrder
                },
                onDragEnd = { from, to ->
                    if (!showOnlyBookmarked && !isSelectionMode) {
                        pdfViewModel.movePage(documentUri, visibleIndices[from], visibleIndices[to])
                    }
                }
            )
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
                    if (isSelectionMode) {
                        IconButton(onClick = { 
                            isSelectionMode = false
                            selectedPages = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Mode")
                        }
                        Text(
                            text = "已選取 ${selectedPages.size} 頁",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.TextButton(onClick = {
                            if (selectedPages.size == pageCount) selectedPages = emptySet() else selectedPages = (0 until pageCount).toSet()
                        }) {
                            Text(if (selectedPages.size == pageCount) "取消全選" else "全選")
                        }
                        IconButton(onClick = {
                            if (selectedPages.isNotEmpty() && !isPageOperationInProgress) {
                                deleteConfirmIndices = selectedPages.toList()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "刪除選擇", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { onModeChange(SidebarMode.NORMAL) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit grid view")
                        }
                        Text(
                            text = "所有頁面",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.FilterChip(
                            selected = showOnlyBookmarked,
                            onClick = { showOnlyBookmarked = !showOnlyBookmarked },
                            label = { Text("書籤") },
                            leadingIcon = { if (showOnlyBookmarked) Icon(Icons.Default.Star, null) else Icon(Icons.Outlined.BookmarkBorder, null) }
                        )
                        IconButton(onClick = { isSelectionMode = true }) {
                            Icon(Icons.Default.Check, contentDescription = "Select Pages")
                        }
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(180.dp),
                    state = gridState,
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().reorderable(reorderState, enabled = !showOnlyBookmarked && !isSelectionMode && !isPageOperationInProgress)
                ) {
                    items(count = dragOrder.size, key = { dragOrder[it] }) { it ->
                        val index = dragOrder[it]
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
                        val canDrag = !showOnlyBookmarked && !isSelectionMode && !isPageOperationInProgress
                        Box(modifier = Modifier.reorderableItem(reorderState, it).then(
                            if (canDrag) {
                                Modifier.clickable {
                                    onPageSelected(index)
                                    onModeChange(SidebarMode.NORMAL)
                                }
                            } else {
                                Modifier.combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            if (index in selectedPages) selectedPages -= index else selectedPages += index
                                        } else {
                                            onPageSelected(index)
                                            onModeChange(SidebarMode.NORMAL)
                                        }
                                    },
                                    onLongClick = { 
                                        if (!isSelectionMode && pageCount > 1 && !isPageOperationInProgress) {
                                            isSelectionMode = true
                                            selectedPages += index
                                        }
                                    }
                                )
                            }
                        )) {
                            Box {
                                PageThumbnail(
                                    pageIndex = index,
                                    bitmap = thumb,
                                    strokes = strokes,
                                    imageAnnotations = images,
                                    textAnnotations = texts,
                                    isSelected = isSelectionMode && index in selectedPages || (!isSelectionMode && index == currentPageIndex),
                                    isBookmarked = index in bookmarkedPages,
                                    onBookmarkToggle = { newState -> pdfViewModel.toggleBookmark(documentUri, index, newState) },
                                    boxModifier = Modifier.fillMaxWidth().aspectRatio(pdfViewModel.getPageAspectRatio(index))
                                        .let { if (isSelectionMode) it.padding(8.dp) else it }
                                )
                                if (isSelectionMode) {
                                    androidx.compose.material3.Checkbox(
                                        checked = index in selectedPages,
                                        onCheckedChange = { chk ->
                                            if (chk) selectedPages += index else selectedPages -= index
                                        },
                                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                                    )
                                }
                            }
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
                                    onLongClick = { if (pageCount > 1 && !isPageOperationInProgress) deleteConfirmIndices = listOf(index) }
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
                                    isBookmarked = index in bookmarkedPages,
                                    onBookmarkToggle = { newState -> pdfViewModel.toggleBookmark(documentUri, index, newState) },
                                    boxModifier = Modifier.width(88.dp).aspectRatio(pdfViewModel.getPageAspectRatio(index))
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
                    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.TextButton(
                            onClick = { expanded = true },
                            enabled = !isPageOperationInProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("新增頁面", style = MaterialTheme.typography.labelMedium)
                        }
                        
                        androidx.compose.material3.DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("空白紙 (Blank)") },
                                onClick = { 
                                    expanded = false
                                    onAddPage(currentPageIndex) 
                                }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("橫線紙 (Lined)") },
                                onClick = { 
                                    expanded = false
                                    onAddPage(currentPageIndex) 
                                }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("網格紙 (Grid)") },
                                onClick = { 
                                    expanded = false
                                    onAddPage(currentPageIndex) 
                                }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("點狀紙 (Dotted)") },
                                onClick = { 
                                    expanded = false
                                    onAddPage(currentPageIndex) 
                                }
                            )
                        }
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
    isBookmarked: Boolean = false,
    onBookmarkToggle: ((Boolean) -> Unit)? = null,
    boxModifier: Modifier = Modifier.width(88.dp).aspectRatio(1f / 1.414f)
) {
    val context = LocalContext.current
    val isDarkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val paperColor = if (isDarkSurface) PaperDark else PaperLight
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
            targetValue = if (isSelected) 1.06f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "ThumbScale"
        )
        Box(
            modifier = boxModifier
                .graphicsLayer { scaleX = thumbScale; scaleY = thumbScale }
                .clip(RoundedCornerShape(18.dp))
                .background(paperColor, shape = RoundedCornerShape(18.dp))
                .border(
                    width = animatedBorderWidth,
                    color = animatedBorderColor,
                    shape = RoundedCornerShape(18.dp)
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
            Spacer(modifier = Modifier.fillMaxSize().drawWithCache {
                val modelW = 595f
                val modelH = 842f
                val sx = size.width / modelW
                val sy = size.height / modelH
                
                val bmpWidth = size.width.toInt().coerceAtLeast(1)
                val bmpHeight = size.height.toInt().coerceAtLeast(1)
                val cachedImage = androidx.compose.ui.graphics.ImageBitmap(bmpWidth, bmpHeight, androidx.compose.ui.graphics.ImageBitmapConfig.Argb8888)
                val cacheCanvas = androidx.compose.ui.graphics.Canvas(cachedImage)
                
                val drawScope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
                drawScope.draw(
                    androidx.compose.ui.unit.Density(1f),
                    androidx.compose.ui.unit.LayoutDirection.Ltr,
                    cacheCanvas,
                    size
                ) {
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
                
                onDrawBehind {
                    drawImage(cachedImage)
                }
            })
            if (onBookmarkToggle != null) {
                androidx.compose.material3.IconButton(
                    onClick = { onBookmarkToggle(!isBookmarked) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.Star,
                        contentDescription = "Toggle Bookmark",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(if (isBookmarked) "🔖 Page ${pageIndex + 1}" else "Page ${pageIndex + 1}", style = MaterialTheme.typography.labelSmall)
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
            .size(44.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                shape = RoundedCornerShape(14.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f), shape = RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("${pageIndex + 1}", style = MaterialTheme.typography.labelMedium)
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
    val isDarkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // Animate container colour on dark/light switch
    val btnContainerColor by animateColorAsState(
        targetValue = if (isDarkSurface) ToolbarGlassDark.copy(alpha = 0.94f) else ToolbarGlassLight.copy(alpha = 0.92f),
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
    val isDarkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val lightWorkspaceSurface = MaterialTheme.colorScheme.surfaceContainerLow
    val deskBrush = remember(isDarkSurface, lightWorkspaceSurface) {
        Brush.linearGradient(
            colors = if (isDarkSurface) {
                listOf(WorkspaceDeskDark, Slate900, BrandIndigo.copy(alpha = 0.10f))
            } else {
                listOf(WorkspaceDeskLight, Slate50, lightWorkspaceSurface)
            }
        )
    }
    val paperColor = if (isDarkSurface) PaperDark else PaperLight
    val stageColor = if (isDarkSurface) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.30f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.76f)
    }

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
            .background(deskBrush)
            .onSizeChanged { size ->
                containerWidth = size.width
                containerHeight = size.height
            }
            .pointerInput(minScale) {
                // Pan / zoom handler for the Workspace background Box.
                //
                // Routing contract (agreed with InkCanvas.pointerInput):
                //   FREE          — InkCanvas does NOT consume Touch events → all single-finger
                //                   touch contacts bubble here for pan.  Stylus is consumed by
                //                   InkCanvas for drawing.
                //   PALM_REJECTION — InkCanvas does NOT consume finger-zone contacts → single
                //                   finger pans here.  Stylus consumed for drawing.  Palm dropped.
                //   STYLUS_ONLY   — InkCanvas does NOT consume any Touch (finger/palm) → all
                //                   touch contacts bubble here for single-finger pan.
                //
                // For 2+ simultaneous pointers this handler handles pinch-zoom + two-finger pan
                // regardless of mode (InkCanvas always passes multi-touch through in FREE /
                // STYLUS_ONLY, and passes finger-only multi-touch through in PALM_REJECTION).
                awaitEachGesture {
                    // requireUnconsumed = false: picks up events not consumed by children (InkCanvas).
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    if (firstDown.isConsumed) return@awaitEachGesture  // InkCanvas claimed it (stylus draw)

                    val touchSlop = viewConfiguration.touchSlop
                    var accZoom = 1f
                    var accPan = androidx.compose.ui.geometry.Offset.Zero
                    var pastTouchSlop = false

                    while (true) {
                        val evt = awaitPointerEvent()
                        // If any change was consumed by a child (shouldn't happen after firstDown
                        // check, but be safe) stop handling.
                        if (evt.changes.any { it.isConsumed }) break
                        if (evt.changes.none { it.pressed }) break

                        val zoomChange = evt.calculateZoom()
                        val panChange  = evt.calculatePan()

                        if (!pastTouchSlop) {
                            accZoom *= zoomChange
                            accPan  += panChange
                            val centroidSize = evt.calculateCentroidSize(useCurrent = false)
                            val zoomMotion = kotlin.math.abs(1 - accZoom) * centroidSize
                            val panMotion  = accPan.getDistance()
                            if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                pastTouchSlop = true
                                accZoom = 1f
                                accPan  = androidx.compose.ui.geometry.Offset.Zero
                            }
                        }

                        // If InkCanvas claimed the gesture on this frame, bail out before
                        // applying any offset change from the same motion sample.
                        if (evt.changes.any { it.isConsumed }) break

                        if (pastTouchSlop) {
                            val centroid = evt.calculateCentroid(useCurrent = false)
                            val oldScale = scale
                            // Single-finger pan: zoom = 1 so scale is unchanged.
                            // Multi-finger pinch: zoom != 1 so scale changes.
                            val newScale = (scale * zoomChange).coerceIn(minScale, 5f)
                            if (newScale <= minScale) {
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                val r = newScale / oldScale
                                offsetX = offsetX * r + (centroid.x - containerWidth / 2f) * (1 - r) + panChange.x
                                offsetY = offsetY * r + (centroid.y - containerHeight / 2f) * (1 - r) + panChange.y
                            }
                            scale = newScale
                            evt.changes.forEach { it.consume() }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(stageColor)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(32.dp)
                )
        )
        Surface(
            modifier = Modifier
                .fillMaxSize(0.86f)
                .aspectRatio(pageAspectRatio, matchHeightConstraintsFirst = true)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 18.dp,
            color = paperColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
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
    onDocumentSettings: () -> Unit = {}
) {
    val activeTool by viewModel.selectedTool.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val recentColors by viewModel.recentColors.collectAsState()
    val selectedShapeSubType by viewModel.selectedShapeSubType.collectAsState()
    val selectedLassoSubType by viewModel.selectedLassoSubType.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val inputMode by viewModel.inputMode.collectAsState()
    val strokeWidth by viewModel.strokeWidth.collectAsState()
    val toolColors = listOf(Color.Black, Color(0xFF_FFC700), Color(0xFF_F44336), Color(0xFF_4CAF50))
    val shownRecentColors = recentColors.filterNot { it in toolColors }.take(8)
    var showColorPicker by remember { mutableStateOf(false) }
    val isDarkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shellColor = if (isDarkSurface) ToolbarGlassDark else ToolbarGlassLight
    val clusterColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isDarkSurface) 0.78f else 0.94f)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
    val toolButtonSize = 32.dp
    val utilityButtonSize = 34.dp

    val drawingTools = listOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.LASSO)
    val drawingActiveIdx = drawingTools.indexOf(activeTool).let { if (it < 0) -1 else it }
    val drawingHighlightOffset by animateDpAsState(
        targetValue = if (drawingActiveIdx >= 0) toolButtonSize * drawingActiveIdx else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "DrawingToolHighlight"
    )

    val annotationTools = listOf(Tool.SHAPE, Tool.TEXT, Tool.IMAGE, Tool.STAMP)
    val annotationActiveIdx = annotationTools.indexOf(activeTool).let { if (it < 0) -1 else it }
    val annotationHighlightOffset by animateDpAsState(
        targetValue = if (annotationActiveIdx >= 0) toolButtonSize * annotationActiveIdx else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "AnnotationToolHighlight"
    )

    val undoScale by animateFloatAsState(
        targetValue = if (canUndo) 1f else 0.84f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "UndoScale"
    )
    val redoScale by animateFloatAsState(
        targetValue = if (canRedo) 1f else 0.84f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "RedoScale"
    )
    if (showColorPicker) {
        ColorPickerDialog(
            onColorSelected = { viewModel.onColorSelected(it) },
            onDismiss = { showColorPicker = false }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                color = shellColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight().padding(start = 6.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(utilityButtonSize)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Library")
                    }
                    Column(modifier = Modifier.widthIn(max = 156.dp)) {
                        Text(
                            text = documentTitle,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "InkFlow Studio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                color = shellColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            
                            .padding(horizontal = 3.dp, vertical = 3.dp)
                    ) {
                        if (drawingActiveIdx >= 0) {
                            Box(
                                modifier = Modifier
                                    .offset(x = drawingHighlightOffset)
                                    .size(toolButtonSize)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                BrandPurple.copy(alpha = 0.82f)
                                            )
                                        ),
                                        RoundedCornerShape(16.dp)
                                    )
                            )
                        }
                        Row {
                            EditorIconButton(
                                onClick = {
                                    if (activeTool == Tool.PEN) onToggleStrokeWidthSlider()
                                    else {
                                        viewModel.onToolSelected(Tool.PEN)
                                        onHideStrokeWidthSlider()
                                    }
                                },
                                isActive = activeTool == Tool.PEN,
                                icon = Icons.Default.Create,
                                contentDescription = "Pen Tool"
                            )
                            EditorIconButton(
                                onClick = {
                                    if (activeTool == Tool.HIGHLIGHTER) onToggleStrokeWidthSlider()
                                    else {
                                        viewModel.onToolSelected(Tool.HIGHLIGHTER)
                                        onHideStrokeWidthSlider()
                                    }
                                },
                                isActive = activeTool == Tool.HIGHLIGHTER,
                                icon = Icons.Rounded.Brush,
                                contentDescription = "Highlighter Tool"
                            )
                            EditorIconButton(
                                onClick = {
                                    viewModel.onToolSelected(Tool.ERASER)
                                    onHideStrokeWidthSlider()
                                },
                                isActive = activeTool == Tool.ERASER,
                                icon = Icons.Rounded.Delete,
                                contentDescription = "Eraser Tool"
                            )
                            EditorIconButton(
                                onClick = {
                                    viewModel.onToolSelected(Tool.LASSO)
                                    onHideStrokeWidthSlider()
                                },
                                isActive = activeTool == Tool.LASSO,
                                icon = Icons.Rounded.Gesture,
                                contentDescription = "Lasso Select Tool"
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = activeTool == Tool.LASSO,
                        enter = fadeIn(tween(200)) + androidx.compose.animation.expandHorizontally(tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
                        exit = fadeOut(tween(180)) + androidx.compose.animation.shrinkHorizontally(tween(200))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                
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
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(
                                        text = when (subType) {
                                            LassoSubType.FREEFORM -> "自由圈選"
                                            LassoSubType.RECT -> "矩形圈選"
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }

                    VerticalDivider(Modifier.height(24.dp).padding(horizontal = 8.dp))

                    Box(
                        modifier = Modifier
                            
                            .padding(horizontal = 3.dp, vertical = 3.dp)
                    ) {
                        if (annotationActiveIdx >= 0) {
                            Box(
                                modifier = Modifier
                                    .offset(x = annotationHighlightOffset)
                                    .size(toolButtonSize)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                BrandIndigo.copy(alpha = 0.78f)
                                            )
                                        ),
                                        RoundedCornerShape(16.dp)
                                    )
                            )
                        }
                        Row {
                            EditorIconButton(
                                onClick = {
                                    viewModel.onToolSelected(Tool.SHAPE)
                                    onHideStrokeWidthSlider()
                                },
                                isActive = activeTool == Tool.SHAPE,
                                icon = Icons.Default.CropSquare,
                                contentDescription = "Shape Tool"
                            )
                            EditorIconButton(
                                onClick = {
                                    viewModel.onToolSelected(Tool.TEXT)
                                    onHideStrokeWidthSlider()
                                },
                                isActive = activeTool == Tool.TEXT,
                                icon = Icons.Default.Title,
                                contentDescription = "Text Tool"
                            )
                            EditorIconButton(
                                onClick = {
                                    viewModel.onToolSelected(Tool.IMAGE)
                                    onHideStrokeWidthSlider()
                                },
                                isActive = activeTool == Tool.IMAGE,
                                icon = Icons.Default.Image,
                                contentDescription = "Image Tool"
                            )
                            EditorIconButton(
                                onClick = {
                                    viewModel.onToolSelected(Tool.STAMP)
                                    onHideStrokeWidthSlider()
                                },
                                isActive = activeTool == Tool.STAMP,
                                icon = Icons.Default.Star,
                                contentDescription = "Stamp Tool"
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = activeTool == Tool.SHAPE,
                        enter = fadeIn(tween(200)) + androidx.compose.animation.expandHorizontally(tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
                        exit = fadeOut(tween(180)) + androidx.compose.animation.shrinkHorizontally(tween(200))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                
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
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(
                                        text = when (subType) {
                                            ShapeSubType.RECT -> "方框"
                                            ShapeSubType.CIRCLE -> "圓形"
                                            ShapeSubType.LINE -> "直線"
                                            ShapeSubType.ARROW -> "箭頭"
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }

                    VerticalDivider(Modifier.height(24.dp).padding(horizontal = 8.dp))

                    Row(
                        modifier = Modifier
                            .background(clusterColor, CircleShape)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        shownRecentColors.forEach { color ->
                            ColorChip(color = color, isSelected = selectedColor == color) {
                                viewModel.onColorSelected(color)
                            }
                        }
                        if (shownRecentColors.isNotEmpty()) {
                            Spacer(Modifier.width(4.dp))
                        }
                        toolColors.forEach { color ->
                            ColorChip(color = color, isSelected = selectedColor == color) {
                                viewModel.onColorSelected(color)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .padding(3.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color.Red,
                                            Color.Yellow,
                                            Color.Green,
                                            Color.Cyan,
                                            Color.Blue,
                                            Color.Magenta,
                                            Color.Red
                                        )
                                    )
                                )
                                .clickable { showColorPicker = true }
                        )
                    }

                    AnimatedVisibility(
                        visible = showStrokeWidthSlider && (activeTool == Tool.PEN || activeTool == Tool.HIGHLIGHTER),
                        enter = fadeIn(tween(180)) + androidx.compose.animation.expandHorizontally(tween(220)),
                        exit = fadeOut(tween(150)) + androidx.compose.animation.shrinkHorizontally(tween(180))
                    ) {
                        Surface(
                            modifier = Modifier.padding(start = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "線寬 ${strokeWidth.toInt()} px",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                color = shellColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = canUndo,
                        modifier = Modifier
                            .size(utilityButtonSize)
                            .graphicsLayer { scaleX = undoScale; scaleY = undoScale }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = canRedo,
                        modifier = Modifier
                            .size(utilityButtonSize)
                            .graphicsLayer { scaleX = redoScale; scaleY = redoScale }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        IconButton(onClick = onExport, modifier = Modifier.size(utilityButtonSize)) {
                            Icon(
                                Icons.Outlined.FileUpload,
                                contentDescription = "Export PDF",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    IconButton(onClick = onDocumentSettings, modifier = Modifier.size(utilityButtonSize)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Article,
                            contentDescription = "Document Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.cycleInputMode() }, modifier = Modifier.size(utilityButtonSize)) {
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
}

@Composable
private fun DocumentSettingsDialog(
    documentTitle: String,
    pageCount: Int,
    currentPageIndex: Int,
    currentStyle: PaperStyle,
    isPageOperationInProgress: Boolean,
    onDismiss: () -> Unit,
    onConfirmStyle: (PaperStyle) -> Unit,
    onInsertPdf: () -> Unit
) {
    var selectedBackground by remember { mutableStateOf(currentStyle.background) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文件設定中心", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "在這裡調整目前文件的顯示設定，或把另一份 PDF 插入目前頁後方。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("文件資訊", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = documentTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "共 $pageCount 頁，目前第 ${currentPageIndex + 1} 頁",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "頁面背景樣式",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val bgOptions = listOf(
                    PageBackground.BLANK to "空白",
                    PageBackground.RULED to "橫線",
                    PageBackground.NARROW_RULED to "密行",
                    PageBackground.WIDE_RULED to "寬行",
                    PageBackground.GRID to "方格",
                    PageBackground.DOT_GRID to "點格",
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("插入 PDF", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (isPageOperationInProgress) {
                                    "正在更新頁面，完成前暫時無法再插入。"
                                } else {
                                    "從檔案挑選 PDF，插入到目前第 ${currentPageIndex + 1} 頁後方。"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = onInsertPdf,
                            enabled = !isPageOperationInProgress,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(if (isPageOperationInProgress) "匯入中" else "選擇 PDF")
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                onConfirmStyle(currentStyle.copy(background = selectedBackground))
                onDismiss()
            }) { Text("確認") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun NewDocPaperSizeDialog(
    onDismiss: () -> Unit,
    onCreate: (widthPt: Float, heightPt: Float) -> Unit
) {
    var selectedWidth by remember { mutableFloatStateOf(595f) }
    var selectedHeight by remember { mutableFloatStateOf(842f) }

    val paperPresets = remember {
        listOf(
            Triple("A3", 842f, 1191f),
            Triple("A4", 595f, 842f),
            Triple("A5", 420f, 595f),
            Triple("B5", 499f, 709f),
            Triple("Letter", 612f, 792f),
        )
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("選擇紙張大小", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "設定新空白筆記的紙張大小（建立後無法更改）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                paperPresets.forEach { (label, w, h) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isPortrait = selectedWidth == w && selectedHeight == h
                        val isLandscape = selectedWidth == h && selectedHeight == w
                        FilterChip(
                            selected = isPortrait,
                            onClick = { selectedWidth = w; selectedHeight = h },
                            label = { Text("$label 直向") },
                            leadingIcon = if (isPortrait) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = isLandscape,
                            onClick = { selectedWidth = h; selectedHeight = w },
                            label = { Text("$label 橫向") },
                            leadingIcon = if (isLandscape) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                onCreate(selectedWidth, selectedHeight)
            }) { Text("建立") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// 新增: 顏色選擇的 Chip 元件，帶有選擇狀態
@Composable
private fun ColorChip(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val animatedBorder by animateDpAsState(if (isSelected) 2.dp else 1.dp, label = "ColorChipBorder")
    val chipScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "ColorChipScale"
    )
    val ringColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        label = "ColorChipRing"
    )
    Box(
        Modifier
            .size(34.dp)
            .graphicsLayer { scaleX = chipScale; scaleY = chipScale }
            .padding(3.dp)
            .clip(CircleShape)
            .background(color)
            .border(animatedBorder, ringColor, CircleShape)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun StrokeWidthSlider(viewModel: EditorViewModel) {
    val strokeWidth by viewModel.strokeWidth.collectAsState()
    val activeTool by viewModel.selectedTool.collectAsState()
    val range = if (activeTool == Tool.HIGHLIGHTER) 5f..40f else 1f..20f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
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
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 32.dp
) {
    val animatedTintColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        label = "EditorButtonTint"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isActive) 1.14f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "EditorIconScale"
    )
    val iconOffsetY by animateDpAsState(
        targetValue = if (isActive) (-1).dp else 0.dp,
        animationSpec = tween(180),
        label = "EditorIconOffsetY"
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(180),
        label = "EditorIconIndicatorAlpha"
    )

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick, modifier = Modifier.size(size)) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = animatedTintColor,
                modifier = Modifier
                    .offset(y = iconOffsetY)
                    .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 3.dp)
                .width(14.dp)
                .height(3.dp)
                .graphicsLayer { alpha = indicatorAlpha }
                .background(MaterialTheme.colorScheme.onPrimaryContainer, RoundedCornerShape(999.dp))
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

private suspend fun androidx.compose.foundation.lazy.LazyListState.animateScrollToCenter(index: Int) {
    scrollToItem(index)
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    val delta = (itemInfo.offset + itemInfo.size / 2 - viewportHeight / 2).toFloat()
    animateScrollBy(delta)
}


@androidx.compose.runtime.Composable
fun AiWebPanel(
    fileUri: android.net.Uri?,
    onClose: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp).fillMaxSize()
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                androidx.compose.material3.Text(
                    text = "AI Parser Preview",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
                androidx.compose.material3.IconButton(onClick = onClose) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
            androidx.compose.material3.HorizontalDivider()
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
            androidx.compose.material3.Text(
                text = if (fileUri != null) "File:\n" else "No file",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        }
    }
}
@Composable
private fun DocumentListRow(
    document: DocumentEntity,
    coverBitmap: Bitmap?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit = {},
    onFavoriteToggle: (Boolean) -> Unit = {}
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameInput by remember(document.displayName) { mutableStateOf(document.displayName) }
    val isDarkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val rowShellColor = if (isDarkSurface) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
    } else {
        Color.White
    }

    val dateStr = remember(document.lastOpenedAt) {
        java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(document.lastOpenedAt))
    }

    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(88.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = rowShellColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f / 1.414f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(Color.White)
            ) {
                if (coverBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = coverBitmap.asImageBitmap(),
                        contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "PDF",
                        tint = Color.LightGray,
                        modifier = Modifier.align(Alignment.Center).size(32.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { onFavoriteToggle(!document.isFavorite) }) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = if (document.isFavorite) Color.Yellow else Color.Gray,
                )
            }
            
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
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
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重新命名") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameInput.isNotBlank()) onRename(renameInput.trim())
                        showRenameDialog = false
                    }
                ) { Text("確認") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("取消") }
            }
        )
    }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("刪除文件") },
            text = { Text("確定要刪除「${document.displayName}」嗎？此操作無法還原。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("刪除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}
