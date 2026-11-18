package com.example.data.ai

import android.util.Log
import com.example.data.firebase.PriorityLevel
import com.example.data.firebase.ProblemDepartment
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiAiRepo"

enum class AiModelTier(val modelId: String, val label: String, val subtitle: String) {
  STANDARD("gemini-3.8-flash", "Standard (3.8 Flash)", "Balanced speed & civic reasoning"),
  FAST("gemini-3.5-flash-lite", "Fast (3.5 Lite)", "Ultra-fast responses & triage"),
  DEEP("gemini-4-argon", "Deep Policy (4 Argon)", "Complex fund & escalation analysis"),
}

data class VoiceComplaintExtraction(
  val transcript: String,
  val title: String,
  val department: ProblemDepartment,
  val priority: PriorityLevel,
  val description: String,
)

data class SmartTriageSuggestion(
  val department: ProblemDepartment,
  val subCategory: String,
  val priority: PriorityLevel,
  val summary: String,
)

class GeminiAiRepository {

  private val systemPrompt =
    content {
      text(
        """
        You are GramSahayak, the civic governance assistant for GramConnect in Telangana, India.
        Help citizens, Ward Representatives, Sarpanches, Mandal/District Officers, and Field Workers with:
        1. Understanding Telangana & Central Government welfare schemes (Rythu Bharosa, Gruha Jyothi, Mahalakshmi, Cheyutha, PM Awas, PM-KISAN) without inventing non-official benefit rules.
        2. Explaining how complaints escalate across the GramConnect hierarchy: Citizen -> Ward -> Sarpanch/Village -> Mandal Smart Module -> District Board -> State Admin.
        3. Assisting with PDS Ration Card & monthly entitlement queries.
        4. Supporting both English and Telugu (తెలుగు) clearly and respectfully.
        Keep answers concise, factual, and grounded in civic governance workflows.
        """.trimIndent()
      )
    }

  private val chatModels by lazy {
    AiModelTier.entries.associateWith { tier ->
      Firebase.ai.generativeModel(
        modelName = tier.modelId,
        systemInstruction = systemPrompt,
      )
    }
  }

  private val voiceTranscriptionModel by lazy {
    val schema =
      Schema.obj(
        properties =
          mapOf(
            "transcript" to Schema.string(description = "Verbatim transcription in Telugu or English"),
            "title" to Schema.string(description = "Short 5-10 word complaint title in English"),
            "department" to
              Schema.enumeration(
                values = ProblemDepartment.entries.map { it.name },
                description = "Best matching GramConnect department",
              ),
            "priority" to
              Schema.enumeration(
                values = PriorityLevel.entries.map { it.name },
                description = "Urgency priority",
              ),
            "description" to Schema.string(description = "Clear summary of the grievance"),
          )
      )
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = schema
        },
    )
  }

  private val fastTriageModel by lazy {
    val schema =
      Schema.obj(
        properties =
          mapOf(
            "department" to
              Schema.enumeration(
                values = ProblemDepartment.entries.map { it.name },
                description = "Best matching department",
              ),
            "subCategory" to Schema.string(description = "Specific issue subcategory"),
            "priority" to
              Schema.enumeration(
                values = PriorityLevel.entries.map { it.name },
                description = "Recommended urgency level",
              ),
            "summary" to Schema.string(description = "One-sentence administrative triage summary"),
          )
      )
    Firebase.ai.generativeModel(
      modelName = "gemini-3.5-flash-lite",
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = schema
        },
    )
  }

  fun streamChatReply(
    tier: AiModelTier,
    history: List<Content>,
    userPrompt: String,
  ): Flow<String> = flow {
    val model = chatModels.getValue(tier)
    val chat = model.startChat(history = history)
    val accumulated = StringBuilder()
    withTimeout(60_000L) {
      chat.sendMessageStream(userPrompt).collect { chunk ->
        val piece = chunk.text.orEmpty()
        if (piece.isNotEmpty()) {
          accumulated.append(piece)
          emit(accumulated.toString())
        }
      }
    }
    if (accumulated.isBlank()) {
      throw IllegalStateException("Gemini returned an empty response.")
    }
  }.catch { error ->
    if (error is CancellationException && error !is TimeoutCancellationException) throw error
    Log.e(TAG, "Chat stream failed", error)
    throw error
  }

  suspend fun transcribeVoiceComplaint(wavBytes: ByteArray): Result<VoiceComplaintExtraction> {
    return try {
      require(wavBytes.size > 44) { "Recorded audio was empty. Please speak and try again." }
      require(wavBytes.size <= 18 * 1024 * 1024) { "Audio exceeds 18MB limit." }
      val response =
        withTimeout(60_000L) {
          voiceTranscriptionModel.generateContent(
            content {
              inlineData(wavBytes, "audio/wav")
              text(
                "Transcribe this Telangana citizen voice recording (Telugu or English) and extract a structured GramConnect grievance."
              )
            }
          )
        }
      val jsonText = response.text ?: throw IllegalStateException("Empty transcription response.")
      val json = JSONObject(jsonText)
      Result.success(
        VoiceComplaintExtraction(
          transcript = json.optString("transcript", ""),
          title = json.optString("title", "Voice Reported Grievance").take(140),
          department = ProblemDepartment.safeValueOf(json.optString("department")),
          priority = PriorityLevel.safeValueOf(json.optString("priority")),
          description = json.optString("description", json.optString("transcript", "")).take(2000),
        )
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Audio transcription timed out", e)
      Result.failure(Exception("Audio transcription timed out after 60 seconds."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Audio transcription failed", e)
      Result.failure(e)
    }
  }

  suspend fun triageComplaintText(title: String, description: String): Result<SmartTriageSuggestion> {
    return try {
      val response =
        withTimeout(45_000L) {
          fastTriageModel.generateContent(
            "Classify this Telangana village complaint.\nTitle: $title\nDescription: $description"
          )
        }
      val jsonText = response.text ?: throw IllegalStateException("Empty triage response.")
      val json = JSONObject(jsonText)
      val dept = ProblemDepartment.safeValueOf(json.optString("department"))
      Result.success(
        SmartTriageSuggestion(
          department = dept,
          subCategory = json.optString("subCategory", dept.subCategories.first()),
          priority = PriorityLevel.safeValueOf(json.optString("priority")),
          summary = json.optString("summary", "Routed to ${dept.title}."),
        )
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Triage timed out", e)
      Result.failure(Exception("AI triage timed out."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Triage failed", e)
      Result.failure(e)
    }
  }
}
