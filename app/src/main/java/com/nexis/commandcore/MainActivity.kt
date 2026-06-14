package com.nexis.commandcore

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private lateinit var memoryStore: NexisMemoryStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        memoryStore = NexisMemoryStore(this)
        requestNotificationPermissionIfNeeded()
        setContent {
            NexisTheme {
                NexisApp(
                    store = memoryStore,
                    onStartOverlay = { startNexisOverlay() },
                    onRequestOverlay = { startActivity(NexisOverlayService.overlaySettingsIntent(this)) }
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200)
            }
        }
    }

    private fun startNexisOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(NexisOverlayService.overlaySettingsIntent(this))
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, NexisOverlayService::class.java))
    }
}

@Composable
private fun NexisTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Color(0xFF00E5FF),
        secondary = Color(0xFFFF4FD8),
        tertiary = Color(0xFFFFD166),
        background = Color(0xFF080713),
        surface = Color(0xFF111025),
        onPrimary = Color(0xFF06121A),
        onSecondary = Color.White,
        onBackground = Color(0xFFEDE7FF),
        onSurface = Color(0xFFEDE7FF)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun NexisApp(
    store: NexisMemoryStore,
    onStartOverlay: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    var entries by remember { mutableStateOf(store.load()) }
    var tab by remember { mutableStateOf("Core") }
    var input by remember { mutableStateOf("") }
    var coach by remember { mutableStateOf(NexisBrain.coach("Build Nexis", entries.size)) }

    val saveCapture: (String) -> Unit = { text ->
        val trimmed = text.trim()
        if (trimmed.isNotBlank()) {
            val response = NexisBrain.coach(trimmed, entries.size)
            entries = store.add(NexisBrain.titleFor(trimmed), trimmed, response.entryType)
            coach = response
            input = ""
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) saveCapture(spoken)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF080713)) {
        Box(modifier = Modifier.fillMaxSize()) {
            NexisBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Header(entries.size, onStartOverlay, onRequestOverlay)
                TabRow(tab) { tab = it }

                Box(modifier = Modifier.weight(1f)) {
                    when (tab) {
                        "Core" -> CoreScreen(coach, entries, onVoice = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell Nexis what to remember or build.")
                            }
                            voiceLauncher.launch(intent)
                        })
                        "Chat" -> ConversationScreen(entries = entries, onSaveCapture = saveCapture)
                        "Capture" -> CaptureScreen(input, { input = it }, saveCapture, coach)
                        "Vault" -> VaultScreen(entries, onDelete = { id -> entries = store.remove(id) }, onClear = { entries = store.clear() })
                        "Build" -> BuildLabScreen()
                        "Providers" -> ProviderBridgeScreen()
                    }
                }

                Text(
                    "Local-first. Review-only. No Accessibility Service. No screenshots. No auto-running code.",
                    color = Color(0xFFB9AEE8),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun Header(vaultSize: Int, onStartOverlay: () -> Unit, onRequestOverlay: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("NEXIS", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Command Core · $vaultSize saved sparks", color = Color(0xFF00E5FF), fontSize = 13.sp)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Awaken") { onStartOverlay() }
            TextButton(onClick = onRequestOverlay) { Text("Overlay permission", color = Color(0xFFFFD166), fontSize = 12.sp) }
        }
    }
}

