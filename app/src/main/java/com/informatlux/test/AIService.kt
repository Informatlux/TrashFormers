package com.informatlux.test

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.random.Random

class AIService {

    companion object {
        private const val TAG = "AIService"
        private const val API_KEY = "Bearer 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"

        // Use local mode by default until API is fixed or implemented
        private var useLocalMode = true

        private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        private val gson = Gson()

        // Predefined conversation responses mapped by keyword lists
        private val conversationResponses = mapOf(
            listOf("hello", "hi", "hey", "good morning", "good evening") to listOf(
                "Hello! I'm your AI assistant for waste management and environmental questions. How can I help you today?",
                "Hi there! I can help you with recycling, waste classification, and environmental tips. What would you like to know?",
                "Hey! I'm here to assist with all your waste management questions. Fire away!"
            ),
            listOf("waste", "trash", "garbage", "rubbish") to listOf(
                "There are several types of waste: recyclable (plastic, paper, glass, metal), organic (food scraps), hazardous (batteries, chemicals), and general waste. Which type are you asking about?",
                "Proper waste management involves sorting into categories: recyclables, compostables, hazardous materials, and landfill waste. What specific waste do you need help with?",
                "Waste can be managed better by following the 3 R's: Reduce, Reuse, Recycle. What aspect would you like to explore?"
            ),
            listOf("recycle", "recycling", "recyclable") to listOf(
                "Great question about recycling! Most plastic bottles, aluminum cans, paper, and cardboard can be recycled. Check the recycling symbols and local guidelines.",
                "Recycling helps reduce environmental impact. Clean containers, separate materials by type, and check your local recycling center's guidelines.",
                "The key to effective recycling is proper sorting: plastics (#1-7), paper products, metals, and glass. Always clean containers first!"
            ),
            listOf("plastic", "bottle", "container") to listOf(
                "Plastic recycling depends on the type! Look for numbers 1-7 in the recycling symbol. PET (#1) and HDPE (#2) are most commonly recycled.",
                "Plastic containers should be emptied, rinsed, and sorted by type. Remove caps and labels when possible for better recycling.",
                "Not all plastics are recyclable. Check with your local facility, but generally avoid #3, #6, and #7 plastics in curbside recycling."
            ),
            listOf("paper", "cardboard", "newspaper") to listOf(
                "Paper products are highly recyclable! Remove staples, plastic tape, and flatten cardboard boxes. Avoid wet or greasy paper.",
                "Most paper can be recycled 5-7 times before the fibers become too short. Keep it dry and separate from other materials.",
                "Cardboard recycling tip: Break down boxes and remove any plastic tape or labels for the best recycling results."
            ),
            listOf("metal", "aluminum", "can", "tin") to listOf(
                "Metal is infinitely recyclable! Aluminum cans, steel cans, and foil can all be recycled. Rinse them clean first.",
                "Aluminum recycling saves 95% of the energy needed to make new aluminum. It's one of the most valuable recyclables!",
                "Metal containers should be empty and rinsed. You can leave labels on - they'll be removed during processing."
            ),
            listOf("glass", "jar", "bottle") to listOf(
                "Glass is 100% recyclable and can be recycled endlessly! Separate by color if your area requires it.",
                "Remove caps and lids from glass containers. Most recycling programs accept clear, brown, and green glass.",
                "Glass recycling tip: Don't include broken glass, mirrors, or window glass in regular recycling - they have different melting points."
            ),
            listOf("electronic", "e-waste", "battery", "phone", "computer") to listOf(
                "E-waste needs special handling! Take electronics to certified e-waste recycling centers, not regular trash.",
                "Electronics contain valuable materials like gold, silver, and rare earth elements that can be recovered through proper recycling.",
                "Many retailers offer e-waste takeback programs. Check with manufacturers or local electronics stores for options."
            ),
            listOf("food", "organic", "compost", "kitchen") to listOf(
                "Food waste can be composted! Fruit peels, vegetable scraps, coffee grounds, and eggshells make great compost.",
                "Composting reduces methane emissions from landfills and creates nutrient-rich soil. Avoid meat, dairy, and oils in home compost.",
                "Organic waste comprises about 30% of household waste. Composting is a great way to reduce your environmental impact!"
            ),
            listOf("help", "what can you do", "assist") to listOf(
                "I can help you with:\n• Waste identification and classification\n• Recycling guidelines\n• Environmental tips\n• Composting advice\n• E-waste disposal\n\nWhat specific topic interests you?",
                "I'm here to assist with waste management questions! I can identify materials, provide recycling tips, and suggest eco-friendly practices.",
                "My expertise covers waste sorting, recycling processes, environmental impact, and sustainable practices. How can I help you today?"
            ),
            listOf("environment", "climate", "sustainability", "green") to listOf(
                "Great to hear you're thinking about the environment! Proper waste management is crucial for reducing pollution and conserving resources.",
                "Sustainable practices include reducing consumption, reusing items, recycling properly, and composting organic waste.",
                "Every small action counts! Proper waste sorting, using reusable items, and supporting circular economy practices make a big difference."
            )
        )

        private val wasteClassifications = listOf(
            "plastic bottle", "aluminum can", "cardboard box", "glass jar", "food waste",
            "paper document", "electronic device", "metal container", "organic matter",
            "recyclable plastic", "compostable material", "hazardous waste"
        )

        private val commonObjects = listOf(
            "plastic bottle", "aluminum can", "paper", "cardboard", "glass container",
            "food packaging", "metal can", "plastic bag", "newspaper", "magazine",
            "electronics", "battery", "organic waste", "fabric", "wood"
        )

        private val imageCaptions = listOf(
            "I can see various items that appear to be waste or recyclable materials",
            "This image contains objects that could be sorted for recycling",
            "I notice several items that might need proper waste classification",
            "The image shows materials that could benefit from proper sorting",
            "I can identify objects that may be recyclable or compostable"
        )
    }

