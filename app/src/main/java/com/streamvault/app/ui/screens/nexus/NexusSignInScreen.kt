package com.streamvault.app.ui.screens.nexus

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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
import com.streamvault.app.R
import com.streamvault.app.branding.NexusBranding
import com.streamvault.app.ui.components.shell.StatusPill
import com.streamvault.app.ui.design.AppColors
import com.streamvault.app.ui.interaction.TvButton
import com.streamvault.app.ui.theme.ErrorColor
import com.streamvault.domain.usecase.ValidateAndAddProvider
import com.streamvault.domain.usecase.ValidateAndAddProviderResult
import com.streamvault.domain.usecase.XtreamProviderSetupCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NexusSignInUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val signInSuccess: Boolean = false
)

@HiltViewModel
class NexusSignInViewModel @Inject constructor(
    private val validateAndAddProvider: ValidateAndAddProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(NexusSignInUiState())
    val uiState: StateFlow<NexusSignInUiState> = _uiState.asStateFlow()

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
                    xtreamFastSyncEnabled = true
                )
            )

            _uiState.update {
                when (result) {
                    is ValidateAndAddProviderResult.Success,
                    is ValidateAndAddProviderResult.SavedWithWarning ->
                        it.copy(isLoading = false, signInSuccess = true, errorMessage = null)
                    is ValidateAndAddProviderResult.ValidationError ->
                        it.copy(isLoading = false, errorMessage = result.message)
                    is ValidateAndAddProviderResult.Error ->
                        it.copy(isLoading = false, errorMessage = genericErrorMessage)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

@Composable
fun NexusSignInScreen(
    onSignInComplete: () -> Unit,
    onAddCustomProvider: () -> Unit,
    viewModel: NexusSignInViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val blankErrorMessage = stringResource(R.string.nexus_sign_in_error_blank)
    val genericErrorMessage = stringResource(R.string.nexus_sign_in_error_credentials)

    LaunchedEffect(uiState.signInSuccess) {
        if (uiState.signInSuccess) onSignInComplete()
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

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        if (uiState.errorMessage != null) viewModel.clearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { androidx.compose.material3.Text(stringResource(R.string.nexus_sign_in_username_hint)) },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (uiState.errorMessage != null) viewModel.clearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { androidx.compose.material3.Text(stringResource(R.string.nexus_sign_in_password_hint)) },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = fieldColors
                )

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorColor,
                        textAlign = TextAlign.Center
                    )
                }

                TvButton(
                    onClick = {
                        viewModel.signIn(username, password, blankErrorMessage, genericErrorMessage)
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(0.dp))
                        Text(
                            text = stringResource(R.string.nexus_sign_in_signing_in),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    } else {
                        Text(text = stringResource(R.string.nexus_sign_in_button))
                    }
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
                    enabled = !uiState.isLoading,
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
        }
    }
}
