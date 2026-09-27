package com.divergent.genesis

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

val NeonCyan = Color(0xFF00FFFF)
val NeonPurple = Color(0xFFBF00FF)
val DeepBlack = Color(0xFF020205)
val DarkGrey = Color(0xFF121216)
val GlassGrey = Color(0x33FFFFFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = DeepBlack, surface = DarkGrey)) {
                val prefs = getSharedPreferences("genesis_prefs", Context.MODE_PRIVATE)
                var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }

                if (apiKey.isEmpty()) {
                    ApiKeyScreen(onSave = { key ->
                        prefs.edit().putString("api_key", key).apply()
                        apiKey = key
                    })
                } else {
                    GenesisApp(apiKey, onLogout = {
                        prefs.edit().remove("api_key").apply()
                        apiKey = ""
                    })
                }
            }
        }
    }
}

@Composable
fun ApiKeyScreen(onSave: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(colors = listOf(Color(0xFF1A1A2E), DeepBlack), radius = 1500f)
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Psychology, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("DIVERGENT GENESIS", color = NeonCyan, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Initialize your neural link", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter OpenRouter API Key", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = NeonPurple) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { if (input.isNotBlank()) onSave(input) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("INITIALIZE", color = DeepBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// FIX: Added OptIn for Material3 Experimental APIs (TopAppBar)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenesisApp(apiKey: String, onLogout: () -> Unit) {
    val navController = rememberNavController()
    GenesisBrain.apiKey = apiKey

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Divergent Genesis", color = NeonCyan, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlack)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = DarkGrey) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Share, contentDescription = "Brain") },
                    label = { Text("Brain Graph") }, selected = false,
                    onClick = { navController.navigate("graph") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                    label = { Text("Genesis Chat") }, selected = true,
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

@Composable
fun BrainGraphScreen() {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse)
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart)
    )

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val nodeCount = 12
            val radius = 350f

            for (i in 0 until nodeCount) {
                val angle = Math.toRadians((i * (360 / nodeCount) + rotation).toDouble())
                val x = center.x + radius * cos(angle).toFloat()
                val y = center.y + radius * sin(angle).toFloat()
                val nodePos = Offset(x, y)

                drawLine(color = NeonCyan.copy(alpha = 0.15f), start = center, end = nodePos, strokeWidth = 2f)
                drawCircle(color = NeonPurple, radius = 12f, center = nodePos)
                drawCircle(color = NeonPurple.copy(alpha = 0.3f), radius = 20f * pulse, center = nodePos, style = Stroke(width = 2f))
            }

            drawCircle(color = NeonCyan.copy(alpha = 0.1f), radius = 100f * pulse, center = center)
            drawCircle(color = NeonCyan, radius = 25f * pulse, center = center)
            drawCircle(color = Color.White, radius = 10f, center = center)
        }
        Text(
            "CURRENT STATE: GENESIS CORE", color = NeonCyan, fontSize = 14.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }
}

data class ChatMessage(val text: String, val isUser: Boolean, val appTarget: String? = null)

@Composable
fun ChatScreen() {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val currentTask by AgentState.currentTask.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack).padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f), reverseLayout = true, contentPadding = PaddingValues(bottom = 16.dp)) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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
                            contentDescription = null, tint = NeonCyan,
                            modifier = Modifier.size(20.dp).align(Alignment.CenterVertically).padding(end = 8.dp)
                        )
                    }
                    Surface(
                        color = if (msg.isUser) NeonPurple.copy(alpha = 0.8f) else DarkGrey,
                        shape = RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = if (msg.isUser) 16.dp else 4.dp,
                            bottomEnd = if (msg.isUser) 4.dp else 16.dp
                        ),
                        border = if (!msg.isUser) BorderStroke(1.dp, GlassGrey) else null
                    ) {
                        Text(msg.text, color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 15.sp)
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
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Command Genesis...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedContainerColor = DarkGrey, unfocusedContainerColor = DarkGrey
                ),
                shape = RoundedCornerShape(24.dp)
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
                                    "tap_text" -> {
                                        val text = action.getString("target")
                                        val node = svc?.findByText(text)
                                        if (node != null) svc.tapNode(node)
                                    }
                                    "type" -> svc?.typeText(action.getString("target"))
                                    "swipe" -> svc?.swipe(
                                        action.getDouble("x1").toFloat(), action.getDouble("y1").toFloat(),
                                        action.getDouble("x2").toFloat(), action.getDouble("y2").toFloat()
                                    )
                                    "run_workflow" -> {
                                        WorkflowEngine.runWorkflow(action.getString("target"), svc)
                                    }
                                }
                                messages.add(ChatMessage("Task executed: ${action.getString("action")}", false, action.optString("target", null)))
                                AgentState.currentTask.value = null
                            } catch (e: Exception) {
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
