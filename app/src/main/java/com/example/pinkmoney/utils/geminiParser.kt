package com.example.pinkmoney.utils

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object GeminiParser {

    private const val API_KEY = "AIzaSyBgeLE3XFhgH6fQhCXFhIch-NeRxxssw7E"

    private val client = OkHttpClient()

    fun parseTransaction(
        text: String,
        callback: (merchant: String?, type: String?) -> Unit
    ) {

        val prompt = """
You are an expert financial SMS parser.

Extract:
1. Merchant or receiver name
2. Transaction type (DEBIT or CREDIT)

Rules:
- If SMS contains "debited" → DEBIT
- If SMS contains "credited" → CREDIT
- Ignore bank names like ICICI, HDFC, SBI
- Prefer UPI receiver name if present

Return ONLY JSON in this format:

{
 "merchant": "name",
 "type": "DEBIT or CREDIT"
}

SMS:
$text
""".trimIndent()

        Log.d("GeminiPrompt", prompt)

        val part = JSONObject()
        part.put("text", prompt)

        val partsArray = JSONArray()
        partsArray.put(part)

        val content = JSONObject()
        content.put("parts", partsArray)

        val contentsArray = JSONArray()
        contentsArray.put(content)

        val bodyJson = JSONObject()
        bodyJson.put("contents", contentsArray)

        // 🔴 FORCE JSON RESPONSE
        val generationConfig = JSONObject()
        generationConfig.put("response_mime_type", "application/json")

        bodyJson.put("generationConfig", generationConfig)

        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            bodyJson.toString()
        )

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$API_KEY")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.e("GeminiAPI", "Request failed: ${e.message}")
                callback(null, null)
            }

            override fun onResponse(call: Call, response: Response) {

                val body = response.body?.string() ?: return

                Log.d("GeminiFullResponse", body)

                try {

                    val json = JSONObject(body)

                    val aiText = json
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    Log.d("GeminiAIText", aiText)

                    // Gemini now returns clean JSON
                    val result = JSONObject(aiText)

                    val merchant = result.optString("merchant", "Unknown")
                    val type = result.optString("type", "UNKNOWN")

                    Log.d("GeminiParsedResult", "Merchant=$merchant Type=$type")

                    callback(merchant, type)

                } catch (e: Exception) {

                    Log.e("GeminiParseError", e.toString())

                    // fallback heuristic
                    val type = if (text.lowercase().contains("debited")) {
                        "DEBIT"
                    } else if (text.lowercase().contains("credited")) {
                        "CREDIT"
                    } else {
                        "UNKNOWN"
                    }

                    callback("Unknown", type)
                }
            }
        })
    }
}