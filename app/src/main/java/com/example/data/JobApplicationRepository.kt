package com.example.data

import com.example.domain.ApplicationStatus
import com.example.domain.CvSet
import com.example.domain.JobApplication
import com.example.domain.JobTrackerRules
import com.example.domain.Region
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

data class ImportSummary(
    val importedCount: Int,
    val skippedDuplicates: Int,
)

class JobApplicationRepository(
    private val db: FirebaseFirestore,
    private val prefs: UserPreferencesRepository,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    private fun requireSignedInUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("লগইন সেশন পাওয়া যায়নি। অনুগ্রহ করে আবার Google দিয়ে লগইন করুন।")
    }

    private fun applicationsCollection(uid: String) =
        db.collection("users").document(uid).collection("applications")

    fun observeApplications(uid: String): Flow<List<JobApplication>> = flow {
        val signedInUid = requireSignedInUid()
        if (uid.isBlank() || signedInUid != uid) {
            throw IllegalStateException("ব্যবহারকারীর সেশন মিলছে না।")
        }
        val path = "users/$uid/applications"
        emitAll(
            applicationsCollection(uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreApplicationDto::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).map { it.toDomain() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                },
        )
    }

    suspend fun ensureDemoDataForNewUser(uid: String): Result<Unit> {
        val path = "users/$uid/applications"
        return try {
            val signedInUid = requireSignedInUid()
            if (uid.isBlank() || signedInUid != uid) {
                return Result.failure(IllegalStateException("অননুমোদিত অ্যাক্সেস"))
            }
            if (!prefs.isDemoSeeded(uid)) {
                val existingSnapshot = applicationsCollection(uid).limit(1).get().await()
                if (existingSnapshot.isEmpty) {
                    val demoApps = JobTrackerRules.buildDemoApplications(uid)
                    val batch = db.batch()
                    demoApps.forEach { demo ->
                        val docRef = applicationsCollection(uid).document()
                        batch.set(docRef, FirestoreApplicationDto.toCreatePayload(demo, uid))
                    }
                    batch.commit().await()
                }
                prefs.markDemoSeeded(uid)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addApplication(app: JobApplication): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/applications"
        return try {
            val uid = requireSignedInUid()
            val colRef = applicationsCollection(uid)
            docPath = colRef.path

            val company = app.company.trim()
            val role = app.role.trim()
            if (company.isBlank()) {
                return Result.failure(IllegalArgumentException("কোম্পানির নাম অবশ্যই লিখতে হবে"))
            }
            if (role.isBlank()) {
                return Result.failure(IllegalArgumentException("পদের নাম (Role) অবশ্যই লিখতে হবে"))
            }

            val existing = colRef.get().await().toObjects(
                FirestoreApplicationDto::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            ).map { it.toDomain() }

            val duplicate = JobTrackerRules.findDuplicate(existing, company, app.email)
            if (duplicate != null) {
                return Result.failure(
                    IllegalStateException(
                        "ডুপ্লিকেট পাওয়া গেছে: \"${duplicate.company}\" (${duplicate.role})",
                    ),
                )
            }

            val normalizedUrl = JobTrackerRules.normalizeWebsite(app.website).getOrElse {
                return Result.failure(it)
            }
            val resolvedCvFile = app.cvFile.trim().ifBlank {
                JobTrackerRules.suggestCvFile(role, company)
            }
            val cleanDate = app.date.trim().ifBlank { JobTrackerRules.todayIso() }
            val resolvedFollowDate = app.followDate.trim().ifBlank {
                JobTrackerRules.addDaysIso(cleanDate, 15)
            }

            val cleaned = app.copy(
                ownerUid = uid,
                company = company,
                role = role,
                country = app.country.trim(),
                city = app.city.trim(),
                date = cleanDate,
                followDate = resolvedFollowDate,
                cvFile = resolvedCvFile,
                website = normalizedUrl,
                email = app.email.trim(),
                confirmCode = app.confirmCode.trim(),
                notes = app.notes.trim(),
            )

            val docRef = colRef.document()
            docPath = docRef.path
            docRef.set(FirestoreApplicationDto.toCreatePayload(cleaned, uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateApplication(app: JobApplication): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/applications/${app.id}"
        return try {
            val uid = requireSignedInUid()
            require(app.id.isNotBlank()) { "আপডেট করার জন্য আবেদন আইডি পাওয়া যায়নি" }

            val company = app.company.trim()
            val role = app.role.trim()
            if (company.isBlank()) {
                return Result.failure(IllegalArgumentException("কোম্পানির নাম অবশ্যই লিখতে হবে"))
            }
            if (role.isBlank()) {
                return Result.failure(IllegalArgumentException("পদের নাম (Role) অবশ্যই লিখতে হবে"))
            }

            val colRef = applicationsCollection(uid)
            val existing = colRef.get().await().toObjects(
                FirestoreApplicationDto::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            ).map { it.toDomain() }

            val duplicate = JobTrackerRules.findDuplicate(existing, company, app.email, excludeId = app.id)
            if (duplicate != null) {
                return Result.failure(
                    IllegalStateException(
                        "অন্য একটি ডুপ্লিকেট আবেদন পাওয়া গেছে: \"${duplicate.company}\" (${duplicate.role})",
                    ),
                )
            }

            val normalizedUrl = JobTrackerRules.normalizeWebsite(app.website).getOrElse {
                return Result.failure(it)
            }
            val resolvedCvFile = app.cvFile.trim().ifBlank {
                JobTrackerRules.suggestCvFile(role, company)
            }
            val cleanDate = app.date.trim().ifBlank { JobTrackerRules.todayIso() }
            val resolvedFollowDate = app.followDate.trim().ifBlank {
                JobTrackerRules.addDaysIso(cleanDate, 15)
            }

            val cleaned = app.copy(
                ownerUid = uid,
                company = company,
                role = role,
                country = app.country.trim(),
                city = app.city.trim(),
                date = cleanDate,
                followDate = resolvedFollowDate,
                cvFile = resolvedCvFile,
                website = normalizedUrl,
                email = app.email.trim(),
                confirmCode = app.confirmCode.trim(),
                notes = app.notes.trim(),
            )

            val docRef = colRef.document(app.id)
            docPath = docRef.path
            docRef.update(FirestoreApplicationDto.toUpdatePayload(cleaned)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateStatus(uid: String, id: String, status: ApplicationStatus): Result<Unit> {
        var docPath = "users/$uid/applications/$id"
        return try {
            val signedInUid = requireSignedInUid()
            require(signedInUid == uid && id.isNotBlank()) { "অবৈধ আবেদন আইডি" }
            val docRef = applicationsCollection(uid).document(id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to status.key,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteApplication(uid: String, id: String): Result<Unit> {
        var docPath = "users/$uid/applications/$id"
        return try {
            val signedInUid = requireSignedInUid()
            require(signedInUid == uid && id.isNotBlank()) { "অবৈধ আবেদন আইডি" }
            val docRef = applicationsCollection(uid).document(id)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDemoApplications(uid: String): Result<Unit> {
        val path = "users/$uid/applications"
        return try {
            val signedInUid = requireSignedInUid()
            require(signedInUid == uid) { "অননুমোদিত অ্যাক্সেস" }
            val demoDocs = applicationsCollection(uid)
                .whereEqualTo("demoEntry", true)
                .get()
                .await()
            if (!demoDocs.isEmpty) {
                val batch = db.batch()
                demoDocs.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun exportToJson(apps: List<JobApplication>): String {
        val arr = JSONArray()
        apps.forEach { app ->
            arr.put(
                JSONObject().apply {
                    put("company", app.company)
                    put("role", app.role)
                    put("region", app.region.key)
                    put("country", app.country)
                    put("city", app.city)
                    put("date", app.date)
                    put("followDate", app.followDate)
                    put("cvSet", app.cvSet.key)
                    put("cvFile", app.cvFile)
                    put("website", app.website)
                    put("email", app.email)
                    put("confirmCode", app.confirmCode)
                    put("status", app.status.key)
                    put("notes", app.notes)
                    put("createdAt", app.createdAt)
                },
            )
        }
        return arr.toString(2)
    }

    suspend fun importFromJson(uid: String, rawJson: String): Result<ImportSummary> {
        val path = "users/$uid/applications"
        return try {
            val signedInUid = requireSignedInUid()
            require(signedInUid == uid) { "অননুমোদিত অ্যাক্সেস" }

            val trimmed = rawJson.trim()
            val array = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val root = JSONObject(trimmed)
                root.optJSONArray("applications") ?: JSONArray()
            }

            val colRef = applicationsCollection(uid)
            val currentList = colRef.get().await().toObjects(
                FirestoreApplicationDto::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            ).map { it.toDomain() }.toMutableList()

            var imported = 0
            var skipped = 0
            val today = JobTrackerRules.todayIso()
            val toInsert = mutableListOf<JobApplication>()

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val company = obj.optString("company").trim()
                val role = obj.optString("role").trim()
                if (company.isBlank() || role.isBlank()) continue

                val email = obj.optString("email").trim()
                if (JobTrackerRules.findDuplicate(currentList, company, email) != null) {
                    skipped++
                    continue
                }

                val date = obj.optString("date").trim().ifBlank { today }
                val followDate = obj.optString("followDate").trim().ifBlank {
                    JobTrackerRules.addDaysIso(date, 15)
                }
                val rawWebsite = obj.optString("website").trim()
                val safeWebsite = JobTrackerRules.normalizeWebsite(rawWebsite).getOrDefault("")
                val cvFile = obj.optString("cvFile").trim().ifBlank {
                    JobTrackerRules.suggestCvFile(role, company)
                }

                val newApp = JobApplication(
                    ownerUid = uid,
                    company = company,
                    role = role,
                    region = Region.fromKey(obj.optString("region")),
                    country = obj.optString("country").trim(),
                    city = obj.optString("city").trim(),
                    date = date,
                    followDate = followDate,
                    cvSet = CvSet.fromKey(obj.optString("cvSet")),
                    cvFile = cvFile,
                    website = safeWebsite,
                    email = email,
                    confirmCode = obj.optString("confirmCode").trim(),
                    status = ApplicationStatus.fromKey(obj.optString("status")),
                    notes = obj.optString("notes").trim(),
                    isDemo = false,
                )

                toInsert.add(newApp)
                currentList.add(newApp)
                imported++
            }

            toInsert.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { item ->
                    val docRef = colRef.document()
                    batch.set(docRef, FirestoreApplicationDto.toCreatePayload(item, uid))
                }
                batch.commit().await()
            }

            Result.success(ImportSummary(importedCount = imported, skippedDuplicates = skipped))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(IllegalArgumentException("ইমপোর্ট ব্যর্থ: ${e.localizedMessage ?: "অবৈধ ফরম্যাট"}"))
        }
    }
}