    /**
     * Coroutine suspend function to generate text response.
     * Currently always uses local mode.
     */
    suspend fun generateText(prompt: String): String {
        Log.d(TAG, "Using local text generation for prompt: $prompt")
        return generateLocalResponse(prompt)
    }

    /**
     * Local simple rule-based response generator
     */
    private fun generateLocalResponse(prompt: String): String {
        val lowerPrompt = prompt.lowercase()

        // Search for matching keyword categories and pick a random response
        for ((keywords, responses) in conversationResponses) {
            if (keywords.any { lowerPrompt.contains(it) }) {
                return responses.random()
            }
        }

        // Handle special context-aware cases
        return when {
            lowerPrompt.contains("how") && lowerPrompt.contains("recycle") -> {
                "To recycle properly: 1) Clean containers, 2) Sort by material type, 3) Check local guidelines, 4) Use proper bins. What specific item do you want to recycle?"
            }
            lowerPrompt.contains("what") && lowerPrompt.contains("type") -> {
                "I can help identify waste types! Common categories include: recyclables (plastic, paper, metal, glass), organics (food scraps), hazardous (batteries, chemicals), and general waste."
            }
            lowerPrompt.contains("where") -> {
                "For disposal locations, check with your local waste management authority. Most areas have recycling centers, hazardous waste facilities, and compost programs."
            }
            lowerPrompt.contains("can i") || lowerPrompt.contains("should i") -> {
                "That depends on the specific material and your local facilities. Generally, clean recyclables go in recycling bins, organics can be composted, and hazardous items need special handling."
            }
            lowerPrompt.length > 50 -> {
                "That's a detailed question! For comprehensive waste management, focus on the 3 R's: Reduce consumption, Reuse items when possible, and Recycle properly. What specific aspect would you like me to elaborate on?"
            }
            else -> {
                "I'm here to help with waste management and recycling questions! You can ask me about:\n• How to recycle specific items\n• Waste classification\n• Environmental tips\n• Composting guidance\n\nWhat would you like to know?"
            }
        }
    }

    /**
     * Local image classification simulation (placeholder for future real ML classification).
     */
    suspend fun classifyImage(base64Image: String): String {
        Log.d(TAG, "Using local image classification simulation")

        // Simulate analysis delay
        delay(1000)

        return wasteClassifications.random()
    }

    /**
     * Local object detection simulation (placeholder).
     */
    suspend fun detectObjects(base64Image: String): List<String> {
        Log.d(TAG, "Using local object detection simulation")

        delay(1200)

        val numObjects = Random.nextInt(2, 5)
        return commonObjects.shuffled().take(numObjects)
    }

    /**
     * Local image captioning simulation (placeholder).
     */
    suspend fun captionImage(base64Image: String): String {
        Log.d(TAG, "Using local image captioning simulation")

        delay(800)

        return imageCaptions.random()
    }

    /**
     * Tests connectivity to remote API (can be used later, currently unused)
     */
    suspend fun testAPIConnectivity(): Boolean = suspendCancellableCoroutine { cont ->
        val requestBody = gson.toJson(mapOf("inputs" to "test"))
        val request = Request.Builder()
            .url("https://api-inference.huggingface.co/models/gpt2")
            .addHeader("Authorization", API_KEY)
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBody))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                Log.e(TAG, "API connectivity test failed", e)
                if (cont.isActive) cont.resume(false)
            }

            override fun onResponse(call: Call, response: Response) {
                val success = response.isSuccessful
                Log.d(TAG, "API connectivity test result: $success (HTTP ${response.code})")
                if (cont.isActive) cont.resume(success)
                response.close()
            }
        })
    }

    /**
     * Enables or disables local mode (for switching to API when implemented)
     */
    fun setLocalMode(enabled: Boolean) {
        useLocalMode = enabled
        Log.d(TAG, "Local mode is now ${if (enabled) "enabled" else "disabled"}")
    }
}
