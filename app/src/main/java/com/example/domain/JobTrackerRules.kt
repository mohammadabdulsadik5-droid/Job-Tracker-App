package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object JobTrackerRules {

    private val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    private val bnMonths = listOf(
        "জানুয়ারি",
        "ফেব্রুয়ারি",
        "মার্চ",
        "এপ্রিল",
        "মে",
        "জুন",
        "জুলাই",
        "আগস্ট",
        "সেপ্টেম্বর",
        "অক্টোবর",
        "নভেম্বর",
        "ডিসেম্বর",
    )

    private val hrLocalPrefixes = listOf("job", "career", "recruit")
    private val hrLocalExact = setOf(
        "hr", "rh", "drh", "cv",
        "recrutamento", "selezione", "personale", "bewerbung", "recursos",
    )
    private val hrDomainTokens = listOf("recruit", "hrsystem", "workday")

    private val hotelKeywords = listOf(
        "hotel", "resort", "room attendant", "housekeeping", "public area",
    )
    private val textileKeywords = listOf(
        "textile", "filati", "cotton", "yarn", "spinning", "machine operator", "mill", "wool", "têxtil", "textil",
    )

    fun toBengaliDigits(value: Int): String = toBengaliDigits(value.toString())

    fun toBengaliDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(bnDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun todayIso(): String {
        val cal = Calendar.getInstance()
        return formatCalendarIso(cal)
    }

    fun addDaysIso(isoDate: String, days: Int): String {
        val cal = parseIsoCalendar(isoDate) ?: Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return formatCalendarIso(cal)
    }

    fun millisToIso(epochMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        return formatCalendarIso(cal)
    }

    fun isoToMillis(isoDate: String): Long? {
        val cal = parseIsoCalendar(isoDate) ?: return null
        return cal.timeInMillis
    }

    fun formatBengaliDate(isoDate: String): String {
        val cal = parseIsoCalendar(isoDate) ?: return toBengaliDigits(isoDate)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthIdx = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        val monthName = bnMonths.getOrElse(monthIdx) { "" }
        return "${toBengaliDigits(day)} $monthName ${toBengaliDigits(year)}"
    }

    fun isFollowUpDue(
        status: ApplicationStatus,
        followDate: String,
        todayIso: String = todayIso(),
    ): Boolean {
        if (status != ApplicationStatus.APPLIED) return false
        val normalizedFollow = followDate.trim()
        if (normalizedFollow.length != 10) return false
        return normalizedFollow <= todayIso
    }

    fun isNewApplication(
        applicationDate: String,
        todayIso: String = todayIso(),
    ): Boolean {
        val normalized = applicationDate.trim()
        if (normalized.length != 10) return false
        val cutoffIso = addDaysIso(todayIso, -15)
        return normalized >= cutoffIso && normalized <= todayIso
    }

    fun extractEmails(rawEmail: String): List<String> {
        if (rawEmail.isBlank()) return emptyList()
        return rawEmail
            .split(',', ';', '\n', ' ')
            .map { it.trim() }
            .filter { it.contains('@') && it.indexOf('@') > 0 && it.indexOf('@') < it.length - 1 }
            .distinctBy { it.lowercase(Locale.US) }
    }

    fun isHrEmail(rawEmail: String): Boolean {
        val emails = extractEmails(rawEmail)
        return emails.any { email -> isSingleEmailHr(email) }
    }

    private fun isSingleEmailHr(email: String): Boolean {
        val parts = email.trim().lowercase(Locale.US).split('@')
        if (parts.size != 2) return false
        val local = parts[0]
        val domain = parts[1]

        if (domain.contains("linkedin.com")) return false
        if (hrDomainTokens.any { domain.contains(it) }) return true

        if (local in hrLocalExact) return true
        if (hrLocalPrefixes.any { local.startsWith(it) }) return true

        val localTokens = local.split('.', '_', '-', '+')
        if (localTokens.any { token ->
                token in hrLocalExact || hrLocalPrefixes.any { token.startsWith(it) }
            }
        ) {
            return true
        }
        return false
    }

    fun suggestCvFile(role: String, company: String): String {
        val combined = "$role $company".lowercase(Locale.US)
        if (hotelKeywords.any { combined.contains(it) }) return "Hotel CV"
        if (textileKeywords.any { combined.contains(it) }) return "Textile CV"
        return ""
    }

    fun normalizeWebsite(raw: String): Result<String> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return Result.success("")
        val lower = trimmed.lowercase(Locale.US)
        if (lower.contains("://")) {
            return if (lower.startsWith("http://") || lower.startsWith("https://")) {
                Result.success(trimmed)
            } else {
                Result.failure(IllegalArgumentException("শুধু http:// বা https:// ওয়েবসাইট লিংক দিন"))
            }
        }
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("ftp:")) {
            return Result.failure(IllegalArgumentException("অবৈধ ওয়েবসাইট লিংক"))
        }
        return Result.success("https://$trimmed")
    }

    fun findDuplicate(
        existing: List<JobApplication>,
        company: String,
        email: String,
        excludeId: String? = null,
    ): JobApplication? {
        val targetCompany = company.trim().lowercase(Locale.US)
        val targetEmails = extractEmails(email).map { it.lowercase(Locale.US) }.toSet()

        return existing.firstOrNull { item ->
            if (excludeId != null && item.id == excludeId) return@firstOrNull false
            val sameCompany = targetCompany.isNotEmpty() &&
                item.company.trim().lowercase(Locale.US) == targetCompany
            val itemEmails = extractEmails(item.email).map { it.lowercase(Locale.US) }.toSet()
            val sameEmail = targetEmails.isNotEmpty() && targetEmails.any { it in itemEmails }
            sameCompany || sameEmail
        }
    }

    fun buildDemoApplications(
        ownerUid: String,
        todayIso: String = todayIso(),
    ): List<JobApplication> {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L

        fun demoItem(
            daysAgo: Int,
            company: String,
            role: String,
            region: Region,
            country: String,
            city: String,
            status: ApplicationStatus,
            email: String,
            cvSet: CvSet,
            website: String,
            confirmCode: String,
            notes: String,
        ): JobApplication {
            val appDate = addDaysIso(todayIso, -daysAgo)
            val followDate = addDaysIso(appDate, 15)
            return JobApplication(
                ownerUid = ownerUid,
                company = company,
                role = role,
                region = region,
                country = country,
                city = city,
                date = appDate,
                followDate = followDate,
                cvSet = cvSet,
                cvFile = suggestCvFile(role, company).ifBlank { "General CV" },
                website = website,
                email = email,
                confirmCode = confirmCode,
                status = status,
                notes = notes,
                isDemo = true,
                createdAt = now - (daysAgo * dayMs),
            )
        }

        return listOf(
            demoItem(
                daysAgo = 2,
                company = "Demo Textile Co.",
                role = "Production / Machine Operator",
                region = Region.EUROPE,
                country = "Italy",
                city = "Milan",
                status = ApplicationStatus.APPLIED,
                email = "hr@demo-textile.example",
                cvSet = CvSet.EUROPE,
                website = "https://demo-textile.example",
                confirmCode = "IT-TX-204",
                notes = "নমুনা আবেদন: ইউরোপ টেক্সটাইল ফ্যাক্টরি",
            ),
            demoItem(
                daysAgo = 5,
                company = "Demo Hotel Dubai",
                role = "Room Attendant",
                region = Region.ASIA,
                country = "UAE",
                city = "Dubai",
                status = ApplicationStatus.REJECTED,
                email = "careers@demo-hotel.example",
                cvSet = CvSet.DUBAI,
                website = "https://demo-hotel.example",
                confirmCode = "DXB-HT-881",
                notes = "নমুনা আবেদন: দুবাই হসপিটালিটি টিম",
            ),
            demoItem(
                daysAgo = 8,
                company = "Demo Logistics FZE",
                role = "Warehouse Assistant",
                region = Region.ASIA,
                country = "UAE",
                city = "Dubai",
                status = ApplicationStatus.INTERVIEW,
                email = "info@demo-logistics.example",
                cvSet = CvSet.DUBAI,
                website = "https://demo-logistics.example",
                confirmCode = "DXB-LG-319",
                notes = "নমুনা আবেদন: জুম ইন্টারভিউ নির্ধারিত",
            ),
            demoItem(
                daysAgo = 20,
                company = "Demo Spinning Mills",
                role = "Spinning Operator",
                region = Region.TURKEY,
                country = "Turkey",
                city = "Istanbul",
                status = ApplicationStatus.FOLLOWUP,
                email = "cv@demo-spinning.example",
                cvSet = CvSet.OTHER,
                website = "https://demo-spinning.example",
                confirmCode = "TR-SP-112",
                notes = "নমুনা আবেদন: ফলো-আপ ইমেইল পাঠানো হয়েছে",
            ),
            demoItem(
                daysAgo = 30,
                company = "Demo Yarn Ltd",
                role = "Production / Machine Operator",
                region = Region.EUROPE,
                country = "Portugal",
                city = "Porto",
                status = ApplicationStatus.APPLIED,
                email = "recrutamento@demo-yarn.example",
                cvSet = CvSet.EUROPE,
                website = "https://demo-yarn.example",
                confirmCode = "PT-YN-507",
                notes = "নমুনা আবেদন: ফলো-আপের সময় পার হয়েছে",
            ),
        )
    }

    private fun formatCalendarIso(cal: Calendar): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    private fun parseIsoCalendar(isoDate: String): Calendar? {
        val trimmed = isoDate.trim()
        if (trimmed.length != 10) return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val date = sdf.parse(trimmed) ?: return null
            Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (_: Exception) {
            null
        }
    }
}
