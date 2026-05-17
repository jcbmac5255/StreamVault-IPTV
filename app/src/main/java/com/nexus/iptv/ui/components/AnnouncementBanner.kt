package com.nexus.iptv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nexus.iptv.R
import com.nexus.iptv.domain.model.Announcement
import com.nexus.iptv.ui.design.AppColors
import com.nexus.iptv.ui.interaction.TvClickableSurface

@Composable
fun AnnouncementBannerRow(
    announcements: List<Announcement>,
    onAnnouncementClick: (Announcement) -> Unit
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
                    onClick = { onAnnouncementClick(announcements.first()) }
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
                        onClick = { onAnnouncementClick(announcement) }
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
    onClick: () -> Unit
) {
    val widthModifier = if (fillWidth) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.width(520.dp)
    }
    TvClickableSurface(
        onClick = onClick,
        modifier = widthModifier.height(120.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AppColors.SurfaceElevated,
            focusedContainerColor = AppColors.SurfaceEmphasis
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, AppColors.Brand.copy(alpha = 0.32f)),
                shape = RoundedCornerShape(20.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, AppColors.Focus),
                shape = RoundedCornerShape(20.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnnouncementCardArt(announcement.imageUrl)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = announcement.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AnnouncementCardArt(imageUrl: String?) {
    val art = Modifier
        .size(width = 120.dp, height = 120.dp)
        .padding(start = 10.dp, top = 10.dp, bottom = 10.dp)
        .clip(RoundedCornerShape(14.dp))

    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = rememberCrossfadeImageModel(imageUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = art
        )
    } else {
        Box(
            modifier = art.background(
                Brush.linearGradient(
                    listOf(
                        AppColors.Brand.copy(alpha = 0.35f),
                        AppColors.Brand.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                )
            )
        )
    }
}
