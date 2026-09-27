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
    private const val MODEL = "meta-llama/llama-3.3-70b-instruct:free"
    var apiKey: String = ""

    private const val SYSTEM_PROMPT = """
You are Divergent Genesis, an Android automation agent. Output ONLY a valid JSON action.
Actions available:
{"action":"open_app","target":"com.google.android.youtube"}
{"action":"tap_text","target":"Subscribe"}
{"action":"type","target":"hello world"}
{"action":"swipe","x1":500,"y1":1500,"x2":500,"y2":500}
{"action":"done"}
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
            val content = JSONObject(body).getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
            val cleaned = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            JSONObject(cleaned)
        }
    }
}
