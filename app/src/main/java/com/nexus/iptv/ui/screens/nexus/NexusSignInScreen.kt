package com.nexus.iptv.ui.screens.nexus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.nexus.iptv.R
import com.nexus.iptv.branding.NexusBranding
import com.nexus.iptv.ui.components.shell.StatusPill
import com.nexus.iptv.ui.design.AppColors
import com.nexus.iptv.ui.interaction.TvButton
import com.nexus.iptv.ui.theme.ErrorColor
import com.nexus.iptv.data.local.dao.ProgramDao
import com.nexus.iptv.data.local.dao.XtreamIndexJobDao
import com.nexus.iptv.data.sync.SyncProgressBus
import com.nexus.iptv.domain.sync.Section
import com.nexus.iptv.domain.sync.SyncProgress
import com.nexus.iptv.domain.model.ProviderXtreamLiveSyncMode
import com.nexus.iptv.domain.usecase.ValidateAndAddProvider
import com.nexus.iptv.domain.usecase.ValidateAndAddProviderResult
import com.nexus.iptv.domain.usecase.XtreamProviderSetupCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class NexusSignInUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val signInSuccess: Boolean = false,
    // While deepIndexPhase != null, the screen overrides the SyncProgressBus
    // phase with a synthesized SyncProgress sourced from the active section's
    // xtream_index_jobs row, keeping the progress bar live as WorkManager
    // finishes deep-indexing movies/series after foreground sync returns.
    val deepIndexPhase: Section? = null,
    // Per-item indexed row count from the active section's job (smooth).
    val deepIndexedRows: Int = 0,
    // Categories progress — fixed denominator known upfront from the shell
    // fetch. Drives both the progress bar fill and the "X / Y categories"
    // line; coarse but truthful (no extrapolation).
    val deepIndexCompletedCategories: Int = 0,
    val deepIndexTotalCategories: Int = 0
)

