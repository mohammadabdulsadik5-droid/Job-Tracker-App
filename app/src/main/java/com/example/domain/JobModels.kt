package com.example.domain

enum class Region(val key: String, val bnLabel: String) {
    ALL("all", "সব"),
    ASIA("asia", "এশিয়া"),
    EUROPE("europe", "ইউরোপ"),
    AFRICA("africa", "আফ্রিকা"),
    NAMERICA("namerica", "উত্তর আমেরিকা"),
    SAMERICA("samerica", "দক্ষিণ আমেরিকা"),
    OCEANIA("oceania", "ওশেনিয়া/অস্ট্রেলিয়া"),
    RUSSIA("russia", "রাশিয়া"),
    TURKEY("turkey", "তুরস্ক");

    companion object {
        val selectableRegions: List<Region> = entries.filter { it != ALL }

        fun fromKey(raw: String?): Region =
            entries.firstOrNull { it.key.equals(raw?.trim(), ignoreCase = true) } ?: ASIA
    }
}

enum class ApplicationStatus(val key: String, val bnLabel: String) {
    APPLIED("applied", "Applied"),
    FOLLOWUP("followup", "Follow-up পাঠিয়েছি"),
    INTERVIEW("interview", "Interview ডাক এসেছে"),
    REJECTED("rejected", "Rejected"),
    OFFER("offer", "Offer");

    companion object {
        fun fromKey(raw: String?): ApplicationStatus =
            entries.firstOrNull { it.key.equals(raw?.trim(), ignoreCase = true) } ?: APPLIED
    }
}

enum class CvSet(val key: String, val label: String) {
    DUBAI("Dubai", "Dubai"),
    EUROPE("Europe", "Europe"),
    OTHER("Other", "Other");

    companion object {
        fun fromKey(raw: String?): CvSet =
            entries.firstOrNull { it.key.equals(raw?.trim(), ignoreCase = true) } ?: DUBAI
    }
}

enum class StatFilter(val bnLabel: String) {
    ALL("মোট আবেদন"),
    INTERVIEW("ইন্টারভিউ"),
    DUE("ফলো-আপ বাকি"),
    FOLLOWUP("ফলো-আপ পাঠানো"),
    NEW("নতুন আবেদন (১৫ দিন)"),
    HR("HR ইমেইল"),
}

enum class StatusFilterOption(val bnLabel: String) {
    ALL("সব স্ট্যাটাস"),
    NEW("নতুন আবেদন"),
    HR("HR ইমেইল"),
    DUE("ফলো-আপ বাকি"),
    APPLIED("Applied"),
    FOLLOWUP("Follow-up পাঠিয়েছি"),
    INTERVIEW("Interview ডাক এসেছে"),
    REJECTED("Rejected"),
    OFFER("Offer"),
}

data class UserAccount(
    val uid: String,
    val displayName: String,
    val email: String,
)

data class JobApplication(
    val id: String = "",
    val ownerUid: String = "",
    val company: String = "",
    val role: String = "",
    val region: Region = Region.ASIA,
    val country: String = "",
    val city: String = "",
    val date: String = "",
    val followDate: String = "",
    val cvSet: CvSet = CvSet.DUBAI,
    val cvFile: String = "",
    val website: String = "",
    val email: String = "",
    val confirmCode: String = "",
    val status: ApplicationStatus = ApplicationStatus.APPLIED,
    val notes: String = "",
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

data class DashboardStats(
    val total: Int = 0,
    val interview: Int = 0,
    val due: Int = 0,
    val followup: Int = 0,
    val newApps: Int = 0,
    val hrEmails: Int = 0,
)
