package com.example.pinkmoney.utils

import android.util.Log
import com.example.pinkmoney.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object GeminiParser {

    private const val API_KEY = BuildConfig.GEMINI_API_KEY
    private val client = OkHttpClient()

    fun parseTransaction(
        text: String,
        callback: (category: String?, merchant: String?, type: String?) -> Unit
    ) {

        val prompt = """
You are an advanced financial SMS classifier.

STEP 1 → Classify message:
- REAL_TRANSACTION
- SPAM
- PROMOTIONAL
- INFO

STEP 2 → ONLY if REAL_TRANSACTION:
Extract:
- merchant
- type: DEBIT or CREDIT

Rules:
- debited → DEBIT
- credited → CREDIT
- ignore bank names
- prefer UPI receiver
- no money movement → NOT REAL_TRANSACTION

Return STRICT JSON:

{
 "category": "REAL_TRANSACTION | SPAM | PROMOTIONAL | INFO",
 "merchant": "name or null",
 "type": "DEBIT | CREDIT | null"
}

SMS:
$text
        """.trimIndent()

        Log.d("GeminiPrompt", prompt)

        val bodyJson = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts",
                    JSONArray().put(JSONObject().put("text", prompt))
                )
            ))

            put("generationConfig", JSONObject().put(
                "response_mime_type", "application/json"
            ))
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$API_KEY")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), bodyJson.toString()))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.e("GeminiAPI", "Failed: ${e.message}")
                callback(null, null, null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return

                Log.d("GeminiFullResponse", body)

                try {
                    val aiText = JSONObject(body)
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    Log.d("GeminiAIText", aiText)

                    val result = JSONObject(aiText)

                    val category = result.optString("category", "UNKNOWN")
                    val merchant = result.optString("merchant", "Unknown")
                    val type = result.optString("type", "UNKNOWN")

                    Log.d("GeminiParsed",
                        "Category=$category Merchant=$merchant Type=$type")

                    callback(category, merchant, type)

                } catch (e: Exception) {

                    Log.e("GeminiParseError", e.toString())

                    // fallback heuristic
                    val lower = text.lowercase()

                    val type = when {
                        "debited" in lower -> "DEBIT"
                        "credited" in lower -> "CREDIT"
                        else -> "UNKNOWN"
                    }

                    callback("UNKNOWN", "Unknown", type)
                }
            }
        })
    }
}