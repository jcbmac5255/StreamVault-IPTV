package com.nexus.iptv.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nexus.iptv.R
import com.nexus.iptv.device.rememberIsTelevisionDevice
import com.nexus.iptv.domain.model.Announcement
import com.nexus.iptv.ui.components.rememberCrossfadeImageModel
import com.nexus.iptv.ui.design.AppColors
import com.nexus.iptv.ui.interaction.mouseClickable

@Composable
fun AnnouncementDialog(
    announcement: Announcement,
    onDismissForever: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val isTelevisionDevice = rememberIsTelevisionDevice()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogContent: @Composable (Modifier) -> Unit = { resolvedModifier ->
            Surface(
                modifier = resolvedModifier,
                shape = RoundedCornerShape(24.dp),
                colors = SurfaceDefaults.colors(containerColor = AppColors.SurfaceElevated)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    AppColors.Brand.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AppColors.TextPrimary
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 460.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!announcement.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = rememberCrossfadeImageModel(announcement.imageUrl),
                                contentDescription = announcement.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                        Text(
                            text = announcement.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.TextSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        Button(
                            onClick = onDismissForever,
                            modifier = Modifier.mouseClickable(onClick = onDismissForever),
                            colors = ButtonDefaults.colors(
                                containerColor = AppColors.Surface,
                                contentColor = AppColors.TextPrimary
                            )
                        ) {
                            Text(stringResource(R.string.announcement_dialog_dismiss))
                        }
                        Button(
                            onClick = onDismissRequest,
                            modifier = Modifier.mouseClickable(onClick = onDismissRequest),
                            colors = ButtonDefaults.colors(
                                containerColor = AppColors.Brand,
                                contentColor = Color.White
                            )
                        ) {
                            Text(stringResource(R.string.announcement_dialog_close))
                        }
                    }
                }
            }
        }

        if (isTelevisionDevice) {
            dialogContent(Modifier.fillMaxWidth(0.55f))
        } else {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val dialogModifier = when {
                    maxWidth < 700.dp -> Modifier.fillMaxWidth(0.9f)
                    maxWidth < 1280.dp -> Modifier.fillMaxWidth(0.72f)
                    else -> Modifier.fillMaxWidth(0.55f)
                }
                dialogContent(dialogModifier)
            }
        }
    }
}
