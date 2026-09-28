package com.divergent.genesis

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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
