package com.divergent.genesis

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

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
                value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(),
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
                    icon = { Icon(Icons.Default.Hub, contentDescription = "Brain") },
                    label = { Text("Neural Brain") }, selected = false,
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

// ============================================================
//  NEURAL BRAIN CANVAS  —  pan / pinch-zoom / tap / add / delete
// ============================================================
@Composable
fun BrainGraphScreen() {
    val nodes = BrainGraphManager.nodes
    var scale by remember { mutableStateOf(0.7f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var dialogParentId by remember { mutableStateOf<String?>(null) }
    var newLabel by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }

    val infinite = rememberInfiniteTransition()
    val pulse by infinite.animateFloat(
        initialValue = 0.85f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse)
    )

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.35f, 3.5f)
                        offset += pan
                    }
                }
                .pointerInput(nodes.size, scale, offset) {
                    detectTapGestures { tap ->
                        val cx = size.width / 2f + offset.x
                        val cy = size.height / 2f + offset.y
                        val hit = nodes.find {
                            val sx = cx + it.x * scale
                            val sy = cy + it.y * scale
                            val dx = sx - tap.x
                            val dy = sy - tap.y
                            (dx * dx + dy * dy) < (BrainGraphManager.getRadius(it.type) * scale * 2.5f).let { r -> r * r }
                        }
                        selectedNodeId = hit?.id
                    }
                }
        ) {
            val cx = size.width / 2f + offset.x
            val cy = size.height / 2f + offset.y

            // Connection lines
            nodes.forEach { node ->
                val parent = nodes.find { it.id == node.parentId }
                val endX = cx + node.x * scale
                val endY = cy + node.y * scale
                if (parent != null) {
                    val startX = cx + parent.x * scale
                    val startY = cy + parent.y * scale
                    drawLine(
                        color = BrainGraphManager.getColor(node.type).copy(alpha = 0.35f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2f * scale
                    )
                } else {
                    drawLine(
                        color = BrainGraphManager.getColor(node.type).copy(alpha = 0.20f),
                        start = Offset(cx, cy),
                        end = Offset(endX, endY),
                        strokeWidth = 2f * scale
                    )
                }
            }

            // Nodes
            nodes.forEach { node ->
                val sx = cx + node.x * scale
                val sy = cy + node.y * scale
                val color = BrainGraphManager.getColor(node.type)
                val r = BrainGraphManager.getRadius(node.type) * scale

                // Outer glow
                drawCircle(
                    color = color.copy(alpha = 0.18f),
                    radius = r * 2f * pulse,
                    center = Offset(sx, sy)
                )
                // Body
                drawCircle(color = color, radius = r, center = Offset(sx, sy))
                // Inner highlight
                drawCircle(color = Color.White.copy(alpha = 0.55f), radius = r * 0.3f, center = Offset(sx, sy))

                // Show label when zoomed enough OR when selected
                val isSelected = node.id == selectedNodeId
                if (scale > 1.4f || isSelected) {
                    drawContext.canvas.nativeCanvas.drawText(
                        node.label,
                        sx + r + 10f,
                        sy + 6f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 28f * scale.coerceAtMost(1.6f)
                            isAntiAlias = true
                        }
                    )
                }
            }

            // Genesis Core
            drawCircle(color = Color(0xFF8B0000).copy(alpha = 0.15f), radius = 90f * pulse * scale, center = Offset(cx, cy))
            drawCircle(color = Color(0xFF8B0000), radius = 45f * scale, center = Offset(cx, cy))
            drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 14f * scale, center = Offset(cx, cy))

            drawContext.canvas.nativeCanvas.drawText(
                "GENESIS",
                cx - 45f * scale,
                cy + 90f * scale,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.RED
                    textSize = 34f * scale.coerceAtMost(1.5f)
                    isAntiAlias = true
                    isFakeBoldText = true
                }
            )
        }

        // Floating controls
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    dialogParentId = null
                    newLabel = ""
                    newUrl = ""
                    showAddDialog = true
                },
                containerColor = NeonCyan
            ) { Icon(Icons.Default.Add, contentDescription = "Add Main Neuron", tint = DeepBlack) }

            FloatingActionButton(
                onClick = {
                    scale = 0.7f
                    offset = Offset.Zero
                },
                containerColor = DarkGrey
            ) { Icon(Icons.Default.CenterFocusStrong, contentDescription = "Recenter", tint = NeonCyan) }
        }

        // Selected node panel
        val selected = nodes.find { it.id == selectedNodeId }
        if (selected != null) {
            Card(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkGrey),
                border = BorderStroke(1.dp, BrainGraphManager.getColor(selected.type))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(selected.label, color = BrainGraphManager.getColor(selected.type), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Type: ${selected.type}", color = Color.Gray, fontSize = 12.sp)
                    selected.url?.let { Text("URL: $it", color = Color.White, fontSize = 12.sp) }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (selected.type != NodeType.MONO_MINI) {
                                    dialogParentId = selected.id
                                    newLabel = ""
                                    newUrl = ""
                                    showAddDialog = true
                                }
                            },
                            enabled = selected.type != NodeType.MONO_MINI,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) { Text("Add Child", color = DeepBlack) }

                        Button(
                            onClick = {
                                BrainGraphManager.deleteNode(selected.id)
                                selectedNodeId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))
                        ) { Text("Delete", color = Color.White) }
                    }
                }
            }
        }

        // Add-node dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = DarkGrey,
                title = { Text(if (dialogParentId == null) "Add Main Neuron" else "Add Child Neuron", color = NeonCyan) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newLabel, onValueChange = { newLabel = it },
                            label = { Text("Name (e.g., Twitter)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.Gray
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newUrl, onValueChange = { newUrl = it },
                            label = { Text("URL (optional)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.Gray
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val label = newLabel.trim()
                            val url = newUrl.trim().ifBlank { null }
                            if (label.isNotEmpty()) {
                                if (dialogParentId == null) {
                                    BrainGraphManager.addMainNode(label, url)
                                } else {
                                    BrainGraphManager.addChild(dialogParentId!!, label, url)
                                }
                            }
                            showAddDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) { Text("Add", color = DeepBlack) }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }
    }
}

// ============================================================
//  CHAT SCREEN  —  cleaner bubbles, thinking indicator
// ============================================================
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

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack).padding(horizontal = 16.dp, vertical = 8.dp)) {
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
                        Icon(
                            imageVector = when {
                                msg.appTarget.contains("instagram") -> Icons.Default.CameraAlt
                                msg.appTarget.contains("youtube") -> Icons.Default.PlayArrow
                                msg.appTarget.contains("chrome") -> Icons.Default.Language
                                else -> Icons.Default.Android
                            },
                            contentDescription = null, tint = NeonCyan,
                            modifier = Modifier.size(22.dp).padding(end = 6.dp)
                        )
                    }
                    Surface(
                        color = if (msg.isUser) NeonPurple.copy(alpha = 0.85f) else DarkGrey,
                        shape = RoundedCornerShape(
                            topStart = 18.dp, topEnd = 18.dp,
                            bottomStart = if (msg.isUser) 18.dp else 4.dp,
                            bottomEnd = if (msg.isUser) 4.dp else 18.dp
                        ),
                        border = if (!msg.isUser) BorderStroke(1.dp, GlassGrey) else null
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                            if (msg.isThinking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = NeonCyan, strokeWidth = 2.dp
                                )
                     
