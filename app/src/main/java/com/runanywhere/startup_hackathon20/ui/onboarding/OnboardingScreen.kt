package com.runanywhere.startup_hackathon20.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.startup_hackathon20.NeuralEngine
import com.runanywhere.startup_hackathon20.SDKState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel to handle state and NeuralEngine interactions
class OnboardingViewModel : ViewModel() {
    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _isDownloadComplete = MutableStateFlow(false)
    val isDownloadComplete: StateFlow<Boolean> = _isDownloadComplete.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Dynamic Model IDs
    private val _highPerfModelId = MutableStateFlow<String?>(null)
    val highPerfModelId: StateFlow<String?> = _highPerfModelId.asStateFlow()

    private val _balancedModelId = MutableStateFlow<String?>(null)
    val balancedModelId: StateFlow<String?> = _balancedModelId.asStateFlow()

    fun resolveModelIds() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val models = com.runanywhere.sdk.public.extensions.listAvailableModels()
                Log.d("OnboardingViewModel", "Resolving IDs from ${models.size} models")
                models.forEach { Log.d("OnboardingViewModel", "Available: ${it.name} -> ${it.id}") }
                
                // Match by the EXACT OFFICIAL NAMES we registered in MyApplication.kt
                val highPerfName = "Qwen 2.5 1.5B Instruct Q6_K"
                val balancedName = "Qwen 2.5 0.5B Instruct Q6_K"

                // Robust matching: Exact match -> Partial match -> Fallback to Name
                val highPerfModel = models.find { it.name == highPerfName }
                    ?: models.find { it.name.contains("1.5B", ignoreCase = true) }
                
                val balancedModel = models.find { it.name == balancedName }
                    ?: models.find { it.name.contains("0.5B", ignoreCase = true) }

                _highPerfModelId.value = highPerfModel?.id ?: highPerfName
                _balancedModelId.value = balancedModel?.id ?: balancedName
                
                if (highPerfModel == null) Log.w("OnboardingViewModel", "High Perf model exact match failed, using fallback: ${_highPerfModelId.value}")
                if (balancedModel == null) Log.w("OnboardingViewModel", "Balanced model exact match failed, using fallback: ${_balancedModelId.value}")
            } catch (e: Exception) {
                Log.e("OnboardingViewModel", "Failed to resolve model IDs", e)
            }
        }
    }

    fun startDownload(modelId: String) {
        if (_isDownloading.value) {
            Log.d("OnboardingViewModel", "Download already in progress")
            return
        }

        if (!SDKState.isInitialized.value) {
            _errorMessage.value = "SDK is still initializing. Please wait..."
            Log.e("OnboardingViewModel", "Attempted download before SDK initialization")
            return
        }

        _errorMessage.value = null
        Log.d("OnboardingViewModel", "Starting download for model: $modelId")
        
        viewModelScope.launch(Dispatchers.IO) {
            _isDownloading.value = true
            try {
                Log.d("OnboardingViewModel", "Calling NeuralEngine.downloadModel with ID: $modelId")
                NeuralEngine.downloadModel(modelId).collect { progress ->
                    Log.d("OnboardingViewModel", "Download progress: $progress")
                    _downloadProgress.value = progress
                }
                Log.d("OnboardingViewModel", "Download complete")
                _downloadProgress.value = null
                _errorMessage.value = "Download complete!"
                _isDownloadComplete.value = true
            } catch (e: Exception) {
                Log.e("OnboardingViewModel", "Error downloading model", e)
                _downloadProgress.value = null
                _errorMessage.value = "Download failed: ${e.message}"
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onSetupComplete: () -> Unit = {},
    viewModel: OnboardingViewModel = viewModel()
) {
    val context = LocalContext.current
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val sdkInitialized by SDKState.isInitialized.collectAsState()
    val sdkError by SDKState.initializationError.collectAsState()
    val isDownloadComplete by viewModel.isDownloadComplete.collectAsState()

    // Navigate when download completes
    LaunchedEffect(isDownloadComplete) {
        if (isDownloadComplete) {
            onSetupComplete()
        }
    }

    // Dynamic Model IDs
    val highPerfModelId by viewModel.highPerfModelId.collectAsState()
    val balancedModelId by viewModel.balancedModelId.collectAsState()

    // Trigger ID resolution when SDK is initialized
    LaunchedEffect(sdkInitialized) {
        if (sdkInitialized) {
            viewModel.resolveModelIds()
        }
    }

    // State for selected model - default to High Perf if available, or null
    var selectedModelId by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Auto-select high perf model once resolved
    LaunchedEffect(highPerfModelId) {
        if (selectedModelId == null && highPerfModelId != null) {
            selectedModelId = highPerfModelId
        }
    }

    // Permission State
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Secure AI Vault Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            if (errorMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(errorMessage!!)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "Secure AI Icon",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Title
                    Text(
                        text = "AI Vault Setup",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description
                    Text(
                        text = "Select your preferred intelligence level.\nAll processing is performed locally.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // SDK Initialization Status
                    if (!sdkInitialized) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (sdkError != null) {
                            Text(
                                text = "⚠️ Initialization failed: $sdkError",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Initializing AI engine...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Model Selection Cards
                    SelectableModelCard(
                        title = "High Performance",
                        subtitle = "Qwen 2.5 1.5B",
                        isSelected = selectedModelId == highPerfModelId,
                        onClick = { if (!isDownloading && highPerfModelId != null) selectedModelId = highPerfModelId },
                        enabled = sdkInitialized && highPerfModelId != null
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SelectableModelCard(
                        title = "Balanced",
                        subtitle = "Qwen 2.5 0.5B",
                        isSelected = selectedModelId == balancedModelId,
                        onClick = { if (!isDownloading && balancedModelId != null) selectedModelId = balancedModelId },
                        enabled = sdkInitialized && balancedModelId != null
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Progress Indicator
                    if (isDownloading && downloadProgress != null) {
                        LinearProgressIndicator(
                            progress = { downloadProgress!! },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Downloading... ${(downloadProgress!! * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Main Action Button
                    val buttonText = when {
                        !sdkInitialized -> "Initializing..."
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission -> "Enable Notifications"
                        isDownloading -> "Downloading..."
                        else -> "Download Intelligence"
                    }

                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                selectedModelId?.let { viewModel.startDownload(it) }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = sdkInitialized && !isDownloading && selectedModelId != null
                    ) {
                        if (sdkInitialized) {
                            Icon(
                                imageVector = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission)
                                    Icons.Filled.CheckCircle else Icons.Filled.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectableModelCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val borderColor = if (isSelected && enabled) 
        MaterialTheme.colorScheme.primary 
    else 
        MaterialTheme.colorScheme.outlineVariant
    
    val containerColor = if (isSelected && enabled) 
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
    else 
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        ),
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface 
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = if (isSelected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (enabled && isSelected) MaterialTheme.colorScheme.primary 
                       else if (enabled) MaterialTheme.colorScheme.outline
                       else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
            )
        }
    }
}
