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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, content = content)
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
    val context = LocalContext.current

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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Header(entries.size, onStartOverlay, onRequestOverlay)
                TabRow(tab) { tab = it }
                when (tab) {
                    "Core" -> CoreScreen(coach, entries, onVoice = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell Nexis what to remember or build.")
                        }
                        voiceLauncher.launch(intent)
                    })
                    "Capture" -> CaptureScreen(input, { input = it }, saveCapture, coach)
                    "Vault" -> VaultScreen(entries, onDelete = { id -> entries = store.remove(id) }, onClear = { entries = store.clear() })
                    "Build" -> BuildLabScreen()
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Local-first. Review-only. No Accessibility Service. No screenshots. No auto-running code.",
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
    val tabs = listOf("Core", "Capture", "Vault", "Build")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEach { tab ->
            val active = tab == selected
            TextButton(
                onClick = { onSelect(tab) },
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, if (active) Color(0xFF00E5FF) else Color(0x337C4DFF), RoundedCornerShape(18.dp))
                    .background(if (active) Color(0x2215E4FF) else Color(0x11111025), RoundedCornerShape(18.dp))
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
                Text("What Nexis is built to solve", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "You do not need another blank notes app. You need a living command center that catches the wild ideas, keeps code changes safe, turns stress into one next action, and makes the phone feel like it has a soul.",
                    color = Color(0xFFD9D2FF),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureScreen(
    input: String,
    onInput: (String) -> Unit,
    onSave: (String) -> Unit,
    coach: NexisCoachResponse
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("Capture anything", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Ideas, code, app fixes, worries, wins, plans. Nexis classifies it and gives the next move.", color = Color(0xFFB9AEE8), fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = input,
                onValueChange = onInput,
                label = { Text("Tell Nexis") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            GlowButton("Save to Nexis") { onSave(input) }
        }
        GlassCard {
            Text("Latest read", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(coach.headline, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(coach.nextStep, color = Color(0xFFD9D2FF), fontSize = 14.sp)
        }
    }
}

@Composable
private fun VaultScreen(entries: List<NexisMemoryEntry>, onDelete: (Long) -> Unit, onClear: () -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Memory Vault", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                TextButton(onClick = onClear) { Text("Clear", color = Color(0xFFFF8A80)) }
            }
        }
        items(entries, key = { it.id }) { entry ->
            MemoryCard(entry, onDelete)
        }
    }
}

@Composable
private fun BuildLabScreen() {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("Build Lab", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("This is where code ideas belong before they touch the repo. Nexis treats every upgrade as review-first.", color = Color(0xFFD9D2FF), fontSize = 14.sp)
        }
        BuildRule("1", "Paste code", "Save the file path, purpose, and expected behavior. Do not run code from chat inside the app.")
        BuildRule("2", "Patch repo", "Apply changes in GitHub/Codespace or Android Studio where the diff can be reviewed.")
        BuildRule("3", "Build APK", "Run gradle :app:assembleDebug or trigger the included GitHub Actions workflow.")
        BuildRule("4", "Connect AI safely", "Provider keys are user-owned, never hardcoded. No hidden network calls in this starter build.")
    }
}

@Composable
private fun BuildRule(number: String, title: String, body: String) {
    GlassCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0x337C4DFF), CircleShape)
                    .border(1.dp, Color(0xFF00E5FF), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(number, color = Color.White, fontWeight = FontWeight.Bold) }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.type.label.uppercase(), color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (entry.pinned) {
                        Spacer(Modifier.width(8.dp))
                        Text("PINNED", color = Color(0xFFFFD166), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0x66111025)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(label, color = Color(0xFFB9AEE8), fontSize = 12.sp)
        }
    }
}

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x337C4DFF), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA111025)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}

@Composable
private fun GlowButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF), contentColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) { Text(label, fontWeight = FontWeight.Bold) }
}

@Composable
private fun NexisBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color(0x553A145F), Color(0xFF080713)),
                center = Offset(size.width * 0.5f, size.height * 0.18f),
                radius = size.maxDimension * 0.75f
            )
        )
        for (i in 0 until 24) {
            val x = size.width * ((i * 37 % 100) / 100f)
            val y = size.height * ((i * 61 % 100) / 100f)
            drawCircle(Color(0x334DDCFF), radius = 1.5f + (i % 4), center = Offset(x, y))
        }
    }
}

@Composable
private fun AnimatedNexisAvatar() {
    val transition = rememberInfiniteTransition(label = "nexis")
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "pulse"
    )
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(5600), RepeatMode.Restart),
        label = "orbit"
    )

    Canvas(modifier = Modifier.size(236.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = min(size.width, size.height) * 0.23f * pulse
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0x8800E5FF), Color.Transparent), center = c, radius = r * 3.0f),
            radius = r * 2.8f,
            center = c
        )
        drawCircle(Color(0x447C4DFF), radius = r * 1.8f, center = c, style = Stroke(width = 4.dp.toPx()))
        drawCircle(Color(0x66FF4FD8), radius = r * 2.15f, center = c, style = Stroke(width = 2.dp.toPx()))
        repeat(12) { i ->
            val angle = orbit + i * (PI.toFloat() * 2f / 12f)
            val orbitRadius = r * (1.7f + (i % 3) * 0.19f)
            val dot = Offset(c.x + cos(angle) * orbitRadius, c.y + sin(angle) * orbitRadius)
            drawCircle(if (i % 2 == 0) Color(0xFF00E5FF) else Color(0xFFFF4FD8), radius = 3.dp.toPx() + i % 3, center = dot)
        }
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFEDE7FF), Color(0xFF7C4DFF), Color(0xFF140B30)), center = Offset(c.x - r * 0.3f, c.y - r * 0.4f), radius = r * 1.35f),
            radius = r,
            center = c
        )
        val eyeY = c.y - r * 0.15f
        val eyeX = r * 0.32f
        drawCircle(Color.White, radius = r * 0.12f, center = Offset(c.x - eyeX, eyeY))
        drawCircle(Color.White, radius = r * 0.12f, center = Offset(c.x + eyeX, eyeY))
        drawCircle(Color(0xFF080713), radius = r * 0.055f, center = Offset(c.x - eyeX, eyeY))
        drawCircle(Color(0xFF080713), radius = r * 0.055f, center = Offset(c.x + eyeX, eyeY))
        drawArc(
            color = Color(0xFFFFD166),
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(c.x - r * 0.35f, c.y + r * 0.16f),
            size = androidx.compose.ui.geometry.Size(r * 0.7f, r * 0.38f),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

private fun formatDate(epoch: Long): String = SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(epoch))
