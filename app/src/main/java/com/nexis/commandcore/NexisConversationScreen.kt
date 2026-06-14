package com.nexis.commandcore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConversationScreen(
    entries: List<NexisMemoryEntry>,
    onSaveCapture: (String) -> Unit
) {
    var mode by remember { mutableStateOf(NexisChatMode.COMPANION) }
    var input by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(emptyList<NexisChatMessage>()) }

    fun send() {
        val clean = input.trim()
        if (clean.isBlank()) return
        val activeMode = mode
        val now = System.currentTimeMillis()
        val user = NexisChatMessage(now, "user", clean, now, activeMode)
        val nexis = NexisChatMessage(now + 1, "nexis", NexisConversationEngine.reply(clean, activeMode, entries.size), now + 1, activeMode)
        messages = messages + user + nexis
        input = ""
    }

    fun saveLatest() {
        val latest = messages.takeLast(2)
        if (latest.isEmpty()) return
        val body = buildString {
            appendLine("Conversational Core Save")
            appendLine("Mode: ${mode.label}")
            appendLine()
            latest.forEach {
                appendLine("${it.role.uppercase()}:")
                appendLine(it.text)
                appendLine()
            }
            if (mode == NexisChatMode.CODE_REVIEW) {
                appendLine("code kotlin gradle manifest review-first")
                appendLine("Review-first only. Nexis stores code ideas but does not execute them.")
            }
        }
        onSaveCapture(body)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChatCard {
            Text("Conversational Core", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Text("Talk to Nexis like a companion, build partner, strategist, or code reviewer.", color = Color(0xFFD9D2FF), fontSize = 14.sp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NexisChatMode.values().forEach { item ->
                    val active = item == mode
                    TextButton(
                        onClick = { mode = item },
                        modifier = Modifier
                            .border(1.dp, if (active) Color(0xFF00E5FF) else Color(0x337C4DFF), RoundedCornerShape(18.dp))
                            .background(if (active) Color(0x3315E4FF) else Color(0x11111025), RoundedCornerShape(18.dp))
                    ) {
                        Text(item.label, color = if (active) Color.White else Color(0xFFB9AEE8))
                    }
                }
            }

            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    mode = NexisConversationEngine.detectMode(it)
                },
                label = { Text("Message Nexis") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { send() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF), contentColor = Color.White),
                    shape = RoundedCornerShape(18.dp)
                ) { Text("Send", fontWeight = FontWeight.Bold) }

                Button(
                    onClick = { saveLatest() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25213D), contentColor = Color.White),
                    shape = RoundedCornerShape(18.dp)
                ) { Text("Save exchange", fontWeight = FontWeight.Bold) }
            }

            Text("Local-first draft mode. Provider keys are optional later and never hardcoded.", color = Color(0xFFFFD166), fontSize = 12.sp)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages, key = { it.id }) { message ->
                val isUser = message.role == "user"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isUser) Color(0x5525365C) else Color(0x66111025)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            if (isUser) "YOU · ${message.mode.label}" else "NEXIS · ${message.mode.label}",
                            color = if (isUser) Color(0xFF00E5FF) else Color(0xFFFF4FD8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(message.text, color = Color(0xFFEDE7FF), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x337C4DFF), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA111025)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}
