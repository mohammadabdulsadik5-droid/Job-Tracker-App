package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.domain.ApplicationStatus
import com.example.domain.CvSet
import com.example.domain.JobApplication
import com.example.domain.JobTrackerRules
import com.example.domain.Region
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "FirebaseAiExtractor"

data class AiExtractionResult(
    val extractedApp: JobApplication,
    val noticeMessage: String,
    val duplicateMatch: JobApplication? = null,
    val suggestedStatusUpdateTarget: JobApplication? = null,
    val isStatusUpdateReply: Boolean = false,
)

interface AiApplicationExtractor {
    suspend fun extractFromEmailText(
        ownerUid: String,
        emailText: String,
        existingApps: List<JobApplication>,
    ): Result<AiExtractionResult>

    suspend fun extractFromScreenshot(
        ownerUid: String,
        bitmap: Bitmap,
        existingApps: List<JobApplication>,
    ): Result<AiExtractionResult>
}

class FirebaseAiApplicationExtractor : AiApplicationExtractor {

    private val extractionSchema = Schema.obj(
        properties = mapOf(
            "company" to Schema.string(description = "Hiring company or hotel or factory name"),
            "role" to Schema.string(description = "Job position or role title"),
            "region" to Schema.enumeration(
                values = listOf("asia", "europe", "africa", "namerica", "samerica", "oceania", "russia", "turkey"),
                description = "Target region key",
            ),
            "country" to Schema.string(description = "Country name in English, e.g., UAE, Italy, Portugal, Germany"),
            "city" to Schema.string(description = "City name in English, e.g., Dubai, Abu Dhabi, Milan, Porto"),
            "date" to Schema.string(description = "Application date in yyyy-MM-dd format if present, otherwise empty string"),
            "cvSet" to Schema.enumeration(
                values = listOf("Dubai", "Europe", "Other"),
                description = "Recommended CV set based on region/country",
            ),
            "website" to Schema.string(description = "Company website or job link URL if mentioned, otherwise empty string"),
            "email" to Schema.string(description = "HR or company contact email address if present, otherwise empty string"),
            "confirmCode" to Schema.string(description = "Application reference or confirmation code if present, otherwise empty string"),
            "status" to Schema.enumeration(
                values = listOf("applied", "followup", "interview", "rejected", "offer"),
                description = "Application status inferred from message content",
            ),
            "notes" to Schema.string(description = "Brief 1-sentence summary in Bengali explaining key details (interview time, follow-up, etc.)"),
            "isStatusUpdate" to Schema.boolean(description = "True if this email or screenshot is a reply about an existing application (interview invitation, rejection, or offer)"),
        ),
        optionalProperties = listOf(
            "country",
            "city",
            "date",
            "website",
            "email",
            "confirmCode",
            "notes",
        ),
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = extractionSchema
            },
        )
    }

    override suspend fun extractFromEmailText(
        ownerUid: String,
        emailText: String,
        existingApps: List<JobApplication>,
    ): Result<AiExtractionResult> {
        return try {
            val prompt = buildExtractionPrompt() + "\n\nEmail / Confirmation Text:\n" + emailText.take(8000)
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonText = response.text
            if (jsonText.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Gemini থেকে কোনো উত্তর পাওয়া যায়নি।"))
            }
            Result.success(parseStructuredResult(ownerUid, jsonText, existingApps))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini text extraction timed out", e)
            Result.failure(IllegalStateException("AI অ্যানালাইসিস ৬০ সেকেন্ডে সম্পন্ন হয়নি। অনুগ্রহ করে আবার চেষ্টা করুন।"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini text extraction failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "AI অ্যানালাইসিস ব্যর্থ হয়েছে।"))
        }
    }

    override suspend fun extractFromScreenshot(
        ownerUid: String,
        bitmap: Bitmap,
        existingApps: List<JobApplication>,
    ): Result<AiExtractionResult> {
        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(bitmap)
                        text(buildExtractionPrompt() + "\n\nExtract the job application details or reply status shown in this screenshot.")
                    },
                )
            }
            val jsonText = response.text
            if (jsonText.isNullOrBlank()) {
                return Result.failure(IllegalStateException("স্ক্রিনশট থেকে AI কোনো তথ্য পায়নি।"))
            }
            Result.success(parseStructuredResult(ownerUid, jsonText, existingApps))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini screenshot extraction timed out", e)
            Result.failure(IllegalStateException("স্ক্রিনশট অ্যানালাইসিস ৬০ সেকেন্ডে সম্পন্ন হয়নি।"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini screenshot extraction failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "স্ক্রিনশট অ্যানালাইসিস ব্যর্থ হয়েছে।"))
        }
    }

    private fun buildExtractionPrompt(): String = """
        You are an assistant for a Job Application Tracker app focused on Dubai (UAE) and Europe jobs (hospitality/hotel, textile, warehouse, production).
        Extract accurate structured details from the user's job email or screenshot.
        Rules:
        - If the text/screenshot mentions Dubai, Abu Dhabi, UAE -> region="asia", country="UAE", cvSet="Dubai".
        - If European country -> region="europe", cvSet="Europe".
        - If interview invitation or shortlisted -> status="interview", isStatusUpdate=true.
        - If rejection ("unfortunately", "not selected") -> status="rejected", isStatusUpdate=true.
        - If job offer -> status="offer", isStatusUpdate=true.
        - Otherwise status="applied", isStatusUpdate=false.
        - Do not invent fake emails or confirmation codes; return empty string if absent.
    """.trimIndent()

    private fun parseStructuredResult(
        ownerUid: String,
        rawJson: String,
        existingApps: List<JobApplication>,
    ): AiExtractionResult {
        val json = JSONObject(rawJson)
        val today = JobTrackerRules.todayIso()
        val company = json.optString("company").trim().ifBlank { "Unknown Company" }
        val role = json.optString("role").trim().ifBlank { "Job Applicant" }
        val region = Region.fromKey(json.optString("region"))
        val country = json.optString("country").trim()
        val city = json.optString("city").trim()
        val rawDate = json.optString("date").trim()
        val appDate = if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(rawDate)) rawDate else today
        val followDate = JobTrackerRules.addDaysIso(appDate, 15)
        val cvSet = CvSet.fromKey(json.optString("cvSet"))
        val cvFile = JobTrackerRules.suggestCvFile(role, company).ifBlank {
            when (cvSet) {
                CvSet.DUBAI -> "Hotel CV"
                CvSet.EUROPE -> "Europe CV"
                CvSet.OTHER -> "General CV"
            }
        }
        val website = JobTrackerRules.normalizeWebsite(json.optString("website").trim()).getOrDefault("")
        val email = json.optString("email").trim()
        val confirmCode = json.optString("confirmCode").trim()
        val status = ApplicationStatus.fromKey(json.optString("status"))
        val notes = json.optString("notes").trim()
        val isStatusUpdate = json.optBoolean("isStatusUpdate", false)

        val extracted = JobApplication(
            ownerUid = ownerUid,
            company = company,
            role = role,
            region = if (region == Region.ALL) Region.ASIA else region,
            country = country,
            city = city,
            date = appDate,
            followDate = followDate,
            cvSet = cvSet,
            cvFile = cvFile,
            website = website,
            email = email,
            confirmCode = confirmCode,
            status = status,
            notes = notes,
        )

        val duplicate = JobTrackerRules.findDuplicate(existingApps, extracted.company, extracted.email)
        val statusTarget = if (
            isStatusUpdate ||
            status == ApplicationStatus.INTERVIEW ||
            status == ApplicationStatus.REJECTED ||
            status == ApplicationStatus.OFFER
        ) {
            duplicate ?: existingApps.firstOrNull { existing ->
                val companyMatch = extracted.company.isNotBlank() &&
                    existing.company.contains(extracted.company, ignoreCase = true)
                val emailList = JobTrackerRules.extractEmails(extracted.email)
                val emailMatch = emailList.any { em -> existing.email.contains(em, ignoreCase = true) }
                companyMatch || emailMatch
            }
        } else {
            null
        }

        val notice = if (statusTarget != null && statusTarget.status != status) {
            "✨ Gemini AI: \"${statusTarget.company}\"-এর জন্য স্ট্যাটাস আপডেট (${status.bnLabel}) পাওয়া গেছে!"
        } else {
            "✨ Gemini AI তথ্য সফলভাবে সংগ্রহ করেছে — নিচে দেখে ফর্মে বসান।"
        }

        return AiExtractionResult(
            extractedApp = extracted,
            noticeMessage = notice,
            duplicateMatch = duplicate,
            suggestedStatusUpdateTarget = statusTarget,
            isStatusUpdateReply = isStatusUpdate || statusTarget != null,
        )
    }
}