@Composable
private fun TabRow(selected: String, onSelect: (String) -> Unit) {
    val tabs = listOf("Core", "Chat", "Capture", "Vault", "Build", "Providers")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            val active = tab == selected
            TextButton(
                onClick = { onSelect(tab) },
                modifier = Modifier
                    .border(1.dp, if (active) Color(0xFF00E5FF) else Color(0x337C4DFF), RoundedCornerShape(18.dp))
                    .background(if (active) Color(0x2215E4FF) else Color(0x11111025), RoundedCornerShape(18.dp))
                    .padding(horizontal = 4.dp)
            ) {
                Text(tab, color = if (active) Color.White else Color(0xFFB9AEE8), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CoreScreen(coach: NexisCoachResponse, entries: List<NexisMemoryEntry>, onVoice: () -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            GlassCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    AnimatedNexisAvatar()
                    Text(coach.headline, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(coach.nextStep, color = Color(0xFFD9D2FF), fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    GlowButton("Voice capture") { onVoice() }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Focus", "1 step", Modifier.weight(1f))
                StatCard("Vault", entries.size.toString(), Modifier.weight(1f))
                StatCard("Mode", coach.entryType.label, Modifier.weight(1f))
            }
        }
        item {
            GlassCard {
                Text("New feature stack", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Chat Core · Provider Bridge · Build Doctor · Premium Avatar · Vault Export Preview", color = Color(0xFFD9D2FF), fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureScreen(input: String, onInput: (String) -> Unit, onSave: (String) -> Unit, coach: NexisCoachResponse) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("Capture anything", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Ideas, code, app fixes, worries, wins, plans. Nexis classifies it and gives the next move.", color = Color(0xFFB9AEE8), fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = input, onValueChange = onInput, label = { Text("Tell Nexis") }, minLines = 6, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            GlowButton("Save to Nexis") { onSave(input) }
        }
        GlassCard {
            Text("Latest read", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
            Text(coach.headline, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(coach.nextStep, color = Color(0xFFD9D2FF), fontSize = 14.sp)
        }
    }
}

@Composable
private fun VaultScreen(entries: List<NexisMemoryEntry>, onDelete: (Long) -> Unit, onClear: () -> Unit) {
    var exportPreview by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Memory Vault", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                TextButton(onClick = onClear) { Text("Clear", color = Color(0xFFFF8A80)) }
            }
        }
        item {
            GlassCard {
                Text("Vault Export Preview", color = Color(0xFFFFD166), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("Create a plain-text export preview of saved sparks. No file permission needed in this pass.", color = Color(0xFFD9D2FF), fontSize = 13.sp)
                GlowButton("Generate preview") {
                    exportPreview = entries.joinToString("\n\n") {
                        "[${it.type.label}] ${it.title}\n${it.body}\n${formatDate(it.createdAt)}"
                    }.ifBlank { "Vault is empty." }
                }
                if (exportPreview.isNotBlank()) {
                    Text(exportPreview.take(1800), color = Color(0xFFEDE7FF), fontSize = 12.sp)
                }
            }
        }
        items(entries, key = { it.id }) { entry -> MemoryCard(entry, onDelete) }
    }
}

@Composable
private fun BuildLabScreen() {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("Build Lab", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Paste code, app ideas, crash logs, or upgrade plans here before they touch the repo.", color = Color(0xFFD9D2FF), fontSize = 14.sp)
        }

        BuildRule("1", "Chat → Plan", "Use Chat Core to turn ideas into exact file targets and build steps.")
        BuildRule("2", "Plan → Patch", "Apply the smallest safe change in Codespace.")
        BuildRule("3", "Patch → Build", "Run gradle :app:assembleDebug --stacktrace.")
        BuildRule("4", "Build → Artifact", "Find app-debug.apk in app/build/outputs/apk/debug.")
        BuildRule("5", "Artifact → Install", "Only install APKs built from reviewed code.")

        GlassCard {
            Text("Build Doctor", color = Color(0xFF00E5FF), fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("If build fails, run:", color = Color(0xFFD9D2FF))
            Text("gradle :app:assembleDebug --stacktrace 2>&1 | tee build.log", color = Color(0xFFFFD166), fontSize = 12.sp)
            Text("Then inspect the first 'What went wrong' block. Fix the first real error, not the last stacktrace line.", color = Color(0xFFB9AEE8), fontSize = 13.sp)
        }

        GlassCard {
            Text("Provider Bridge Rules", color = Color(0xFFFFD166), fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("GPT · Gemini · Claude · Grok · Local Mock", color = Color(0xFFEDE7FF))
            Text("User-owned keys only. No hardcoded secrets. No automatic repo patching. No runtime code execution.", color = Color(0xFFB9AEE8), fontSize = 13.sp)
        }
    }
}

@Composable
private fun BuildRule(number: String, title: String, body: String) {
    GlassCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(38.dp).background(Color(0x337C4DFF), CircleShape).border(1.dp, Color(0xFF00E5FF), CircleShape), contentAlignment = Alignment.Center) {
                Text(number, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(body, color = Color(0xFFB9AEE8), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun MemoryCard(entry: NexisMemoryEntry, onDelete: (Long) -> Unit) {
    GlassCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.type.label.uppercase(), color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(entry.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(entry.body, color = Color(0xFFD9D2FF), maxLines = 4, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                Text(formatDate(entry.createdAt), color = Color(0xFF8B82B8), fontSize = 11.sp)
            }
            TextButton(onClick = { onDelete(entry.id) }) { Text("Delete", color = Color(0xFFFF8A80)) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0x66111025)), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(label, color = Color(0xFFB9AEE8), fontSize = 12.sp)
        }
    }
}

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x337C4DFF), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA111025)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { content() }
    }
}

@Composable
private fun GlowButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF), contentColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NexisBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Brush.radialGradient(listOf(Color(0x553A145F), Color(0xFF080713)), center = Offset(size.width * 0.5f, size.height * 0.18f), radius = size.maxDimension * 0.75f))
        for (i in 0 until 32) {
            val x = size.width * ((i * 37 % 100) / 100f)
            val y = size.height * ((i * 61 % 100) / 100f)
            drawCircle(Color(0x334DDCFF), radius = 1.5f + (i % 4), center = Offset(x, y))
        }
    }
}

