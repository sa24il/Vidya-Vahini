package com.example.vidhyavahini.api

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.delay

class GeminiService(private val generativeModel: GenerativeModel) {

    suspend fun generateContentWithRetry(prompt: String, maxRetries: Int = 3): GenerateContentResponse? {
        var currentRetry = 0
        var waitTime = 2000L // Start with 2 seconds

        while (currentRetry < maxRetries) {
            try {
                return generativeModel.generateContent(prompt)
            } catch (e: Exception) {
                if (e.message?.contains("429") == true) {
                    currentRetry++
                    if (currentRetry >= maxRetries) throw e
                    delay(waitTime)
                    waitTime *= 2 // Exponential backoff
                } else {
                    throw e
                }
            }
        }
        return null
    }
}
