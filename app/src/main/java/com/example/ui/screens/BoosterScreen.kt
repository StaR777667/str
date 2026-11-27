package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.formatGigabytes
import com.example.model.DisplayTelemetry
import com.example.model.GameProfile
import com.example.model.LaunchableApp
import com.example.model.OptimizerSettings
import com.example.model.RamBoostResult
import com.example.model.RamTelemetry
import com.example.ui.components.GlassTelemetryCard
import com.example.ui.components.SectionTitleRow
import com.example.ui.components.TouchSmoothnessPad
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoosterScreen(
    display: DisplayTelemetry,
    liveFps: Float,
    liveFrameTimeMs: Float,
    ram: RamTelemetry,
    lastRamBoost: RamBoostResult?,
    settings: OptimizerSettings,
    apps: List<LaunchableApp>,
    onSelectRefreshRate: (Float) -> Unit,
    onSelectGameProfile: (GameProfile) -> Unit,
    onBoostRam: () -> Unit,
    onOpenDisplaySettings: () -> Intent,
    onToggleAddedGame: (LaunchableApp) -> Unit,
    onRemoveAddedGame: (LaunchableApp) -> Unit,
    onBoostForGame: (LaunchableApp) -> Unit,
    onRefreshApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddGameSheet by rememberSaveable { mutableStateOf(false) }

    val distinctRefreshRates = display.supportedModes
        .map { it.refreshRateHz.toInt().toFloat() }
        .distinct()
        .sortedDescending()

    val addedGames = remember(apps, settings.addedGamePackages) {
        apps.filter { it.packageName in settings.addedGamePackages }
    }
    val suggestedGames = remember(apps, settings.addedGamePackages) {
        apps.filter { it.isGame && it.packageName !in settings.addedGamePackages }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen Smoothness & Frame Rate Tuner
        item {
            GlassTelemetryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                borderColor = ElectricCyan.copy(alpha = 0.35f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionTitleRow(
                        title = "Perlicin Layar & Frame Pacing",
                        subtitle = "Layar aktif ${display.activeRefreshRateHz.toInt()} Hz • ${display.widthPx}x${display.heightPx}",
                        icon = Icons.Rounded.Speed,
                        iconTint = ElectricCyan,
                        trailing = {
                            OutlinedButton(
                                onClick = {
                                    runCatching { context.startActivity(onOpenDisplaySettings()) }
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("open_display_settings_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Settings,
                                    contentDescription = "Pengaturan Layar OS",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = ElevatedSlate
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Live FPS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "${liveFps.toInt()} FPS",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MintEmerald
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = ElevatedSlate
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Frame Time", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = String.format("%.1f ms", liveFrameTimeMs),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = ElectricCyan
                                )
                            }
                        }
                    }

                    Text(
                        text = "Target Refresh Rate Jendela Aplikasi",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = settings.preferredRefreshHz <= 0f,
                            onClick = { onSelectRefreshRate(0f) },
                            label = { Text("Otomatis (Max)") },
                            modifier = Modifier.testTag("hz_auto_chip"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanContainer,
                                selectedLabelColor = ElectricCyan
                            )
                        )
                        distinctRefreshRates.forEach { hz ->
                            val isSelected = settings.preferredRefreshHz.toInt() == hz.toInt()
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectRefreshRate(hz) },
                                label = { Text("${hz.toInt()} Hz") },
                                modifier = Modifier.testTag("hz_${hz.toInt()}_chip"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanContainer,
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.TouchApp,
                            contentDescription = "Uji Respons Sentuh",
                            tint = MintEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Uji Kelicinan & Sampling Sentuh",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                    }
                    TouchSmoothnessPad()
                }
            }
        }

        // RAM Management & Game Mode Card
        item {
            GlassTelemetryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                borderColor = MintEmerald.copy(alpha = 0.35f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionTitleRow(
                        title = "Manajemen RAM & Game Mode",
                        subtitle = "Bebas ${formatGigabytes(ram.availableBytes)} dari ${formatGigabytes(ram.totalBytes)}",
                        icon = Icons.Rounded.Memory,
                        iconTint = MintEmerald
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { ram.usagePercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = if (ram.usagePercent > 0.85f) SolarAmber else MintEmerald,
                            trackColor = ElevatedSlate
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Terpakai: ${formatGigabytes(ram.usedBytes)} (${(ram.usagePercent * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Cache Kernel: ${formatGigabytes(ram.cachedBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    AnimatedVisibility(visible = lastRamBoost != null) {
                        lastRamBoost?.let { boost ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldContainer.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, MintEmerald.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Boost Terakhir: ${boost.trimmedAppsCount} proses latar dipangkas • RAM bebas naik dari ${boost.beforeAvailMb} MB ke ${boost.afterAvailMb} MB (+${boost.freedMb} MB)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MintEmerald,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Profil Performa Gaming",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameProfile.entries.forEach { profile ->
                            val selected = settings.gameProfile == profile
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) CyanContainer.copy(alpha = 0.8f) else ElevatedSlate.copy(alpha = 0.65f),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) ElectricCyan else OutlineSubtle
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectGameProfile(profile) }
                                    .testTag("game_profile_${profile.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (selected) ElectricCyan else TextPrimary
                                        )
                                        Text(
                                            text = profile.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    if (selected) {
                                        Text(
                                            text = "AKTIF",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MintEmerald
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onBoostRam,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("boost_ram_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintEmerald,
                            contentColor = ObsidianVoid
                        )
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = "Bebaskan RAM")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bebaskan RAM Sekarang", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Optimized Games Space Header + Add Game Button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                SectionTitleRow(
                    title = "Ruang Game Terpilih",
                    subtitle = "${addedGames.size} game/app dipilih untuk di-optimize",
                    icon = Icons.Rounded.SportsEsports,
                    iconTint = SolarAmber,
                    trailing = {
                        Button(
                            onClick = {
                                onRefreshApps()
                                showAddGameSheet = true
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("add_game_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SolarAmber,
                                contentColor = ObsidianVoid
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Tambah Game",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah Game", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        // Quick suggestions if device has unadded CATEGORY_GAME apps
        if (suggestedGames.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Game Terdeteksi Otomatis (Ketuk untuk Tambah):",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestedGames.forEach { game ->
                            AssistChip(
                                onClick = { onToggleAddedGame(game) },
                                label = { Text("+ ${game.appName}") },
                                modifier = Modifier.testTag("suggest_game_${game.packageName}"),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = AmberContainer.copy(alpha = 0.7f),
                                    labelColor = SolarAmber
                                )
                            )
                        }
                    }
                }
            }
        }

        // Added Games List or Empty State
        if (addedGames.isEmpty()) {
            item {
                GlassTelemetryCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    borderColor = SolarAmber.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = "Belum Ada Game",
                            tint = SolarAmber,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "Belum ada game yang ditambahkan",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pilih game atau aplikasi favorit Anda untuk pembersihan RAM & cache otomatis sebelum bermain.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        OutlinedButton(
                            onClick = {
                                onRefreshApps()
                                showAddGameSheet = true
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("empty_add_game_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SolarAmber)
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Pilih Game",
                                tint = SolarAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pilih Game Sekarang", color = SolarAmber, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            items(addedGames, key = { it.packageName }) { app ->
                GlassTelemetryCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    borderColor = ElectricCyan.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldContainer
                                ) {
                                    Text(
                                        text = settings.gameProfile.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MintEmerald,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "RAM Trim + Cache Clean • ${if (settings.preferredRefreshHz > 0f) "${settings.preferredRefreshHz.toInt()} Hz" else "Max Hz"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                onBoostForGame(app)
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                if (launchIntent != null) {
                                    runCatching { context.startActivity(launchIntent) }
                                }
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("boost_launch_${app.packageName}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = ObsidianVoid
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = "Boost dan Buka ${app.appName}",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Boost", fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { onRemoveAddedGame(app) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("remove_game_${app.packageName}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Hapus ${app.appName}",
                                tint = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddGameSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var searchQuery by rememberSaveable { mutableStateOf("") }
        var onlyGamesFilter by rememberSaveable { mutableStateOf(false) }

        val filteredApps = remember(apps, searchQuery, onlyGamesFilter) {
            apps.filter { app ->
                val matchesFilter = !onlyGamesFilter || app.isGame
                val q = searchQuery.trim()
                val matchesSearch = q.isEmpty() ||
                    app.appName.contains(q, ignoreCase = true) ||
                    app.packageName.contains(q, ignoreCase = true)
                matchesFilter && matchesSearch
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showAddGameSheet = false },
            sheetState = sheetState,
            containerColor = DeepSpace,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pilih Game untuk Di-Optimize",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "${settings.addedGamePackages.size} terpilih dari ${apps.size} aplikasi terinstal",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onRefreshApps,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("refresh_apps_button")
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Muat Ulang Daftar Aplikasi",
                            tint = ElectricCyan
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama game atau package...") },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = "Cari Game",
                            tint = TextSecondary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_game_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = OutlineSubtle,
                        focusedContainerColor = ObsidianVoid,
                        unfocusedContainerColor = ObsidianVoid
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !onlyGamesFilter,
                        onClick = { onlyGamesFilter = false },
                        label = { Text("Semua Aplikasi (${apps.size})") },
                        modifier = Modifier.testTag("filter_all_apps"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanContainer,
                            selectedLabelColor = ElectricCyan
                        )
                    )
                    FilterChip(
                        selected = onlyGamesFilter,
                        onClick = { onlyGamesFilter = true },
                        label = { Text("Kategori Game (${apps.count { it.isGame }})") },
                        modifier = Modifier.testTag("filter_only_games"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberContainer,
                            selectedLabelColor = SolarAmber
                        )
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredApps.isEmpty()) {
                        item {
                            Text(
                                text = "Tidak ada game atau aplikasi yang cocok dengan pencarian.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 20.dp)
                            )
                        }
                    } else {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isAdded = app.packageName in settings.addedGamePackages
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAdded) CyanContainer.copy(alpha = 0.65f) else ElevatedSlate,
                                border = BorderStroke(
                                    1.dp,
                                    if (isAdded) ElectricCyan else Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleAddedGame(app) }
                                    .testTag("picker_app_${app.packageName}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = app.appName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (app.isGame) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = AmberContainer
                                                ) {
                                                    Text(
                                                        text = "GAME",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SolarAmber,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        imageVector = if (isAdded) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline,
                                        contentDescription = if (isAdded) "Sudah dipilih" else "Tambahkan ${app.appName}",
                                        tint = if (isAdded) MintEmerald else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { showAddGameSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("done_add_game_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = ObsidianVoid
                    )
                ) {
                    Text("Selesai (${settings.addedGamePackages.size} Dipilih)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
