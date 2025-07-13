package com.viditnakhawa.photowatermarkcard.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Grid3x3
import androidx.compose.material.icons.outlined.Grid4x4
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.viditnakhawa.photowatermarkcard.templates.TemplateRepository
import com.viditnakhawa.photowatermarkcard.utils.FrameUtils
import kotlinx.coroutines.launch
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager

// Sealed class to represent the different possible layouts for the gallery
sealed class LayoutMode {
    data object Staggered : LayoutMode()
    data class Uniform(val columns: Int) : LayoutMode()
}

private data class ViewerState(
    val imageItems: List<TimelineItem.ImageItem>,
    val initialIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    onNavigateToAutomation: () -> Unit,
    onNavigateToTemplates: (imageUri: Uri?) -> Unit
) {
    val viewModel: GalleryViewModel = viewModel()
    val timelineItems by viewModel.timelineItems.collectAsState()
    var viewerState by remember { mutableStateOf<ViewerState?>(null) }
    var imageToFrameManuallyUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("AutoFramePrefs", Context.MODE_PRIVATE) }
    val backgroundServiceRunning = remember { mutableStateOf(prefs.getBoolean("service_enabled", false)) }

    var layoutMode by remember { mutableStateOf<LayoutMode>(LayoutMode.Staggered) }
    var bottomBarVisible by remember { mutableStateOf(true) }

    var isProcessingManual by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var inSelectionMode by remember { mutableStateOf(false) }
    var selectedUris by remember { mutableStateOf(setOf<Uri>()) }

    val lazyGridState = rememberLazyGridState()
    val lazyStaggeredGridState = rememberLazyStaggeredGridState()

    var previousOffset by remember { mutableIntStateOf(0) }
    var previousIndex by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadAutoFramedImages()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(context, viewModel) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                viewModel.loadAutoFramedImages()
            }
        }
        val filter = IntentFilter("com.viditnakhawa.photowatermarkcard.ACTION_IMAGE_FRAMED")
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)

        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
    }

    val isScrollingDown = remember(lazyGridState, lazyStaggeredGridState) {
        derivedStateOf {
            val currentFirstVisibleIndex = when (layoutMode) {
                is LayoutMode.Staggered -> lazyStaggeredGridState.firstVisibleItemIndex
                is LayoutMode.Uniform -> lazyGridState.firstVisibleItemIndex
            }
            val currentFirstVisibleOffset = when (layoutMode) {
                is LayoutMode.Staggered -> lazyStaggeredGridState.firstVisibleItemScrollOffset
                is LayoutMode.Uniform -> lazyGridState.firstVisibleItemScrollOffset
            }

            val isDown = if (previousIndex == currentFirstVisibleIndex) {
                currentFirstVisibleOffset > previousOffset
            } else {
                currentFirstVisibleIndex > previousIndex
            }

            previousOffset = currentFirstVisibleOffset
            previousIndex = currentFirstVisibleIndex
            isDown
        }
    }

    LaunchedEffect(isScrollingDown.value) {
        if (!inSelectionMode) {
            bottomBarVisible = !isScrollingDown.value
        }
    }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageToFrameManuallyUri = it
        }
    }

    Scaffold(
        topBar = {
            if (inSelectionMode) {
                SelectionTopAppBar(
                    selectedItemCount = selectedUris.size,
                    onClose = {
                        inSelectionMode = false
                        selectedUris = emptySet()
                    },
                    onDelete = {
                        viewModel.deleteImages(selectedUris)
                        inSelectionMode = false
                        selectedUris = emptySet()
                    },
                    onShare = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND_MULTIPLE
                            type = "image/jpeg"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(selectedUris))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Images"))
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            "AutoFrame",
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize * 1.05f
                        )
                    },
                    actions = {
                        LayoutSwitcher(
                            currentLayout = layoutMode,
                            onLayoutChange = { newLayout -> layoutMode = newLayout }
                        )
                    }
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (timelineItems.isEmpty() && !isProcessingManual) {
                EmptyGalleryView(modifier = Modifier.fillMaxSize())
            } else {
                val imageOnlyItems = remember(timelineItems) {
                    timelineItems.filterIsInstance<TimelineItem.ImageItem>()
                }
                when (val mode = layoutMode) {
                    is LayoutMode.Staggered -> {
                        LazyVerticalStaggeredGrid(
                            state = lazyStaggeredGridState,
                            columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalItemSpacing = 8.dp
                        ) {
                            items(
                                count = timelineItems.size,
                                key = { timelineItems[it].hashCode() },
                                span = { index ->
                                    if (timelineItems[index] is TimelineItem.HeaderItem) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane
                                }
                            ) { index ->
                                val item = timelineItems[index]
                                when (item) {
                                    is TimelineItem.HeaderItem -> HeaderText(item.date)
                                    is TimelineItem.ImageItem -> GalleryImage(
                                        item = item.galleryItem,
                                        isStaggered = true,
                                        isSelected = selectedUris.contains(item.galleryItem.uri),
                                        onClick = {
                                            if (inSelectionMode) {
                                                selectedUris = if (selectedUris.contains(item.galleryItem.uri)) {
                                                    selectedUris - item.galleryItem.uri
                                                } else {
                                                    selectedUris + item.galleryItem.uri
                                                }
                                                // If no items are selected, exit selection mode
                                                if (selectedUris.isEmpty()) inSelectionMode = false
                                            } else {
                                                val itemIndex = imageOnlyItems.indexOf(item)
                                                if (itemIndex != -1) {
                                                    viewerState = ViewerState(imageOnlyItems, itemIndex)
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            if (!inSelectionMode) {
                                                inSelectionMode = true
                                                selectedUris = selectedUris + item.galleryItem.uri
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    is LayoutMode.Uniform -> {
                        LazyVerticalGrid(
                            state = lazyGridState,
                            columns = GridCells.Fixed(mode.columns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                count = timelineItems.size,
                                key = { timelineItems[it].hashCode() },
                                span = { index ->
                                    if (timelineItems[index] is TimelineItem.HeaderItem) GridItemSpan(mode.columns) else GridItemSpan(1)
                                }
                            ) { index ->
                                val item = timelineItems[index]
                                when (item) {
                                    is TimelineItem.HeaderItem -> HeaderText(item.date)
                                    is TimelineItem.ImageItem -> GalleryImage(
                                        item = item.galleryItem,
                                        isStaggered = false,
                                        isSelected = selectedUris.contains(item.galleryItem.uri),
                                        onClick = {
                                            if (inSelectionMode) {
                                                selectedUris = if (selectedUris.contains(item.galleryItem.uri)) {
                                                    selectedUris - item.galleryItem.uri
                                                } else {
                                                    selectedUris + item.galleryItem.uri
                                                }
                                                if (selectedUris.isEmpty()) inSelectionMode = false
                                            } else {
                                                val itemIndex = imageOnlyItems.indexOf(item)
                                                if (itemIndex != -1) {
                                                    viewerState = ViewerState(imageOnlyItems, itemIndex)
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            if (!inSelectionMode) {
                                                inSelectionMode = true
                                                selectedUris = selectedUris + item.galleryItem.uri
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = bottomBarVisible,
                enter = slideInVertically { it },
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 150),
                    targetOffsetY = { it }
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomBar(
                        onChooseTemplate = { onNavigateToTemplates(null) },
                        onAutomation = onNavigateToAutomation,
                        isServiceRunning = backgroundServiceRunning
                    )
                    FloatingActionButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(70.dp),
                        shape = RoundedCornerShape(28.dp),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add Image")
                    }
                }
            }

            AnimatedVisibility(
                visible = isProcessingManual,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false, onClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    imageToFrameManuallyUri?.let { uri ->
        ManualFrameDialog(
            onDismiss = { imageToFrameManuallyUri = null },
            onTemplateSelected = { templateId ->
                imageToFrameManuallyUri = null
                scope.launch {
                    isProcessingManual = true
                    // Call FrameUtils with the specific template ID
                    val success = FrameUtils.processImage(context, uri, templateId)
                    if (!success) {
                        Toast.makeText(context, "Failed to frame image.", Toast.LENGTH_SHORT).show()
                    }
                    isProcessingManual = false
                }
            }
        )
    }

    viewerState?.let { state ->
        FullScreenImageViewer(
            imageItems = state.imageItems,
            initialIndex = state.initialIndex,
            onDismiss = { viewerState = null },
            onDelete = { imageUri ->
                viewModel.deleteImage(imageUri)
                viewerState = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopAppBar(
    selectedItemCount: Int,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("$selectedItemCount selected") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Close selection")
            }
        },
        actions = {
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, contentDescription = "Share selected")
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete selected")
            }
        }
    )

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun LayoutSwitcher(
    currentLayout: LayoutMode,
    onLayoutChange: (LayoutMode) -> Unit
) {
    val (nextLayout, icon, description) = when (currentLayout) {
        is LayoutMode.Staggered -> {
            Triple(LayoutMode.Uniform(3), Icons.Outlined.GridView, "Switch to 3x3 Grid")
        }
        is LayoutMode.Uniform -> when (currentLayout.columns) {
            3 -> Triple(LayoutMode.Uniform(4), Icons.Outlined.Grid3x3, "Switch to 4x4 Grid")
            else -> Triple(LayoutMode.Staggered, Icons.Outlined.Grid4x4, "Switch to Staggered Grid")
        }
    }

    IconButton(onClick = { onLayoutChange(nextLayout) }) {
        Icon(imageVector = icon, contentDescription = description)
    }
}

@Composable
fun ManualFrameDialog(
    onDismiss: () -> Unit,
    onTemplateSelected: (templateId: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a Template") },
        text = {
            LazyColumn {
                items(TemplateRepository.templates) { template ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTemplateSelected(template.id) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(template.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryImage(
    item: GalleryItem,
    isStaggered: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val aspectRatio = if (isStaggered && item.height > 0) {
        item.width.toFloat() / item.height.toFloat()
    } else {
        1f
    }

    val context = LocalContext.current

    val imageRequest = remember(item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = "Framed Photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                )
            }
        }
    }
}

@Composable
fun HeaderText(date: String) {
    Text(
        text = date,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
fun EmptyGalleryView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Your Gallery is Empty",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Photos you frame will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
