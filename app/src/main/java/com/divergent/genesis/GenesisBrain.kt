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
    private const val MODEL = "nvidia/nemotron-3-ultra-550b-a55b:free"
    
    var apiKey: String = ""

    private const val SYSTEM_PROMPT = """
You are Divergent Genesis, an Android automation agent. Output ONLY a valid JSON action.
Known brain shortcuts:
- To open YouTube: {"action":"open_app","target":"com.google.android.youtube"}
- To open Instagram: {"action":"open_app","target":"com.instagram.android"}
- To open Google: {"action":"open_url","target":"https://google.com"}
- To post a Reel on Instagram: {"action":"run_workflow","target":"instagram_reel"}
- To upload a YouTube Short: {"action":"run_workflow","target":"youtube_short"}
- To search YouTube for a channel and subscribe: {"action":"run_workflow","target":"youtube_search_subscribe","param":"CHANNEL_NAME"}
- To tap any on-screen text: {"action":"tap_text","target":"TEXT"}
- To type into the focused field: {"action":"type","target":"TEXT"}
- If you don't know what to do: {"action":"done"}
If the user pastes a URL and asks to open it, reply with: {"action":"open_url","target":"THE_URL"}
Reply with JSON only, no markdown, no explanation.
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
            if (!resp.isSuccessful) throw Exception("API Error: $body")
            val jsonBody = JSONObject(body)
            if (!jsonBody.has("choices")) throw Exception("Invalid Response: $body")
            val content = jsonBody.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            val cleaned = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            JSONObject(cleaned)
        }
    }
}
