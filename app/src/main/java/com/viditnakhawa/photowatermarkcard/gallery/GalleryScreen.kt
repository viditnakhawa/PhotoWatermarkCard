package com.viditnakhawa.photowatermarkcard.gallery

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Grid3x3
import androidx.compose.material.icons.outlined.Grid4x4
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.IconButton
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest

// Sealed class to represent the different possible layouts for the gallery
sealed class LayoutMode {
    data object Staggered : LayoutMode()
    data class Uniform(val columns: Int) : LayoutMode()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(onNavigateToAutomation: () -> Unit, onNavigateToTemplates: () -> Unit) {
    val viewModel: GalleryViewModel = viewModel()
    val timelineItems by viewModel.timelineItems.collectAsState()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSaveWarningDialog by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("AutoFramePrefs", Context.MODE_PRIVATE) }
    val backgroundServiceRunning = remember { mutableStateOf(prefs.getBoolean("service_enabled", false)) }

    var layoutMode by remember { mutableStateOf<LayoutMode>(LayoutMode.Staggered) }
    var bottomBarVisible by remember { mutableStateOf(true) }

    val lazyGridState = rememberLazyGridState()
    val lazyStaggeredGridState = rememberLazyStaggeredGridState()

    val isScrollingDown = remember {
        var previousOffset = 0
        var previousIndex = 0
        derivedStateOf {
            val currentFirstVisibleIndex = when (layoutMode) {
                is LayoutMode.Staggered -> lazyStaggeredGridState.firstVisibleItemIndex
                is LayoutMode.Uniform -> lazyGridState.firstVisibleItemIndex
            }
            val currentFirstVisibleOffset = when (layoutMode) {
                is LayoutMode.Staggered -> lazyStaggeredGridState.firstVisibleItemScrollOffset
                is LayoutMode.Uniform -> lazyGridState.firstVisibleItemScrollOffset
            }

            val isScrollingDown = if (previousIndex == currentFirstVisibleIndex) {
                currentFirstVisibleOffset > previousOffset
            } else {
                currentFirstVisibleIndex > previousIndex
            }
            previousOffset = currentFirstVisibleOffset
            previousIndex = currentFirstVisibleIndex
            isScrollingDown
        }
    }

    LaunchedEffect(isScrollingDown.value) {
        bottomBarVisible = !isScrollingDown.value
    }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            showSaveWarningDialog = it
        }
    }
    LaunchedEffect(Unit) {
        viewModel.loadAutoFramedImages()
    }

    Scaffold(
        topBar = {
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
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (timelineItems.isEmpty()) {
                EmptyGalleryView(modifier = Modifier.fillMaxSize())
            } else {
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
                                        onClick = { selectedImageUri = item.galleryItem.uri }
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
                                        onClick = { selectedImageUri = item.galleryItem.uri }
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
                        onChooseTemplate = onNavigateToTemplates,
                        onAutomation = onNavigateToAutomation,
                        isServiceRunning = backgroundServiceRunning
                    )
                    FloatingActionButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(28.dp),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add Image")
                    }
                }
            }
        }
    }

    showSaveWarningDialog?.let { uri ->
        SaveWarningDialog(
            onConfirm = {
                // TODO: Navigate to the framing/automation screen with the selected URI
                Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSaveWarningDialog = null }
        )
    }

    FullScreenImageViewer(
        imageUri = selectedImageUri,
        onDismiss = { selectedImageUri = null },
        onDelete = {
            selectedImageUri?.let { viewModel.deleteImage(it) }
            selectedImageUri = null
        },
        onEdit = { /* TODO */ }
    )
}

@Composable
private fun LayoutSwitcher(
    currentLayout: LayoutMode,
    onLayoutChange: (LayoutMode) -> Unit
) {
    val (nextLayout, icon, description) = when (currentLayout) {
        is LayoutMode.Staggered -> {
            // Currently Staggered, next is 3x3
            Triple(LayoutMode.Uniform(3), Icons.Outlined.GridView, "Switch to 3x3 Grid")
        }
        is LayoutMode.Uniform -> when (currentLayout.columns) {
            3 -> {
                // Currently 3x3, next is 4x4
                Triple(LayoutMode.Uniform(4), Icons.Outlined.Grid3x3, "Switch to 4x4 Grid")
            }
            else -> { // Assumes 4x4 or other uniform grids
                // Currently 4x4, next is Staggered
                Triple(LayoutMode.Staggered, Icons.Outlined.Grid4x4, "Switch to Staggered Grid")
            }
        }
    }

    IconButton(onClick = { onLayoutChange(nextLayout) }) {
        Icon(
            imageVector = icon,
            contentDescription = description
        )
    }
}

@Composable
fun SaveWarningDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Framing") },
        text = { Text("Images you frame manually will not be saved to your gallery by default. You will need to save it yourself after framing.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
fun GalleryImage(item: GalleryItem, isStaggered: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val aspectRatio = if (isStaggered && item.height > 0) {
        item.width.toFloat() / item.height.toFloat()
    } else {
        1f
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(item.uri)
            .crossfade(true)
            .build(),
        contentDescription = "Framed Photo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clickable(onClick = onClick)
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
