package com.nexus.iptv.domain.repository

import com.nexus.iptv.domain.model.Announcement
import com.nexus.iptv.domain.model.Result

interface AnnouncementRepository {
    suspend fun fetchAnnouncements(): Result<List<Announcement>>
}
