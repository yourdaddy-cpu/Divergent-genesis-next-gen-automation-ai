package com.divergent.genesis

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val appTarget: String? = null,
    val isThinking: Boolean = false
)

@Composable
fun ChatScreen() {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val currentTask by AgentState.currentTask.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!msg.isUser && msg.appTarget != null) {
                        val icon = when {
                            msg.appTarget.contains("instagram") -> Icons.Default.CameraAlt
                            msg.appTarget.contains("youtube") -> Icons.Default.PlayArrow
                            msg.appTarget.contains("chrome") -> Icons.Default.Language
                            else -> Icons.Default.Android
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp).padding(end = 6.dp)
                        )
                    }
                    MessageBubble(msg)
                }
            }
        }

        if (currentTask != null) {
            Row(
                modifier = Modifier.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = NeonCyan,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(currentTask!!, color = NeonCyan, fontSize = 13.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Command Genesis...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = DarkGrey,
                    unfocusedContainerColor = DarkGrey
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.isNotBlank()) {
                        val cmd = input
                        messages.add(ChatMessage(cmd, true))
                        input = ""
                        scope.launch {
                            val thinkingMsg = ChatMessage("Thinking...", false, null, isThinking = true)
                            messages.add(thinkingMsg)
                            try {
                                val action = GenesisBrain.decide(cmd)
                                messages.remove(thinkingMsg)
                                val svc = GenesisAccessibilityService.instance
                                val target = action.optString("target", null)
                                val param = action.optString("param", null)
                                when (action.getString("action")) {
                                    "open_app" -> svc?.openApp(action.getString("target"))
                                    "open_url" -> {
                                        val url = action.getString("target")
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        svc?.startActivity(intent)
                                    }
                                    "tap_text" -> {
                                        val node = svc?.findByText(action.getString("target"))
                                        if (node != null) svc.tapNode(node)
                                    }
                                    "type" -> svc?.typeText(action.getString("target"))
                                    "swipe" -> svc?.swipe(
                                        action.getDouble("x1").toFloat(),
                                        action.getDouble("y1").toFloat(),
                                        action.getDouble("x2").toFloat(),
                                        action.getDouble("y2").toFloat()
                                    )
                                    "run_workflow" -> WorkflowEngine.runWorkflow(action.getString("target"), svc, param)
                                }
                                messages.add(ChatMessage("Done: ${action.getString("action")}", false, target))
                                AgentState.currentTask.value = null
                            } catch (e: Exception) {
                                messages.remove(thinkingMsg)
                                messages.add(ChatMessage("Error: ${e.message}", false))
                                AgentState.currentTask.value = null
                            }
                        }
                    }
                },
                modifier = Modifier.background(NeonCyan, RoundedCornerShape(50)).size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = DeepBlack)
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val bubbleColor = if (msg.isUser) NeonPurple.copy(alpha = 0.85f) else DarkGrey
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (msg.isUser) 18.dp else 4.dp,
        bottomEnd = if (msg.isUser) 4.dp else 18.dp
    )
    Surface(
        color = bubbleColor,
        shape = shape,
        border = if (!msg.isUser) BorderStroke(1.dp, GlassGrey) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            if (msg.isThinking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = NeonCyan,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(msg.text, color = Color.White, fontSize = 15.sp)
        }
    }
}