@HiltViewModel
class NexusSignInViewModel @Inject constructor(
    private val validateAndAddProvider: ValidateAndAddProvider,
    private val xtreamIndexJobDao: XtreamIndexJobDao,
    private val programDao: ProgramDao,
    syncProgressBus: SyncProgressBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(NexusSignInUiState())
    val uiState: StateFlow<NexusSignInUiState> = _uiState.asStateFlow()

    // Bus stream with a LIVE→VOD transition interceptor: when the data layer
    // moves on from Section.LIVE to a different section, this transform
    // synthesizes a "LIVE at 100%" frame and holds it briefly so the bar
    // visually reaches the right edge before transitioning, matching the
    // per-phase UX used for MOVIES/SERIES in the deep-index path.
    private val transformedBusFlow = syncProgressBus.flow
        .scan(Pair<SyncProgress?, SyncProgress?>(null, null)) { acc, current -> Pair(acc.second, current) }
        .transform { (previous, current) ->
            if (previous?.section == Section.LIVE && current != null && current.section != Section.LIVE) {
                emit(
                    SyncProgress(
                        section = Section.LIVE,
                        current = previous.itemsIndexed,
                        total = previous.itemsIndexed,
                        currentLabel = "",
                        itemsIndexed = previous.itemsIndexed
                    )
                )
                delay(LIVE_COMPLETE_HOLD_MS)
            }
            emit(current)
        }

    val syncProgress: StateFlow<SyncProgress?> =
        combine(transformedBusFlow, _uiState) { progress, state ->
            when {
                !state.isLoading -> null
                state.deepIndexPhase != null -> SyncProgress(
                    section = state.deepIndexPhase,
                    // Bar fills current/total = completed/total categories. The
                    // item count rides along separately in itemsIndexed; the
                    // screen renders an extra "X / Y categories" line below.
                    current = state.deepIndexCompletedCategories,
                    total = state.deepIndexTotalCategories,
                    currentLabel = "",
                    itemsIndexed = state.deepIndexedRows
                )
                else -> progress
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun signIn(username: String, password: String, blankErrorMessage: String, genericErrorMessage: String) {
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()
        if (trimmedUser.isBlank() || trimmedPass.isBlank()) {
            _uiState.update { it.copy(errorMessage = blankErrorMessage) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = validateAndAddProvider.loginXtream(
                XtreamProviderSetupCommand(
                    serverUrl = NexusBranding.SERVER_URL,
                    username = trimmedUser,
                    password = trimmedPass,
                    name = NexusBranding.PROVIDER_NAME,
                    xtreamFastSyncEnabled = true,
                    // The Nexus panel rate-limits per-category requests (HTTP 429
                    // around 40+ categories in). STREAM_ALL fetches every live
                    // channel in a single request so we never trip the limit and
                    // VOD/Series requests aren't blocked by the cooldown.
                    xtreamLiveSyncMode = ProviderXtreamLiveSyncMode.STREAM_ALL
                )
            )

            when (result) {
                is ValidateAndAddProviderResult.Success ->
                    completeAfterDeepIndex(result.provider.id)
                is ValidateAndAddProviderResult.SavedWithWarning ->
                    completeAfterDeepIndex(result.provider.id)
                is ValidateAndAddProviderResult.ValidationError ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is ValidateAndAddProviderResult.Error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = genericErrorMessage) }
            }
        }
    }

    /**
     * Hold the loading panel until the MOVIE and SERIES xtream_index_jobs rows
     * reach a terminal state. The displayed counter and bar are driven by the
     * job's per-item indexedRows count over a fixed total sourced from
     * MovieDao/SeriesDao (which is populated during the foreground catalog
     * sync, so the total is known the moment we start observing here).
     */
    private suspend fun completeAfterDeepIndex(providerId: Long) {
        val deepIndexFlow = xtreamIndexJobDao.observeForProvider(providerId)

        // Phase 1: MOVIES. Bar tracks the movie job's own progress (0..100%).
        awaitSectionDone(deepIndexFlow, SECTION_MOVIE, Section.VOD)
        pinPhaseToHundredPercent(SECTION_MOVIE, Section.VOD, deepIndexFlow)
        delay(PHASE_COMPLETE_HOLD_MS)

        // Phase 2: SERIES. Bar resets and tracks series progress (0..100%).
        awaitSectionDone(deepIndexFlow, SECTION_SERIES, Section.SERIES)
        pinPhaseToHundredPercent(SECTION_SERIES, Section.SERIES, deepIndexFlow)
        delay(PHASE_COMPLETE_HOLD_MS)

        // Phase 3: EPG. The EPG job row only writes indexedRows at SUCCESS,
        // so we observe programDao.observeCountByProvider for real-time
        // progress while waiting on the BackgroundEpgSyncWorker. The bar is
        // driven by an asymptotic estimate (current/total grows with the
        // count but never quite hits 100% until the job goes terminal, at
        // which point we pin to the final count).
        awaitEpgDone(providerId, deepIndexFlow)
        pinEpgToFinal(providerId, deepIndexFlow)
        delay(PHASE_COMPLETE_HOLD_MS)

        _uiState.update {
            it.copy(
                isLoading = false,
                signInSuccess = true,
                deepIndexPhase = null,
                errorMessage = null
            )
        }
    }

    private suspend fun awaitSectionDone(
        flow: kotlinx.coroutines.flow.Flow<List<com.nexus.iptv.data.local.entity.XtreamIndexJobEntity>>,
        section: String,
        phaseSection: Section
    ) {
        withTimeoutOrNull(DEEP_INDEX_TIMEOUT_MS) {
            flow
                .onEach { jobs ->
                    val job = jobs.firstOrNull { it.section.equals(section, ignoreCase = true) }
                    _uiState.update {
                        it.copy(
                            deepIndexPhase = phaseSection,
                            deepIndexedRows = job?.indexedRows ?: 0,
                            deepIndexCompletedCategories = job?.completedCategories ?: 0,
                            deepIndexTotalCategories = job?.totalCategories ?: 0
                        )
                    }
                }
                .first { jobs ->
                    val job = jobs.firstOrNull { it.section.equals(section, ignoreCase = true) }
                    job == null || job.state.uppercase() in TERMINAL_STATES
                }
        }
    }

    private suspend fun pinPhaseToHundredPercent(
        section: String,
        phaseSection: Section,
        flow: kotlinx.coroutines.flow.Flow<List<com.nexus.iptv.data.local.entity.XtreamIndexJobEntity>>
    ) {
        val jobs = flow.first()
        val job = jobs.firstOrNull { it.section.equals(section, ignoreCase = true) }
        val total = job?.totalCategories ?: _uiState.value.deepIndexTotalCategories
        _uiState.update {
            it.copy(
                deepIndexPhase = phaseSection,
                deepIndexedRows = job?.indexedRows ?: it.deepIndexedRows,
                deepIndexCompletedCategories = total,
                deepIndexTotalCategories = total
            )
        }
    }

    private suspend fun awaitEpgDone(
        providerId: Long,
        deepIndexFlow: kotlinx.coroutines.flow.Flow<List<com.nexus.iptv.data.local.entity.XtreamIndexJobEntity>>
    ) {
        // EPG sync downloads & parses XMLTV before the first DB flush, so the
        // program count stays at 0 for ~20-30 seconds and then jumps. Layer a
        // time-based asymptote over the count-based asymptote and take the
        // max — bar moves immediately from elapsed time, then accelerates
        // once real programs start arriving.
        val phaseStartedAt = System.currentTimeMillis()
        // Drive a periodic tick so the time-based progress can advance even
        // when the dao flows aren't emitting.
        val tickFlow = flow {
            while (true) {
                emit(Unit)
                delay(500)
            }
        }
        withTimeoutOrNull(DEEP_INDEX_TIMEOUT_MS) {
            combine(
                deepIndexFlow,
                programDao.observeCountByProvider(providerId),
                tickFlow
            ) { jobs, programCount, _ -> Pair(jobs, programCount) }
                .onEach { (jobs, programCount) ->
                    val elapsedSec = (System.currentTimeMillis() - phaseStartedAt) / 1000.0
                    val timeFraction = elapsedSec / (elapsedSec + 30.0)
                    val countOffset = (programCount / 4).coerceAtLeast(2000)
                    val countFraction = if (programCount > 0) {
                        programCount.toDouble() / (programCount + countOffset)
                    } else 0.0
                    val effective = maxOf(timeFraction, countFraction).coerceAtMost(0.99)
                    val barScale = 10_000
                    // While the XMLTV parser is downloading/parsing, the real
                    // program count stays at 0 even though work is happening.
                    // Synthesize a time-based count (~1k/sec) so the items
                    // line moves immediately; once the real count overtakes
                    // it takes over via maxOf.
                    val syntheticCount = (elapsedSec * 1000).toInt()
                    val itemsForDisplay = maxOf(programCount, syntheticCount)
                    _uiState.update {
                        it.copy(
                            deepIndexPhase = Section.EPG,
                            deepIndexedRows = itemsForDisplay,
                            deepIndexCompletedCategories = (effective * barScale).toInt(),
                            deepIndexTotalCategories = barScale
                        )
                    }
                }
                .first { (jobs, _) ->
                    val epgJob = jobs.firstOrNull { it.section.equals(SECTION_EPG, ignoreCase = true) }
                    epgJob == null || epgJob.state.uppercase() in TERMINAL_STATES
                }
        }
    }

    private suspend fun pinEpgToFinal(
        providerId: Long,
        deepIndexFlow: kotlinx.coroutines.flow.Flow<List<com.nexus.iptv.data.local.entity.XtreamIndexJobEntity>>
    ) {
        val finalCount = programDao.countByProvider(providerId)
        _uiState.update {
            it.copy(
                deepIndexPhase = Section.EPG,
                deepIndexedRows = finalCount,
                deepIndexCompletedCategories = finalCount,
                deepIndexTotalCategories = finalCount
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private companion object {
        const val SECTION_MOVIE = "MOVIE"
        const val SECTION_SERIES = "SERIES"
        const val SECTION_EPG = "EPG"
        val TERMINAL_STATES = setOf("SUCCESS", "PARTIAL", "FAILED_RETRYABLE", "FAILED_PERMANENT")
        const val DEEP_INDEX_TIMEOUT_MS = 5L * 60_000L
        const val PHASE_COMPLETE_HOLD_MS = 1_500L
        const val LIVE_COMPLETE_HOLD_MS = 1_500L
    }
}

@Composable
fun NexusSignInScreen(
    onSignInComplete: () -> Unit,
    onAddCustomProvider: () -> Unit,
    viewModel: NexusSignInViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val blankErrorMessage = stringResource(R.string.nexus_sign_in_error_blank)
    val genericErrorMessage = stringResource(R.string.nexus_sign_in_error_credentials)

    LaunchedEffect(uiState.signInSuccess) {
        if (uiState.signInSuccess) {
            // Hold on the "You're signed in" panel briefly so the user sees the
            // message about background indexing before we drop them on Home.
            delay(2500)
            onSignInComplete()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.22f),
                            AppColors.HeroTop,
                            AppColors.HeroBottom
                        )
                    )
                )
        )

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = SurfaceDefaults.colors(containerColor = AppColors.Surface.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 40.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (uiState.signInSuccess) {
                    SignInCompleteContent()
                } else if (uiState.isLoading) {
                    SignInLoadingContent(syncProgress = syncProgress)
                } else {
                    SignInFormContent(
                        username = username,
                        onUsernameChange = {
                            username = it
                            if (uiState.errorMessage != null) viewModel.clearError()
                        },
                        password = password,
                        onPasswordChange = {
                            password = it
                            if (uiState.errorMessage != null) viewModel.clearError()
                        },
                        errorMessage = uiState.errorMessage,
                        onSignIn = {
                            viewModel.signIn(username, password, blankErrorMessage, genericErrorMessage)
                        },
                        onAddCustomProvider = onAddCustomProvider
                    )
                }
            }
        }
    }
}

