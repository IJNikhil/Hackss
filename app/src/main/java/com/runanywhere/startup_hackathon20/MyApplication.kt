package com.runanywhere.startup_hackathon20

import android.app.Application
import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.data.models.SDKEnvironment
import com.runanywhere.sdk.public.extensions.addModelFromURL
import com.runanywhere.sdk.llm.llamacpp.LlamaCppServiceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SDK asynchronously
        GlobalScope.launch(Dispatchers.IO) {
            initializeSDK()
        }
    }

    private suspend fun initializeSDK() {
        try {
            Log.i("MyApp", "Starting SDK initialization...")
            
            // Step 1: Initialize SDK
            RunAnywhere.initialize(
                context = this@MyApplication,
                apiKey = "dev",  // Any string works in dev mode
                environment = SDKEnvironment.DEVELOPMENT
            )

            // Step 2: Register LLM Service Provider
            LlamaCppServiceProvider.register()

            // Step 3: Register Models
            registerModels()

            // Step 4: Scan for previously downloaded models
            RunAnywhere.scanForDownloadedModels()

            // Log all available models for debugging
            val availableModels = com.runanywhere.sdk.public.extensions.listAvailableModels()
            Log.i("MyApp", "SDK initialized successfully")
            Log.i("MyApp", "Available models (${availableModels.size}):")
            availableModels.forEach { model ->
                Log.i("MyApp", "  - ID: '${model.id}' | Name: '${model.name}' | Downloaded: ${model.isDownloaded}")
            }

            SDKState.setInitialized(true)

        } catch (e: Exception) {
            Log.e("MyApp", "SDK initialization failed: ${e.message}", e)
            SDKState.setInitialized(false, e.message ?: "Unknown error")
        }
    }

    private suspend fun registerModels() {
        // START FINAL REGISTRATION LOGIC (Using official names for maximum stability)
        // 1. High Performance Model (1.2 GB)
        addModelFromURL(
            url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q6_k.gguf",
            name = "Qwen 2.5 1.5B Instruct Q6_K", // EXACT OFFICIAL NAME
            type = "LLM"
        )
        // 2. Balanced Safety Model (374 MB)
        addModelFromURL(
            url = "https://huggingface.co/Triangle104/Qwen2.5-0.5B-Instruct-Q6_K-GGUF/resolve/main/qwen2.5-0.5b-instruct-q6_k.gguf",
            name = "Qwen 2.5 0.5B Instruct Q6_K", // EXACT OFFICIAL NAME
            type = "LLM"
        )
        // END FINAL REGISTRATION LOGIC
    }
}
