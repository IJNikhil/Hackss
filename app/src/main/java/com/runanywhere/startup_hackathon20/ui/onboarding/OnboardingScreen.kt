package com.runanywhere.startup_hackathon20.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.startup_hackathon20.ChatViewModel
import kotlinx.coroutines.delay
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.runanywhere.startup_hackathon20.workers.LlmWorker
import android.content.Context

@Composable
fun OnboardingScreen(
    onSetupComplete: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val availableModels by viewModel.availableModels.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Model Options
    val powerModelName = "Phi-3 Mini 4k (High Performance)"
    val safeModelName = "Qwen 2.5 1.5B (Balanced)"

    // Use rememberSaveable to persist selection across configuration changes (theme switch, rotation)
    var selectedModelName by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(powerModelName) }
    
    // Local state to show immediate feedback when button is clicked
    var isInitializing by remember { mutableStateOf(false) }

    // Find the target model based on selection
    val targetModel = availableModels.find { it.name == selectedModelName }

    // Permission Launcher
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
        }
    )

    // Check if ALREADY downloaded
    LaunchedEffect(availableModels, selectedModelName) {
        if (targetModel?.isDownloaded == true) {
            onSetupComplete()
        }
    }

    // Auto-refresh logic if model is missing
    LaunchedEffect(targetModel) {
        if (targetModel == null) {
            while(true) {
                delay(3000)
                viewModel.refreshModels()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header
            Spacer(modifier = Modifier.height(24.dp))
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Secure AI Vault",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Select your intelligence engine",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 2. Model Selector
            Text(
                text = "Select Intelligence Engine",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Disable selection if downloading or initializing
            val isLocked = downloadProgress != null || isInitializing

            ModelOptionCard(
                title = "High Performance (Phi-3)",
                description = "Smartest. Best for Legal/Finance. Requires 2.3GB.",
                icon = Icons.Filled.Bolt,
                isSelected = selectedModelName == powerModelName,
                isEnabled = !isLocked,
                onClick = { selectedModelName = powerModelName }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ModelOptionCard(
                title = "Balanced Mode (Qwen)",
                description = "Faster. Good for General tasks. Requires 1.2GB.",
                icon = Icons.Filled.Shield,
                isSelected = selectedModelName == safeModelName,
                isEnabled = !isLocked,
                onClick = { selectedModelName = safeModelName }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 3. Action Area
            if (downloadProgress != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Downloading...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(downloadProgress!! * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress!! },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please keep the app open",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.cancelDownload() }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (isInitializing) return@Button
                        
                        // Check for Notification Permission on Android 13+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            targetModel?.let { model ->
                                isInitializing = true
                                viewModel.downloadModel(model.id)
                            } ?: run {
                                viewModel.refreshModels()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    enabled = !isInitializing, 
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (targetModel == null || isInitializing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (isInitializing) "Starting..." else "Connecting...")
                    } else {
                        val buttonText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            "Enable Notifications & Initialize"
                        } else {
                            "Download Intelligence"
                        }
                        
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buttonText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            // Reset initializing state if download starts or fails
            LaunchedEffect(downloadProgress, statusMessage) {
                if (downloadProgress != null || statusMessage.contains("failed") || statusMessage.contains("cancelled")) {
                    isInitializing = false
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Test Button for WorkManager
            Button(
                onClick = { enqueueProcessingJob(context) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Test Background Processing")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun enqueueProcessingJob(context: Context) {
    val workManager = WorkManager.getInstance(context)
    val inputData = workDataOf("INPUT_DATA" to "Test Document")
    
    val request = OneTimeWorkRequestBuilder<LlmWorker>()
        .setInputData(inputData)
        .build()
        
    workManager.enqueue(request)
}

@Composable
fun ModelOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
    val alpha = if (isEnabled) 1f else 0.5f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = isEnabled, onClick = onClick),
        color = backgroundColor.copy(alpha = if (isSelected) 0.1f else 1f).copy(alpha = alpha) // Adjust alpha for disabled state
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = (if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = alpha),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = (if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = alpha)
            )
        }
    }
}
