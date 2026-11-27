package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiPerformanceRepository
import com.example.model.AiChatMessage
import com.example.model.AiModelMode
import com.example.model.AiResponseState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

private const val TAG = "AiAdvisorViewModel"

class AiAdvisorViewModel : ViewModel() {

    private val aiRepository = AiPerformanceRepository()

    private val _selectedMode = MutableStateFlow(AiModelMode.FAST)
    val selectedMode: StateFlow<AiModelMode> = _selectedMode.asStateFlow()

    private val _quickPulseState = MutableStateFlow<AiResponseState>(AiResponseState.Idle)
    val quickPulseState: StateFlow<AiResponseState> = _quickPulseState.asStateFlow()

    private val _chatState = MutableStateFlow<AiResponseState>(AiResponseState.Idle)
    val chatState: StateFlow<AiResponseState> = _chatState.asStateFlow()

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    fun selectMode(mode: AiModelMode) {
        _selectedMode.value = mode
    }

    /**
     * Low-latency diagnostic pulse using gemini-3.1-flash-lite.
     */
    fun runQuickPulse(telemetrySummary: String) {
        viewModelScope.launch {
            _quickPulseState.value = AiResponseState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(45_000L) {
                    aiRepository.streamAdvice(
                        userPrompt = "Berikan ringkasan diagnosa kilat performa HP ini dalam 3 poin singkat (RAM, suhu baterai, dan kelancaran layar) serta 1 langkah aksi prioritas.",
                        telemetryContext = telemetrySummary,
                        mode = AiModelMode.FAST
                    ).collect { chunk ->
                        accumulated.append(chunk)
                        if (accumulated.isNotBlank()) {
                            _quickPulseState.value = AiResponseState.Streaming(accumulated.toString())
                        }
                    }
                }
                val finalText = accumulated.toString().trim()
                if (finalText.isNotEmpty()) {
                    _quickPulseState.value = AiResponseState.Success(finalText, AiModelMode.FAST)
                } else {
                    _quickPulseState.value = AiResponseState.Error("Respons AI kosong. Silakan coba lagi.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Quick pulse timed out", e)
                _quickPulseState.value = AiResponseState.Error("Permintaan diagnosa kilat timeout. Coba lagi.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Quick pulse failed", e)
                _quickPulseState.value = AiResponseState.Error(
                    e.localizedMessage ?: "Gagal menghubungi layanan Firebase AI Logic."
                )
            }
        }
    }

    /**
     * Interactive AI Performance Advisor supporting both gemini-3.1-flash-lite (low-latency)
     * and gemini-4-carbon with ThinkingLevel.HIGH (deep analysis).
     */
    fun sendQuery(prompt: String, telemetrySummary: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        val activeMode = _selectedMode.value

        _messages.update {
            it + AiChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = true,
                text = trimmed
            )
        }

        viewModelScope.launch {
            _chatState.value = AiResponseState.Loading
            val accumulated = StringBuilder()
            try {
                // Deep thinking queries can take longer to reason thoroughly
                withTimeout(60_000L) {
                    aiRepository.streamAdvice(
                        userPrompt = trimmed,
                        telemetryContext = telemetrySummary,
                        mode = activeMode
                    ).collect { chunk ->
                        accumulated.append(chunk)
                        if (accumulated.isNotBlank()) {
                            _chatState.value = AiResponseState.Streaming(accumulated.toString())
                        }
                    }
                }

                val finalText = accumulated.toString().trim()
                if (finalText.isNotEmpty()) {
                    _messages.update {
                        it + AiChatMessage(
                            id = UUID.randomUUID().toString(),
                            isUser = false,
                            text = finalText,
                            modeUsed = activeMode
                        )
                    }
                    _chatState.value = AiResponseState.Success(finalText, activeMode)
                } else {
                    _chatState.value = AiResponseState.Error("Model mengembalikan respons kosong.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI query timed out", e)
                _chatState.value = AiResponseState.Error(
                    "Waktu analisis habis setelah 60 detik. Coba sederhanakan pertanyaan atau gunakan mode Kilat."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI query failed", e)
                _chatState.value = AiResponseState.Error(
                    e.localizedMessage ?: "Gagal memproses analisis AI."
                )
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
        _chatState.value = AiResponseState.Idle
    }
}
