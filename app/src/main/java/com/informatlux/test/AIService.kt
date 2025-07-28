
package com.informatlux.test

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AIService(private val context: Context) {

    companion object {
        private const val TAG = "AIService"
        private const val HUGGING_FACE_API_URL = "https://api-inference.huggingface.co/models/microsoft/DialoGPT-medium"
        // In production, store this securely
        private const val API_KEY = "hf_your_api_key_here" // Replace with actual key
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun classifyWaste(description: String): String = withContext(Dispatchers.IO) {
        try {
            val wasteClassificationPrompt = """
                Classify this waste item and provide recycling instructions: "$description"
                
                Provide classification in this format:
                Category: [Recyclable/Compostable/Hazardous/General Waste]
                Instructions: [Specific disposal instructions]
                Environmental Impact: [Brief impact statement]
                EcoPoints: [Points earned: 10-50]
            """.trimIndent()

            return@withContext queryAI(wasteClassificationPrompt) ?: getOfflineClassification(description)
        } catch (e: Exception) {
            Log.e(TAG, "Error classifying waste", e)
            return@withContext getOfflineClassification(description)
        }
    }

    suspend fun getEcoTip(): String = withContext(Dispatchers.IO) {
        try {
            val tipPrompt = """
                Generate a daily environmental tip about waste reduction, recycling, or sustainability.
                Include actionable steps and environmental impact.
                Format as: 
                Tip: [Main tip]
                Action: [What to do]
                Impact: [Environmental benefit]
            """.trimIndent()

            return@withContext queryAI(tipPrompt) ?: getOfflineEcoTip()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting eco tip", e)
            return@withContext getOfflineEcoTip()
        }
    }

    suspend fun chatWithEcoBot(userMessage: String, conversationHistory: List<String> = emptyList()): String = withContext(Dispatchers.IO) {
        try {
            val contextualPrompt = buildString {
                append("You are EcoBot, an AI assistant specializing in environmental sustainability, waste management, and eco-friendly practices. ")
                append("Provide helpful, accurate advice about recycling, composting, waste reduction, and environmental conservation.\n\n")

                if (conversationHistory.isNotEmpty()) {
                    append("Previous conversation:\n")
                    conversationHistory.takeLast(4).forEach { append("$it\n") }
                    append("\n")
                }

                append("User: $userMessage\nEcoBot:")
            }

            return@withContext queryAI(contextualPrompt) ?: getOfflineEcoBotResponse(userMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error in EcoBot chat", e)
            return@withContext getOfflineEcoBotResponse(userMessage)
        }
    }

    suspend fun analyzeFootprint(wasteData: Map<String, Double>): String = withContext(Dispatchers.IO) {
        try {
            val analysisPrompt = """
                Analyze this weekly waste footprint data and provide insights:
                ${wasteData.entries.joinToString("\n") { "${it.key}: ${it.value} kg" }}
                
                Provide analysis in this format:
                Summary: [Overall assessment]
                Improvements: [Specific recommendations]
                Goals: [Suggested targets for next week]
                Environmental Impact: [CO2 savings potential]
            """.trimIndent()

            return@withContext queryAI(analysisPrompt) ?: getOfflineFootprintAnalysis(wasteData)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing footprint", e)
            return@withContext getOfflineFootprintAnalysis(wasteData)
        }
    }

    private suspend fun queryAI(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(HUGGING_FACE_API_URL)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $API_KEY")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestBody = JSONObject().apply {
                put("inputs", prompt)
                put("parameters", JSONObject().apply {
                    put("max_length", 200)
                    put("temperature", 0.7)
                    put("do_sample", true)
                })
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val jsonResponse = JSONObject(response)

                // Parse response based on API structure
                return@withContext when {
                    jsonResponse.has("generated_text") -> jsonResponse.getString("generated_text")
                    jsonResponse.has("choices") -> jsonResponse.getJSONArray("choices").getJSONObject(0).getString("text")
                    else -> null
                }
            } else {
                Log.e(TAG, "API request failed with code: $responseCode")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying AI API", e)
            return@withContext null
        }
    }

    // Fallback methods for offline/error scenarios
    private fun getOfflineClassification(description: String): String {
        return when {
            description.contains("bottle", ignoreCase = true) -> """
                Category: Recyclable
                Instructions: Clean the bottle, remove cap and label if possible, place in recycling bin
                Environmental Impact: Recycling saves 60% energy compared to producing new plastic
                EcoPoints: 25
            """.trimIndent()

            description.contains("food", ignoreCase = true) || description.contains("organic", ignoreCase = true) -> """
                Category: Compostable
                Instructions: Add to compost bin or organic waste collection
                Environmental Impact: Composting reduces methane emissions from landfills by 50%
                EcoPoints: 20
            """.trimIndent()

            description.contains("battery", ignoreCase = true) || description.contains("electronic", ignoreCase = true) -> """
                Category: Hazardous
                Instructions: Take to designated e-waste collection center or electronics store
                Environmental Impact: Proper disposal prevents toxic materials from contaminating soil
                EcoPoints: 50
            """.trimIndent()

            else -> """
                Category: General Waste
                Instructions: Check local recycling guidelines for specific disposal methods
                Environmental Impact: Proper sorting helps improve recycling efficiency
                EcoPoints: 10
            """.trimIndent()
        }
    }

    private fun getOfflineEcoTip(): String {
        val tips = listOf(
            """
            Tip: Use a reusable water bottle instead of buying plastic bottles
            Action: Invest in a good quality reusable bottle and carry it everywhere
            Impact: Can save up to 1,460 plastic bottles per year
            """.trimIndent(),

            """
            Tip: Start composting your food scraps
            Action: Set up a small compost bin for fruit peels, vegetable scraps, and coffee grounds
            Impact: Reduces household waste by 30% and creates nutrient-rich soil
            """.trimIndent(),

            """
            Tip: Bring reusable bags when shopping
            Action: Keep foldable bags in your car, purse, or by your front door
            Impact: Prevents hundreds of plastic bags from entering the waste stream annually
            """.trimIndent()
        )
        return tips.random()
    }

    private fun getOfflineEcoBotResponse(userMessage: String): String {
        return when {
            userMessage.contains("plastic", ignoreCase = true) ->
                "Great question about plastic! Here are key tips: 1) Reduce single-use plastics, 2) Choose reusable alternatives, 3) Recycle clean containers. Look for recycling numbers 1, 2, and 5 - these are most commonly accepted."

            userMessage.contains("compost", ignoreCase = true) ->
                "Composting is fantastic for the environment! Start with fruit/veggie scraps, coffee grounds, and eggshells. Avoid meat, dairy, and oily foods. Turn your pile weekly and keep it moist but not soggy."

            userMessage.contains("recycle", ignoreCase = true) ->
                "Recycling tips: 1) Clean containers first, 2) Check local guidelines, 3) When in doubt, throw it out (contamination hurts recycling). Paper, cardboard, glass, and metals are great recycling candidates!"

            else ->
                "I'm here to help with all your environmental questions! I can assist with waste sorting, recycling guidelines, composting advice, sustainable living tips, and more. What specific topic interests you?"
        }
    }

    private fun getOfflineFootprintAnalysis(wasteData: Map<String, Double>): String {
        val totalWaste = wasteData.values.sum()
        val recyclableWaste = wasteData.filterKeys { it.contains("plastic", true) || it.contains("paper", true) || it.contains("glass", true) }.values.sum()
        val recyclingRate = if (totalWaste > 0) (recyclableWaste / totalWaste * 100).toInt() else 0

        return """
            Summary: Generated ${totalWaste.toInt()}kg of waste this week with ${recyclingRate}% recycling rate
            Improvements: Focus on reducing single-use items and increasing compost separation
            Goals: Target 20% waste reduction and 80% recycling rate next week
            Environmental Impact: Potential to save 15kg CO2 equivalent through better sorting
        """.trimIndent()
    }

    fun cleanup() {
        serviceScope.cancel()
    }
}
