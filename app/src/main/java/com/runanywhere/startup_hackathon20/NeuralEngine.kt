package com.runanywhere.startup_hackathon20

import com.runanywhere.sdk.data.models.ModelInfo
import com.runanywhere.sdk.public.RunAnywhere
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object NeuralEngine {

    /**
     * Lists all models registered in the RunAnywhere SDK.
     */
    fun listAvailableModels(): List<ModelInfo> {
        return RunAnywhere.listAvailableModels()
    }

    /**
     * Downloads a model by its ID and emits progress updates.
     * Returns a Flow emitting progress from 0.0 to 1.0.
     */
    fun downloadModel(modelId: String): Flow<Float> {
        return RunAnywhere.downloadModel(modelId)
    }

    /**
     * Loads a model into memory.
     * Returns true if successful, false otherwise.
     */
    suspend fun loadModel(modelId: String): Boolean {
        return try {
            RunAnywhere.loadModel(modelId)
            true
        } catch (e: Exception) {
            false
        }
    }
}
