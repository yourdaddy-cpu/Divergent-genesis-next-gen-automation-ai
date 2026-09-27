package com.divergent.genesis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

val NeonCyan = Color(0xFF00FFFF)
val NeonPurple = Color(0xFFBF00FF)
val DeepBlack = Color(0xFF050505)
val DarkGrey = Color(0xFF1A1A1A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = DeepBlack, surface = DarkGrey)) {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = {
                        NavigationBar(containerColor = DarkGrey) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Share, contentDescription = "Brain") },
                                label = { Text("Brain Graph") },
                                selected = false,
                                onClick = { navController.navigate("graph") }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                                label = { Text("Genesis Chat") },
                                selected = true,
                                onClick = { navController.navigate("chat") }
                            )
                        }
                    }
                ) { padding ->
                    NavHost(navController = navController, startDestination = "chat", modifier = Modifier.padding(padding)) {
                        composable("graph") { BrainGraphScreen() }
                        composable("chat") { ChatScreen() }
                    }
                }
            }
        }
    }
}

@Composable
fun BrainGraphScreen() {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse)
    )

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val nodes = listOf(
                Offset(center.x - 300f, center.y - 400f),
                Offset(center.x + 300f, center.y - 300f),
                Offset(center.x - 400f, center.y + 200f),
                Offset(center.x + 250f, center.y + 350f),
                Offset(center.x, center.y - 500f)
            )

            nodes.forEach { node ->
                drawLine(color = NeonCyan.copy(alpha = 0.3f), start = center, end = node, strokeWidth = 3f)
                drawCircle(color = NeonPurple, radius = 15f, center = node)
                drawCircle(color = NeonPurple.copy(alpha = 0.2f), radius = 25f * pulse, center = node)
            }

            drawCircle(color = NeonCyan, radius = 30f * pulse, center = center)
            drawCircle(color = NeonCyan.copy(alpha = 0.1f), radius = 60f * pulse, center = center, style = Stroke(width = 2f))
        }
        Text("CURRENT STATE: GENESIS CORE", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 50.dp))
    }
}

data class ChatMessage(val text: String, val isUser: Boolean, val appTarget: String? = null)

@Composable
fun ChatScreen() {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val currentTask by AgentState.currentTask.collectAsState()
    val currentApp by AgentState.currentTargetApp.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack).padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f), reverseLayout = true) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!msg.isUser && msg.appTarget != null) {
                        Icon(
                            imageVector = when {
                                msg.appTarget.contains("instagram") -> Icons.Default.CameraAlt
                                msg.appTarget.contains("youtube") -> Icons.Default.PlayArrow
                                msg.appTarget.contains("chrome") -> Icons.Default.Language
                                else -> Icons.Default.Android
                            },
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp).align(Alignment.CenterVertically).padding(end = 8.dp)
                        )
                    }
                    Surface(color = if (msg.isUser) NeonPurple else DarkGrey, shape = RoundedCornerShape(12.dp)) {
                        Text(msg.text, color = Color.White, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }

        if (currentTask != null) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(currentTask!!, color = NeonCyan, fontSize = 14.sp)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Command Genesis...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.DarkGray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.isNotBlank()) {
                        val cmd = input
                        messages.add(ChatMessage(cmd, true))
                        input = ""
                        scope.launch {
                            try {
                                val action = GenesisBrain.decide(cmd)
                                val svc = GenesisAccessibilityService.instance
                                when (action.getString("action")) {
                                    "open_app" -> svc?.openApp(action.getString("target"))
                                    "tap_text" -> svc?.findByText(action.getString("target"))?.let { svc.tapNode(it) }
                                    "type" -> svc?.typeText(action.getString("target"))
                                    "swipe" -> svc?.swipe(action.getDouble("x1").toFloat(), action.getDouble("y1").toFloat(), action.getDouble("x2").toFloat(), action.getDouble("y2").toFloat())
                                }
                                messages.add(ChatMessage("Task executed: ${action.getString("action")}", false, action.optString("target", null)))
                                AgentState.currentTask.value = null
                            } catch (e: Exception) {
                                messages.add(ChatMessage("Error: ${e.message}", false))
                                AgentState.currentTask.value = null
                            }
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = NeonCyan)
            }
        }
    }
}
