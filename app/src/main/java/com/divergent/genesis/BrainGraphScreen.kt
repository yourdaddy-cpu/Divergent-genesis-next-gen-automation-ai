package com.divergent.genesis

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                            val radius = BrainGraphManager.getRadius(it.type) * scale * 2.5f
                            (dx * dx + dy * dy) < (radius * radius)
                        }
                        selectedNodeId = hit?.id
                    }
                }
        ) {
            val cx = size.width / 2f + offset.x
            val cy = size.height / 2f + offset.y

            nodes.forEach { node ->
                val parent = nodes.find { it.id == node.parentId }
                val endX = cx + node.x * scale
                val endY = cy + node.y * scale
                if (parent != null) {
                    drawLine(
                        color = BrainGraphManager.getColor(node.type).copy(alpha = 0.35f),
                        start = Offset(cx + parent.x * scale, cy + parent.y * scale),
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

            nodes.forEach { node ->
                val sx = cx + node.x * scale
                val sy = cy + node.y * scale
                val nodeColor = BrainGraphManager.getColor(node.type)
                val r = BrainGraphManager.getRadius(node.type) * scale

                drawCircle(color = nodeColor.copy(alpha = 0.18f), radius = r * 2f * pulse, center = Offset(sx, sy))
                drawCircle(color = nodeColor, radius = r, center = Offset(sx, sy))
                drawCircle(color = Color.White.copy(alpha = 0.55f), radius = r * 0.3f, center = Offset(sx, sy))

                if (scale > 1.4f || node.id == selectedNodeId) {
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

            drawCircle(color = Color(0xFF8B0000).copy(alpha = 0.15f), radius = 90f * pulse * scale, center = Offset(cx, cy))
            drawCircle(color = Color(0xFF8B0000), radius = 45f * scale, center = Offset(cx, cy))
            drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 14f * scale, center = Offset(cx, cy))
        }

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
            ) { Icon(Icons.Default.Add, contentDescription = "Add", tint = DeepBlack) }

            FloatingActionButton(
                onClick = {
                    scale = 0.7f
                    offset = Offset.Zero
                },
                containerColor = DarkGrey
            ) { Icon(Icons.Default.CenterFocusStrong, contentDescription = "Recenter", tint = NeonCyan) }
        }

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

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = DarkGrey,
                title = { Text(if (dialogParentId == null) "Add Main Neuron" else "Add Child Neuron", color = NeonCyan) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newLabel, onValueChange = { newLabel = it },
                            label = { Text("Name") },
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
