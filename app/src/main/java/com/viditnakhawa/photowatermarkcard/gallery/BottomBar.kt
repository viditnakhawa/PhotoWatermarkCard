package com.viditnakhawa.photowatermarkcard.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun BottomBar(onChooseTemplate: () -> Unit, onAutomation: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        modifier = Modifier,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onChooseTemplate) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = "Choose Template",
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = onAutomation) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = "Automation",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewBottomBar() {
    MaterialTheme {
        BottomBar({}, {})
    }
}
