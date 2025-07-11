package com.viditnakhawa.photowatermarkcard.gallery

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.Window
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun FullScreenImageViewer(
    imageUri: Uri?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (Uri) -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }
    val window = getActivityWindow()
    DisposableEffect(imageUri, window) {
        if (imageUri == null || window == null) {
            onDispose {}
        } else {
            val originalColorMode = window.attributes?.colorMode ?: ActivityInfo.COLOR_MODE_DEFAULT
            window.colorMode = ActivityInfo.COLOR_MODE_HDR
            onDispose {
                window.colorMode = originalColorMode
            }
        }
    }

    AnimatedVisibility(
        visible = imageUri != null,
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(150))
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var showDeleteDialog by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
            ) {
                if (imageUri != null) {
                    ZoomableAsyncImage(
                        imageUri = imageUri,
                        modifier = Modifier.fillMaxSize(),
                        onTap = { controlsVisible = !controlsVisible }
                    )
                }

                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
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

                        // New Bottom Action Bar
                        ViewerBottomBar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp),
                            onShare = {
                                val context = it
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_STREAM, imageUri)
                                    type = "image/jpeg"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                            },
                            onEdit = {
                                // TODO: Implement edit functionality
                                val context = it
                                Toast.makeText(context, "Edit coming soon!", Toast.LENGTH_SHORT).show()
                                if (imageUri != null) {
                                    onEdit(imageUri)
                                }
                            },
                            onDelete = { showDeleteDialog = true }
                        )
                    }
                }
            }

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
    }
}

@Composable
fun ZoomableAsyncImage(
    imageUri: Uri,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offset += pan
                    } else {
                        offset = Offset.Zero
                    }
                }
            }
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Full-screen Zoomable Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
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
    onEdit: (Context) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 42.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(icon = Icons.Outlined.Share, text = "Share") { onShare(context) }
            ActionButton(icon = Icons.Outlined.Edit, text = "Edit") { onEdit(context) }
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
