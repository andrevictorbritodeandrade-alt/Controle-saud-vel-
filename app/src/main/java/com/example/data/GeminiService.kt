package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val fallbackTips = listOf(
        "💡 **Luz Matinal**: Exponha-se à luz solar natural nos primeiros 30 minutos ao acordar para sinalizar o pico saudável de cortisol e sincronizar seu ritmo circadiano.",
        "💡 **Pausa Consciente**: Faça a pausa sem celular no meio do dia. 5 minutos de respiração profunda (4-7-8) reduzem instantaneamente a ativação do sistema simpático.",
        "💡 **Higiene do Sono**: Desligue telas 1 hora antes de dormir e tome o magnésio inositol. Isso bloqueia a estimulação de cortisol noturno e facilita o sono profundo.",
        "💡 **Hidratação e Aterramento**: Comece o dia bebendo 500ml de água com uma pitada de sal marinho. Se possível, caminhe descalço na grama por 5 minutos.",
        "💡 **Alimentação Anti-Cortisol**: Priorize refeições com gorduras boas e proteínas limpas no café da manhã para evitar picos de glicemia que disparam o estresse."
    )

    suspend fun fetchDailyCortisolTip(
        dayOfWeek: String,
        completedCount: Int,
        totalCount: Int
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // If no valid key or placeholder, return a context-aware fallback tip
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "DEFAULT_API_KEY") {
            return@withContext getSmartFallbackTip(dayOfWeek, completedCount, totalCount)
        }

        val promptText = """
            Você é um especialista em ritmo circadiano, neurobiologia do estresse e otimização do cortisol.
            O usuário está seguindo um protocolo de saúde de 30 dias focado na REDUÇÃO DE CORTISOL.
            
            Contexto do usuário hoje ($dayOfWeek):
            - Tarefas do protocolo concluídas hoje: $completedCount de $totalCount
            
            Gere uma DICA DIÁRIA personalizada, acionável e cientificamente embasada (máximo 2 a 3 frases) em português para otimizar a redução de cortisol e o ritmo circadiano do usuário hoje.
            Use um tom encorajador, prático e direto com emojis.
        """.trimIndent()

        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 250)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        if (text.isNotBlank()) {
                            return@withContext text.trim()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext getSmartFallbackTip(dayOfWeek, completedCount, totalCount)
    }

    private fun getSmartFallbackTip(dayOfWeek: String, completedCount: Int, totalCount: Int): String {
        val index = (dayOfWeek.hashCode() + completedCount) % fallbackTips.size
        return fallbackTips[Math.abs(index)]
    }
}
