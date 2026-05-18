package com.nexus.iptv.ui.components.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Border
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.nexus.iptv.domain.repository.ProviderRepository
import com.nexus.iptv.ui.design.AppColors
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class NavBarExpirationViewModel @Inject constructor(
    providerRepository: ProviderRepository
) : ViewModel() {
    val expirationDate: StateFlow<Long?> = providerRepository.getActiveProvider()
        .map { it?.expirationDate?.takeIf { ts -> ts > 0L } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun NavBarExpirationPill(
    modifier: Modifier = Modifier,
    viewModel: NavBarExpirationViewModel = hiltViewModel()
) {
    val expirationDate by viewModel.expirationDate.collectAsStateWithLifecycle()
    val timestamp = expirationDate ?: return

    val now = System.currentTimeMillis()
    val daysRemaining = ((timestamp - now) / DAY_MS).toInt()
    val accent = when {
        daysRemaining < 0 -> Color(0xFFFF6B6B)
        daysRemaining <= 7 -> Color(0xFFFFB44A)
        else -> AppColors.TextSecondary
    }

    val dateLabel = remember(timestamp) { dateFormat.format(Date(timestamp)) }
    val text = when {
        daysRemaining < 0 -> "Expired $dateLabel"
        daysRemaining == 0 -> "Expires today"
        daysRemaining == 1 -> "Expires tomorrow"
        else -> "Expires $dateLabel"
    }

    Spacer(modifier = Modifier.width(24.dp))
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        colors = SurfaceDefaults.colors(containerColor = accent.copy(alpha = 0.14f)),
        border = Border(BorderStroke(1.dp, accent.copy(alpha = 0.42f)))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private const val DAY_MS = 24L * 60L * 60L * 1000L
private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
