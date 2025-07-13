package com.viditnakhawa.photowatermarkcard.gallery

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.Window
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import android.util.Log
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import kotlinx.coroutines.coroutineScope

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageViewer(
    imageItems: List<TimelineItem.ImageItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onDelete: (Uri) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { imageItems.size }
    )
    var controlsVisible by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf<Uri?>(null) }
    val window = getActivityWindow()
    val zoomStates = remember { mutableStateMapOf<Int, Boolean>() }

    val isCurrentZoomed by remember {
        derivedStateOf {
            zoomStates[pagerState.currentPage] ?: false
        }
    }

    DisposableEffect(window) {
        val originalColorMode = window?.attributes?.colorMode ?: ActivityInfo.COLOR_MODE_DEFAULT
        window?.colorMode = ActivityInfo.COLOR_MODE_HDR
        onDispose {
            window?.colorMode = originalColorMode
        }
    }

    Dialog(
        onDismissRequest = {
            zoomStates.clear()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !isCurrentZoomed
            ) { pageIndex ->

                if (zoomStates[pageIndex] == null) {
                    zoomStates[pageIndex] = false
                }

                ZoomableAsyncImage(
                    imageUri = imageItems[pageIndex].galleryItem.uri,
                    modifier = Modifier.fillMaxSize(),
                    onTap = { controlsVisible = !controlsVisible },
                    onZoomStateChanged = { zoomed -> zoomStates[pageIndex] = zoomed }
                )
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(150))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                    }

                    ViewerBottomBar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        onShare = { context ->
                            val currentUri = imageItems[pagerState.currentPage].galleryItem.uri
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM, currentUri)
                                type = "image/jpeg"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                        },
                        onDelete = {
                            showDeleteDialog = imageItems[pagerState.currentPage].galleryItem.uri
                        }
                    )
                }
            }
        }

        showDeleteDialog?.let { uriToDelete ->
            DeleteConfirmationDialog(
                onConfirm = {
                    onDelete(uriToDelete)
                    showDeleteDialog = null
                },
                onDismiss = { showDeleteDialog = null }
            )
        }
    }
}

@Composable
fun ZoomableAsyncImage(
    imageUri: Uri,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onZoomStateChanged: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val TAG = "ZoomableImage"

    LaunchedEffect(imageUri) {
        scale = 1f
        offset = Offset.Zero
        onZoomStateChanged(false)
    }

    BoxWithConstraints(modifier = modifier) {
        val constraints = this@BoxWithConstraints.constraints

        AsyncImage(
            model = imageUri,
            contentDescription = "Full-screen Zoomable Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    coroutineScope {
                        detectTapGestures(onTap = { onTap() })
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()
                            var wasZooming = false
                            var wasPanning = false

                            do {
                                val event = awaitPointerEvent()
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()

                                if (zoom != 1f) {
                                    wasZooming = true
                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                                    Log.d(TAG, "Zoom detected: newScale = $newScale")
                                    scale = newScale
                                    onZoomStateChanged(scale > 1f)
                                }

                                if (pan != Offset.Zero) {
                                    if (scale > 1f) {
                                        wasPanning = true
                                        Log.d(TAG, "Pan detected: CONSUMING PAN because scale is $scale")
                                        val maxPanX = (constraints.maxWidth * (scale - 1)) / 2f
                                        val maxPanY = (constraints.maxHeight * (scale - 1)) / 2f
                                        val newOffsetX = (offset.x + pan.x).coerceIn(-maxPanX, maxPanX)
                                        val newOffsetY = (offset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                        offset = Offset(newOffsetX, newOffsetY)
                                    } else {
                                        Log.d(TAG, "Pan detected: IGNORING PAN because scale is 1f. Pager should swipe.")
                                    }
                                }

                                if (wasZooming || wasPanning) {
                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })

                            if (scale == 1f) {
                                offset = Offset.Zero
                            }
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

@Composable
fun ViewerBottomBar(
    modifier: Modifier = Modifier,
    onShare: (Context) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 40.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(66.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(icon = Icons.Outlined.Share, text = "Share") { onShare(context) }
            ActionButton(icon = Icons.Outlined.Delete, text = "Delete") { onDelete() }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = text)
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Image?") },
        text = { Text("This action is permanent and cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
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
fun getActivityWindow(): Window? {
    tailrec fun Context.findActivity(): Activity? =
        when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }
    return LocalContext.current.findActivity()?.window
}

//@Preview(showBackground = true)
//@Composable
//private fun FullScreenImageViewerPreview() {
//    PhotoWatermarkCardTheme {
//        val sampleUri = Uri.parse("https://picsum.photos/seed/picsum/1080/1920")
//
//        FullScreenImageViewer(
//            imageUri = sampleUri,
//            onDismiss = {},
//            onDelete = {},
//        )
//    }
//}

//@Preview(showBackground = true, backgroundColor = 0xFF000000)
//@Composable
//private fun ViewerBottomBarPreview() {
//    PhotoWatermarkCardTheme {
//        Box(modifier = Modifier.padding(16.dp)) {
//            ViewerBottomBar(
//                onShare = {},
//                onDelete = {}
//            )
//        }
//    }
//}

//@Preview
//@Composable
//private fun DeleteConfirmationDialogPreview() {
//    PhotoWatermarkCardTheme {
//        DeleteConfirmationDialog(
//            onConfirm = {},
//            onDismiss = {}
//        )
//    }
//}