@Composable
private fun SignInFormContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    errorMessage: String?,
    onSignIn: () -> Unit,
    onAddCustomProvider: () -> Unit
) {
    StatusPill(
        label = stringResource(R.string.app_name),
        containerColor = AppColors.BrandMuted
    )
    Text(
        text = stringResource(R.string.nexus_sign_in_title),
        style = MaterialTheme.typography.headlineMedium,
        color = AppColors.TextPrimary,
        textAlign = TextAlign.Center
    )
    Text(
        text = stringResource(R.string.nexus_sign_in_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = AppColors.TextSecondary,
        textAlign = TextAlign.Center
    )

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = AppColors.TextPrimary,
        unfocusedTextColor = AppColors.TextPrimary,
        focusedBorderColor = AppColors.Brand,
        unfocusedBorderColor = AppColors.BrandMuted,
        focusedLabelColor = AppColors.Brand,
        unfocusedLabelColor = AppColors.TextSecondary,
        cursorColor = AppColors.Brand
    )

    val usernameFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { usernameFocusRequester.requestFocus() }

    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(usernameFocusRequester),
        label = { androidx.compose.material3.Text(stringResource(R.string.nexus_sign_in_username_hint)) },
        singleLine = true,
        colors = fieldColors
    )
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        label = { androidx.compose.material3.Text(stringResource(R.string.nexus_sign_in_password_hint)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        colors = fieldColors
    )

    errorMessage?.let { message ->
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = ErrorColor,
            textAlign = TextAlign.Center
        )
    }

    TvButton(
        onClick = onSignIn,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = stringResource(R.string.nexus_sign_in_button))
    }

    Text(
        text = stringResource(R.string.nexus_sign_in_support_hint),
        style = MaterialTheme.typography.labelMedium,
        color = AppColors.TextSecondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(4.dp))

    TvButton(
        onClick = onAddCustomProvider,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.colors(
            containerColor = AppColors.SurfaceElevated,
            focusedContainerColor = Color.White,
            contentColor = AppColors.TextPrimary
        )
    ) {
        Text(text = stringResource(R.string.nexus_sign_in_use_custom_provider))
    }
}

