package com.divergent.genesis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object GenesisBrain {
    private const val URL = "https://openrouter.ai/api/v1/chat/completions"
    
    // UPDATED: Changed to a currently active free model
    private const val MODEL = "nvidia/nemotron-3-ultra-550b-a55b:free"
    
    var apiKey: String = ""

    private const val SYSTEM_PROMPT = """
You are Divergent Genesis, an Android automation agent. Output ONLY a valid JSON action.
Actions available:
{"action":"open_app","target":"com.google.android.youtube"}
{"action":"tap_text","target":"Subscribe"}
{"action":"type","target":"hello world"}
{"action":"swipe","x1":500,"y1":1500,"x2":500,"y2":500}
{"action":"run_workflow","target":"instagram_reel"}
{"action":"run_workflow","target":"youtube_short"}
{"action":"done"}
If the user asks to post a Reel or video to Instagram, reply with: {"action":"run_workflow","target":"instagram_reel"}
If the user asks to upload a Short to YouTube, reply with: {"action":"run_workflow","target":"youtube_short"}
If the user asks to open YouTube, reply with: {"action":"open_app","target":"com.google.android.youtube"}
Reply with JSON only. No markdown.
"""

    suspend fun decide(command: String): JSONObject = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val payload = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role","system").put("content", SYSTEM_PROMPT))
                put(JSONObject().put("role","user").put("content", command))
            })
        }
        val req = Request.Builder()
            .url(URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: "{}"
            
            // Show the actual API error in the chat if the request fails
            if (!resp.isSuccessful) {
                throw Exception("API Error: $body")
            }
            
            val jsonBody = JSONObject(body)
            if (!jsonBody.has("choices")) {
                throw Exception("Invalid Response: $body")
            }
            
            val content = jsonBody.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
            val cleaned = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            JSONObject(cleaned)
        }
    }
}
