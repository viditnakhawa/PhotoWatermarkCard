package com.viditnakhawa.photowatermarkcard.gallery

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.Window
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(onNavigateBack: () -> Unit) {
    val viewModel: GalleryViewModel = viewModel()
    val timelineItems by viewModel.timelineItems.collectAsState()
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Load images when the screen is first composed
    LaunchedEffect(Unit) {
        viewModel.loadAutoFramedImages()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Framed Photos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (timelineItems.isEmpty()) {
            EmptyGalleryView(modifier = Modifier.padding(paddingValues))
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp
            ) {
                items(timelineItems, key = { it.hashCode() }) { item ->
                    when (item) {
                        is TimelineItem.HeaderItem -> {
                            Text(
                                text = item.date,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                            Spacer(modifier = Modifier.fillMaxWidth().height(0.dp))
                        }
                        is TimelineItem.ImageItem -> {
                            GalleryImage(
                                item = item.galleryItem,
                                onClick = { selectedImageUri = item.galleryItem.uri },
                                modifier = Modifier
                            )
                        }
                    }
                }
            }
        }
    }

    FullScreenImageViewer(
        imageUri = selectedImageUri,
        onDismiss = { selectedImageUri = null }
    )
}

@Composable
fun GalleryImage(item: GalleryItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val aspectRatio = if (item.height > 0) item.width.toFloat() / item.height.toFloat() else 1f

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(item.uri)
            .crossfade(true)
            .crossfade(400)
            .build(),
        contentDescription = "Framed Photo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(MaterialTheme.shapes.medium)
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

@Composable
fun FullScreenImageViewer(
    imageUri: android.net.Uri?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val window = getActivityWindow()
        DisposableEffect(imageUri, window) {
            if (imageUri != null && window != null) {
                window.colorMode = ActivityInfo.COLOR_MODE_HDR
                onDispose {
                    window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
                }
            } else {
                onDispose { }
            }
        }
    }


    AnimatedVisibility(
        visible = imageUri != null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(onClick = onDismiss)
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Full-screen Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                IconButton(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            type = "image/jpeg"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun getActivityWindow(): Window? {
    tailrec fun Context.findActivity(): Activity? =
        when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }
    return LocalContext.current.findActivity()?.window
}
