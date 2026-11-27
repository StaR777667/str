package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AiChatMessage
import com.example.model.AiModelMode
import com.example.model.AiResponseState
import com.example.ui.components.GlassTelemetryCard
import com.example.ui.components.SectionTitleRow
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val QuickPrompts = listOf(
    "Analisa kenapa HP panas & throttling saat main game",
    "Rekomendasi setting FPS & refresh rate terbaik untuk HP ini",
    "Evaluasi kesehatan RAM & proses multitasking saat ini",
    "Tips menjaga baterai awet & performa tetap licin"
)

@Composable
fun AiAdvisorScreen(
    selectedMode: AiModelMode,
    chatState: AiResponseState,
    messages: List<AiChatMessage>,
    onSelectMode: (AiModelMode) -> Unit,
    onSendPrompt: (String) -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isBusy = chatState is AiResponseState.Loading || chatState is AiResponseState.Streaming

    LaunchedEffect(messages.size, chatState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Model Selector Header (Fast Flash-Lite vs Deep Thinking HIGH)
        GlassTelemetryCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            borderColor = ElectricCyan.copy(alpha = 0.35f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionTitleRow(
                    title = "Asisten Performa AI",
                    subtitle = "Analisis berbasis telemetri hardware langsung",
                    icon = Icons.Rounded.AutoAwesome,
                    iconTint = ElectricCyan,
                    trailing = {
                        if (messages.isNotEmpty()) {
                            IconButton(
                                onClick = onClearChat,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("clear_ai_chat_button")
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Hapus Percakapan",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiModelMode.entries.forEach { mode ->
                        val active = selectedMode == mode
                        val accent = if (mode == AiModelMode.FAST) MintEmerald else ElectricCyan
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectMode(mode) }
                                .testTag("ai_mode_${mode.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) accent.copy(alpha = 0.16f) else ElevatedSlate,
                            border = BorderStroke(1.dp, if (active) accent else OutlineSubtle)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (mode == AiModelMode.FAST) Icons.Rounded.Bolt else Icons.Rounded.Psychology,
                                        contentDescription = mode.title,
                                        tint = accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (active) accent else TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = mode.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat / Streaming Output Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && chatState is AiResponseState.Idle) {
                item {
                    GlassTelemetryCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Tanyakan apapun tentang optimisasi perangkat Anda",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "• Mode Kilat (gemini-3.1-flash-lite): Jawaban secepat kilat untuk diagnosa ringan.\n• Mode Analisis Mendalam (gemini-4-carbon • Thinking HIGH): Penalaran mendalam untuk thermal throttling, setting game FPS, dan arsitektur RAM.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                ChatBubble(message = msg)
            }

            when (chatState) {
                AiResponseState.Loading -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DeepSpace,
                            border = BorderStroke(1.dp, OutlineSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = if (selectedMode == AiModelMode.DEEP_THINKING) {
                                        "Berpikir mendalam (Thinking HIGH)..."
                                    } else {
                                        "Menyiapkan jawaban kilat..."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
                is AiResponseState.Streaming -> {
                    item {
                        ChatBubble(
                            message = AiChatMessage(
                                id = "streaming",
                                isUser = false,
                                text = chatState.text,
                                modeUsed = selectedMode
                            )
                        )
                    }
                }
                is AiResponseState.Error -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CoralCrimson.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, CoralCrimson),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = chatState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoralCrimson,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                else -> Unit
            }
        }

        // Quick Prompt Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickPrompts.forEachIndexed { idx, prompt ->
                AssistChip(
                    onClick = {
                        if (!isBusy) onSendPrompt(prompt)
                    },
                    label = { Text(prompt, maxLines = 1) },
                    modifier = Modifier.testTag("quick_prompt_$idx"),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = ElevatedSlate,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        // Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Tanya tentang FPS, RAM, suhu, atau spek...") },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .testTag("ai_prompt_input"),
                shape = RoundedCornerShape(14.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = OutlineSubtle,
                    focusedContainerColor = DeepSpace,
                    unfocusedContainerColor = DeepSpace
                )
            )
            FilledIconButton(
                onClick = {
                    val toSend = inputText.trim()
                    if (toSend.isNotEmpty() && !isBusy) {
                        inputText = ""
                        onSendPrompt(toSend)
                    }
                },
                enabled = inputText.isNotBlank() && !isBusy,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("ai_send_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ElectricCyan,
                    contentColor = ObsidianVoid
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Kirim Pertanyaan AI"
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: AiChatMessage) {
    val bgColor = if (message.isUser) CyanContainer else DeepSpace
    val borderColor = if (message.isUser) ElectricCyan.copy(alpha = 0.5f) else OutlineSubtle
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (message.isUser) "Anda" else "PulseBoost AI",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (message.isUser) ElectricCyan else MintEmerald
                )
                message.modeUsed?.let { mode ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldContainer
                    ) {
                        Text(
                            text = mode.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MintEmerald,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
    }
}