@Composable
private fun AnimatedNexisAvatar() {
    val transition = rememberInfiniteTransition(label = "premiumNexis")
    val pulse by transition.animateFloat(0.88f, 1.13f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "pulse")
    val orbit by transition.animateFloat(0f, (PI * 2).toFloat(), infiniteRepeatable(tween(5200), RepeatMode.Restart), label = "orbit")
    val blink by transition.animateFloat(1f, 0.12f, infiniteRepeatable(tween(2100), RepeatMode.Reverse), label = "blink")

    Canvas(modifier = Modifier.size(260.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val base = min(size.width, size.height) * 0.22f
        val r = base * pulse

        drawCircle(Brush.radialGradient(listOf(Color(0x6600E5FF), Color(0x334F2DFF), Color.Transparent), c, r * 3.6f), r * 3.35f, c)

        drawOval(Color(0x224DDCFF), Offset(c.x - r * 2.45f, c.y - r * 0.82f), Size(r * 1.95f, r * 1.65f))
        drawOval(Color(0x22FF4FD8), Offset(c.x + r * 0.50f, c.y - r * 0.82f), Size(r * 1.95f, r * 1.65f))

        repeat(3) { ring ->
            drawCircle(listOf(Color(0x887C4DFF), Color(0x8800E5FF), Color(0x66FF4FD8))[ring], r * (1.45f + ring * 0.34f), c, style = Stroke(width = (3 - ring).dp.toPx()))
        }

        repeat(18) { i ->
            val angle = orbit * if (i % 2 == 0) 1f else -1f + i * (PI.toFloat() * 2f / 18f)
            val orbitRadius = r * (1.72f + (i % 4) * 0.13f)
            val dot = Offset(c.x + cos(angle) * orbitRadius, c.y + sin(angle) * orbitRadius)
            drawCircle(if (i % 3 == 0) Color(0xFFFFD166) else if (i % 2 == 0) Color(0xFF00E5FF) else Color(0xFFFF4FD8), (2.3f + (i % 3)).dp.toPx(), dot)
        }

        drawOval(Color(0x55000000), Offset(c.x - r * 0.85f, c.y + r * 1.20f), Size(r * 1.70f, r * 0.32f))

        drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFEDE7FF), Color(0xFF7C4DFF), Color(0xFF120A2B)), Offset(c.x - r * 0.32f, c.y - r * 0.45f), r * 1.45f), r, c)

        drawCircle(Color(0xAAFFFFFF), r * 0.18f, Offset(c.x - r * 0.35f, c.y - r * 0.45f))

        val eyeY = c.y - r * 0.15f
        val eyeDx = r * 0.34f
        val eyeOpen = (r * 0.13f * blink).coerceAtLeast(2f)

        drawOval(Color.White, Offset(c.x - eyeDx - r * 0.13f, eyeY - eyeOpen), Size(r * 0.26f, eyeOpen * 2f))
        drawOval(Color.White, Offset(c.x + eyeDx - r * 0.13f, eyeY - eyeOpen), Size(r * 0.26f, eyeOpen * 2f))
        drawCircle(Color(0xFF080713), r * 0.055f, Offset(c.x - eyeDx, eyeY))
        drawCircle(Color(0xFF080713), r * 0.055f, Offset(c.x + eyeDx, eyeY))
        drawCircle(Color(0xFF00E5FF), r * 0.022f, Offset(c.x - eyeDx + r * 0.025f, eyeY - r * 0.025f))
        drawCircle(Color(0xFF00E5FF), r * 0.022f, Offset(c.x + eyeDx + r * 0.025f, eyeY - r * 0.025f))

        drawArc(Color(0xFFFFD166), 18f, 144f + (pulse - 1f) * 70f, false, Offset(c.x - r * 0.34f, c.y + r * 0.16f), Size(r * 0.68f, r * 0.38f), style = Stroke(width = 3.dp.toPx()))

        drawLine(Color(0xFFFFD166), Offset(c.x - r * 0.36f, c.y - r * 0.98f), Offset(c.x - r * 0.18f, c.y - r * 1.30f), strokeWidth = 3.dp.toPx())
        drawLine(Color(0xFFFFD166), Offset(c.x, c.y - r * 1.03f), Offset(c.x, c.y - r * 1.42f), strokeWidth = 3.dp.toPx())
        drawLine(Color(0xFFFFD166), Offset(c.x + r * 0.36f, c.y - r * 0.98f), Offset(c.x + r * 0.18f, c.y - r * 1.30f), strokeWidth = 3.dp.toPx())
        drawCircle(Color(0xFFFFF1A6), r * 0.055f, Offset(c.x, c.y - r * 1.45f))
    }
}

private fun formatDate(epoch: Long): String = SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(epoch))
