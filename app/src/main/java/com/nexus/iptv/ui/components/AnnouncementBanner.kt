package com.nexus.iptv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nexus.iptv.R
import com.nexus.iptv.domain.model.Announcement
import com.nexus.iptv.ui.design.AppColors
import com.nexus.iptv.ui.interaction.TvClickableSurface

@Composable
fun AnnouncementBannerRow(
    announcements: List<Announcement>,
    onAnnouncementClick: (Announcement) -> Unit,
    onDismissAnnouncement: (Announcement) -> Unit
) {
    if (announcements.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.dashboard_announcements_title),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary
            )
        }

        if (announcements.size == 1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                AnnouncementCard(
                    announcement = announcements.first(),
                    fillWidth = true,
                    onClick = { onAnnouncementClick(announcements.first()) },
                    onDismiss = { onDismissAnnouncement(announcements.first()) }
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(announcements, key = { it.id }) { announcement ->
                    AnnouncementCard(
                        announcement = announcement,
                        fillWidth = false,
                        onClick = { onAnnouncementClick(announcement) },
                        onDismiss = { onDismissAnnouncement(announcement) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnouncementCard(
    announcement: Announcement,
    fillWidth: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val widthModifier = if (fillWidth) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.width(520.dp)
    }
    Row(
        modifier = widthModifier.height(78.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TvClickableSurface(
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = AppColors.SurfaceElevated,
                focusedContainerColor = AppColors.SurfaceEmphasis
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            border = ClickableSurfaceDefaults.border(
                border = Border(
                    border = BorderStroke(1.dp, AppColors.Brand.copy(alpha = 0.32f)),
                    shape = RoundedCornerShape(16.dp)
                ),
                focusedBorder = Border(
                    border = BorderStroke(2.dp, AppColors.Focus),
                    shape = RoundedCornerShape(16.dp)
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = announcement.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        DismissPillButton(onClick = onDismiss)
    }
}

@Composable
private fun DismissPillButton(onClick: () -> Unit) {
    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(999.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AppColors.Surface,
            focusedContainerColor = AppColors.SurfaceEmphasis
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, AppColors.TextTertiary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(999.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, AppColors.Focus),
                shape = RoundedCornerShape(999.dp)
            )
        )
    ) {
        Text(
            text = stringResource(R.string.announcement_dialog_dismiss),
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}