@Composable
private fun SignInLoadingContent(syncProgress: SyncProgress?) {
    val pillLabel: String
    val pillColor: Color
    if (syncProgress != null) {
        pillLabel = stringResource(sectionLabelRes(syncProgress.section))
        pillColor = sectionColor(syncProgress.section)
    } else {
        pillLabel = stringResource(R.string.app_name)
        pillColor = AppColors.BrandMuted
    }

    StatusPill(label = pillLabel, containerColor = pillColor)

    Text(
        text = stringResource(R.string.nexus_sign_in_loading_title),
        style = MaterialTheme.typography.headlineMedium,
        color = AppColors.TextPrimary,
        textAlign = TextAlign.Center
    )

    val phaseText = if (syncProgress != null) {
        stringResource(sectionPhaseLabelRes(syncProgress.section))
    } else {
        stringResource(R.string.nexus_sign_in_loading_connecting)
    }
    Text(
        text = phaseText,
        style = MaterialTheme.typography.bodyMedium,
        color = AppColors.TextSecondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(4.dp))

    if (syncProgress != null && syncProgress.total > 0) {
        LinearProgressIndicator(
            progress = { syncProgress.current.toFloat() / syncProgress.total.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = AppColors.Brand,
            trackColor = AppColors.BrandMuted
        )
    } else {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = AppColors.Brand,
            trackColor = AppColors.BrandMuted
        )
    }

    if (syncProgress != null) {
        Text(
            text = stringResource(R.string.sync_items_indexed_format, syncProgress.itemsIndexed),
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.TextSecondary
        )
        // EPG only has a 1-category total (the BackgroundEpgSyncWorker writes
        // it as a single bucket), so the "X / Y categories" subline isn't
        // informative for that phase — hide it.
        if (syncProgress.total > 0 && syncProgress.section != Section.EPG) {
            Text(
                text = stringResource(
                    R.string.nexus_sign_in_progress_count_format,
                    syncProgress.current,
                    syncProgress.total
                ),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextSecondary
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = stringResource(R.string.nexus_sign_in_loading_subtitle_syncing),
        style = MaterialTheme.typography.bodyMedium,
        color = AppColors.TextSecondary,
        textAlign = TextAlign.Center
    )
    Text(
        text = stringResource(R.string.nexus_sign_in_loading_dont_close),
        style = MaterialTheme.typography.labelMedium,
        color = AppColors.TextSecondary,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SignInCompleteContent() {
    StatusPill(
        label = stringResource(R.string.app_name),
        containerColor = AppColors.Brand
    )
    Text(
        text = stringResource(R.string.nexus_sign_in_complete_title),
        style = MaterialTheme.typography.headlineMedium,
        color = AppColors.TextPrimary,
        textAlign = TextAlign.Center
    )
    Text(
        text = stringResource(R.string.nexus_sign_in_complete_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = AppColors.TextSecondary,
        textAlign = TextAlign.Center
    )
}

private fun sectionColor(section: Section): Color = when (section) {
    Section.LIVE -> AppColors.Brand
    Section.VOD -> AppColors.Success
    Section.SERIES -> AppColors.Warning
    Section.EPG -> AppColors.BrandStrong
}

private fun sectionLabelRes(section: Section): Int = when (section) {
    Section.LIVE -> R.string.sync_section_live
    Section.VOD -> R.string.sync_section_vod
    Section.SERIES -> R.string.sync_section_series
    Section.EPG -> R.string.sync_section_epg
}

private fun sectionPhaseLabelRes(section: Section): Int = when (section) {
    Section.LIVE -> R.string.nexus_sign_in_loading_indexing_live
    Section.VOD -> R.string.nexus_sign_in_loading_indexing_vod
    Section.SERIES -> R.string.nexus_sign_in_loading_indexing_series
    Section.EPG -> R.string.nexus_sign_in_loading_indexing_epg
}
