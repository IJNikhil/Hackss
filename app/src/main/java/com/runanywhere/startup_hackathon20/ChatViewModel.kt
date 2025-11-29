package com.runanywhere.startup_hackathon20

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.listAvailableModels
import com.runanywhere.sdk.models.ModelInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Simple Message Data Class
data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

// ViewModel
class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress

    private val _currentModelId = MutableStateFlow<String?>(null)
    val currentModelId: StateFlow<String?> = _currentModelId

    private val _statusMessage = MutableStateFlow<String>("Initializing...")
    val statusMessage: StateFlow<String> = _statusMessage

    init {
        loadAvailableModels()
    }

    private fun loadAvailableModels() {
        viewModelScope.launch {
            try {
                val models = listAvailableModels()
                _availableModels.value = models
                
                // Check if our target models exist
                val phiName = "Phi-3 Mini 4k (High Performance)"
                val qwenName = "Qwen 2.5 1.5B (Balanced)"
                
                val phiExists = models.any { it.name == phiName }
                val qwenExists = models.any { it.name == qwenName }
                
                if (!phiExists || !qwenExists) {
                    _statusMessage.value = "Registering models..."
                    
                    if (!phiExists) {
                        com.runanywhere.sdk.public.extensions.addModelFromURL(
                            url = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf",
                            name = phiName,
                            type = "LLM"
                        )
                    }
                    
                    if (!qwenExists) {
                        com.runanywhere.sdk.public.extensions.addModelFromURL(
                            url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q6_k.gguf",
                            name = qwenName,
                            type = "LLM"
                        )
                    }
                    
                    // Refresh list after adding
                    _availableModels.value = listAvailableModels()
                }
                
                _statusMessage.value = "Ready - Please download and load a model"
            } catch (e: Exception) {
                _statusMessage.value = "Error loading models: ${e.message}"
            }
        }
    }

    private var downloadJob: kotlinx.coroutines.Job? = null

    fun downloadModel(modelId: String) {
        downloadJob?.cancel() // Cancel any existing download
        downloadJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _statusMessage.value = "Initializing download..."
                // Add a small delay to ensure UI updates
                kotlinx.coroutines.delay(100)
                
                RunAnywhere.downloadModel(modelId).collect { progress ->
                    _downloadProgress.value = progress
                    if (progress <= 0.0f) {
                        _statusMessage.value = "Connecting to server..."
                    } else {
                        _statusMessage.value = "Downloading: ${(progress * 100).toInt()}%"
                    }
                }
                _downloadProgress.value = null
                _statusMessage.value = "Download complete! Please load the model."
            } catch (e: kotlinx.coroutines.CancellationException) {
                _statusMessage.value = "Download cancelled"
                _downloadProgress.value = null
            } catch (e: Exception) {
                _statusMessage.value = "Download failed: ${e.message}"
                _downloadProgress.value = null
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _downloadProgress.value = null
        _statusMessage.value = "Download cancelled by user"
    }

    fun loadModel(modelId: String) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Loading model..."
                val success = RunAnywhere.loadModel(modelId)
                if (success) {
                    _currentModelId.value = modelId
                    _statusMessage.value = "Model loaded! Ready to chat."
                } else {
                    _statusMessage.value = "Failed to load model"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error loading model: ${e.message}"
            }
        }
    }

    fun sendMessage(text: String) {
        if (_currentModelId.value == null) {
            _statusMessage.value = "Please load a model first"
            return
        }

        // Add user message
        _messages.value += ChatMessage(text, isUser = true)

        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Generate response with streaming
                var assistantResponse = ""
                RunAnywhere.generateStream(text).collect { token ->
                    assistantResponse += token

                    // Update assistant message in real-time
                    val currentMessages = _messages.value.toMutableList()
                    if (currentMessages.lastOrNull()?.isUser == false) {
                        currentMessages[currentMessages.lastIndex] =
                            ChatMessage(assistantResponse, isUser = false)
                    } else {
                        currentMessages.add(ChatMessage(assistantResponse, isUser = false))
                    }
                    _messages.value = currentMessages
                }
            } catch (e: Exception) {
                _messages.value += ChatMessage("Error: ${e.message}", isUser = false)
            }

            _isLoading.value = false
        }
    }

    fun refreshModels() {
        loadAvailableModels()
    }
}
