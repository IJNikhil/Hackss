package com.runanywhere.startup_hackathon20

import com.runanywhere.sdk.data.models.ModelInfo
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.listAvailableModels
import kotlinx.coroutines.flow.Flow

object NeuralEngine {

    suspend fun listAvailableModels(): List<ModelInfo> {
        return listAvailableModels()
    }

    suspend fun downloadModel(modelId: String): Flow<Float> {
        android.util.Log.d("NeuralEngine", "downloadModel called for: $modelId")
        return com.runanywhere.sdk.public.RunAnywhere.downloadModel(modelId)
    }

    suspend fun loadModel(modelId: String): Boolean {
        return try {
            RunAnywhere.loadModel(modelId)
            true
        } catch (e: Exception) {
            false
        }
    }
}
