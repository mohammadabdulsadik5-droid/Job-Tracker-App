package com.example.data

import androidx.annotation.Keep
import com.example.domain.ApplicationStatus
import com.example.domain.CvSet
import com.example.domain.JobApplication
import com.example.domain.JobTrackerRules
import com.example.domain.Region
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FirestoreApplicationDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val company: String = "",
    val role: String = "",
    val region: String = Region.ASIA.key,
    val country: String = "",
    val city: String = "",
    val date: String = "",
    val followDate: String = "",
    val cvSet: String = CvSet.DUBAI.key,
    val cvFile: String = "",
    val website: String = "",
    val email: String = "",
    val confirmCode: String = "",
    val status: String = ApplicationStatus.APPLIED.key,
    val notes: String = "",
    val demoEntry: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toDomain(): JobApplication {
        return JobApplication(
            id = id,
            ownerUid = userId,
            company = company,
            role = role,
            region = Region.fromKey(region),
            country = country,
            city = city,
            date = date,
            followDate = followDate,
            cvSet = CvSet.fromKey(cvSet),
            cvFile = cvFile,
            website = website,
            email = email,
            confirmCode = confirmCode,
            status = ApplicationStatus.fromKey(status),
            notes = notes,
            isDemo = demoEntry,
            createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
        )
    }

    companion object {
        private fun normalizeIsoDate(raw: String): String {
            val trimmed = raw.trim()
            return if (trimmed.length == 10 && Regex("\\d{4}-\\d{2}-\\d{2}").matches(trimmed)) {
                trimmed
            } else {
                JobTrackerRules.todayIso()
            }
        }

        private fun normalizeFollowDate(raw: String, fallbackDate: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return JobTrackerRules.addDaysIso(fallbackDate, 15)
            return trimmed.take(10)
        }

        private fun safeRegionKey(region: Region): String {
            return if (region == Region.ALL) Region.ASIA.key else region.key
        }

        fun toCreatePayload(app: JobApplication, uid: String): Map<String, Any> {
            val cleanDate = normalizeIsoDate(app.date)
            val cleanFollow = normalizeFollowDate(app.followDate, cleanDate)
            return mapOf(
                "userId" to uid,
                "company" to app.company.trim().take(150),
                "role" to app.role.trim().take(150),
                "region" to safeRegionKey(app.region),
                "country" to app.country.trim().take(100),
                "city" to app.city.trim().take(100),
                "date" to cleanDate,
                "followDate" to cleanFollow,
                "cvSet" to app.cvSet.key,
                "cvFile" to app.cvFile.trim().take(150),
                "website" to app.website.trim().take(300),
                "email" to app.email.trim().take(300),
                "confirmCode" to app.confirmCode.trim().take(100),
                "status" to app.status.key,
                "notes" to app.notes.trim().take(2000),
                "demoEntry" to app.isDemo,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        }

        fun toUpdatePayload(app: JobApplication): Map<String, Any> {
            val cleanDate = normalizeIsoDate(app.date)
            val cleanFollow = normalizeFollowDate(app.followDate, cleanDate)
            return mapOf(
                "company" to app.company.trim().take(150),
                "role" to app.role.trim().take(150),
                "region" to safeRegionKey(app.region),
                "country" to app.country.trim().take(100),
                "city" to app.city.trim().take(100),
                "date" to cleanDate,
                "followDate" to cleanFollow,
                "cvSet" to app.cvSet.key,
                "cvFile" to app.cvFile.trim().take(150),
                "website" to app.website.trim().take(300),
                "email" to app.email.trim().take(300),
                "confirmCode" to app.confirmCode.trim().take(100),
                "status" to app.status.key,
                "notes" to app.notes.trim().take(2000),
                "demoEntry" to app.isDemo,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        }
    }
}
