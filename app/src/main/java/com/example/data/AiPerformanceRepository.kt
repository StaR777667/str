package com.example.data

import com.example.model.AiModelMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AiPerformanceRepository {

    // Low-latency model for quick performance diagnostics & instant optimization pulse
    private val fastModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.1-flash-lite"
        )
    }

    // Deep reasoning model with ThinkingLevel.HIGH and NO maxOutputTokens set
    private val deepThinkingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-carbon",
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            }
        )
    }

    fun streamAdvice(
        userPrompt: String,
        telemetryContext: String,
        mode: AiModelMode
    ): Flow<String> {
        val model = when (mode) {
            AiModelMode.FAST -> fastModel
            AiModelMode.DEEP_THINKING -> deepThinkingModel
        }

        val fullPrompt = buildString {
            appendLine("Anda adalah pakar teknik performa Android, gaming FPS, manajemen RAM, dan termal baterai.")
            appendLine("Jawab dalam Bahasa Indonesia yang jelas, terstruktur, praktis, dan langsung merujuk ke data telemetri perangkat pengguna berikut:")
            appendLine()
            appendLine("=== TELEMETRI PERANGKAT SAAT INI ===")
            appendLine(telemetryContext)
            appendLine("======================================")
            appendLine()
            appendLine("Pertanyaan / Permintaan Pengguna:")
            appendLine(userPrompt)
        }

        return model.generateContentStream(fullPrompt).map { chunk ->
            chunk.text.orEmpty()
        }
    }
}
