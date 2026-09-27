package com.divergent.genesis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var key by remember { mutableStateOf("") }
                var cmd by remember { mutableStateOf("") }
                var log by remember { mutableStateOf("") }
                val scope = rememberCoroutineScope()

                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(value = key, onValueChange = { key = it },
                        label = { Text("OpenRouter API Key") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = cmd, onValueChange = { cmd = it },
                        label = { Text("Command") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        GenesisBrain.apiKey = key
                        scope.launch {
                            try {
                                val action = GenesisBrain.decide(cmd)
                                val svc = GenesisAccessibilityService.instance
                                when (action.getString("action")) {
                                    "open_app" -> svc?.openApp(action.getString("target"))
                                    "tap_text" -> svc?.findByText(action.getString("target"))?.let { svc.tapNode(it) }
                                    "type" -> svc?.typeText(action.getString("target"))
                                    "swipe" -> svc?.swipe(
                                        action.getDouble("x1").toFloat(), action.getDouble("y1").toFloat(),
                                        action.getDouble("x2").toFloat(), action.getDouble("y2").toFloat())
                                }
                                log = "Done: $action"
                            } catch (e: Exception) { log = "Error: ${e.message}" }
                        }
                    }) { Text("Run") }
                    Spacer(Modifier.height(8.dp))
                    Text(log, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
