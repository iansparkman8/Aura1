package com.nexis.commandcore

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class NexisProviderSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("nexis_provider_bridge", Context.MODE_PRIVATE)
    private val providers = listOf("GPT", "Gemini", "Claude", "Grok", "Local Mock")

    fun load(): Map<String, Boolean> = providers.associateWith { prefs.getBoolean(it, it == "Local Mock") }

    fun setEnabled(provider: String, enabled: Boolean) {
        prefs.edit().putBoolean(provider, enabled).apply()
    }
}

@Composable
fun ProviderBridgeScreen() {
    val context = LocalContext.current
    val store = remember { NexisProviderSettingsStore(context) }
    val enabled = remember { mutableStateMapOf<String, Boolean>().apply { putAll(store.load()) } }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProviderCard {
            Text("Provider Bridge", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Text(
                "Architecture for GPT, Gemini, Claude, Grok, and local mock mode. This screen stores visible toggles only. No network calls. No hardcoded keys.",
                color = Color(0xFFD9D2FF),
                fontSize = 14.sp
            )
        }

        enabled.keys.forEach { provider ->
            val isOn = enabled[provider] == true
            ProviderCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(provider, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            if (provider == "Local Mock") "Safe offline conversation engine." else "Reserved for future user-owned API key connection.",
                            color = Color(0xFFB9AEE8),
                            fontSize = 13.sp
                        )
                    }
                    Button(
                        onClick = {
                            enabled[provider] = !isOn
                            store.setEnabled(provider, !isOn)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOn) Color(0xFF00A8C6) else Color(0xFF25213D),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(if (isOn) "ON" else "OFF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        ProviderCard {
            Text("Provider Rules", color = Color(0xFFFFD166), fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("• User-owned keys only", color = Color(0xFFEDE7FF))
            Text("• Visible toggle per provider", color = Color(0xFFEDE7FF))
            Text("• No hardcoded secrets", color = Color(0xFFEDE7FF))
            Text("• No hidden repo patching", color = Color(0xFFEDE7FF))
            Text("• No runtime code execution", color = Color(0xFFEDE7FF))
        }
    }
}

@Composable
private fun ProviderCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x337C4DFF), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA111025)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            content()
        }
    }
}
