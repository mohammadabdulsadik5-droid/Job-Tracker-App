package com.example

import com.example.domain.ApplicationStatus
import com.example.domain.JobApplication
import com.example.domain.JobTrackerRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JobTrackerRulesTest {

    @Test
    fun `bengali digits and date formatting match expected output`() {
        assertEquals("২০", JobTrackerRules.toBengaliDigits(20))
        assertEquals("১৫ অক্টোবর ২০২৬", JobTrackerRules.formatBengaliDate("2026-10-15"))
    }

    @Test
    fun `hr email detection handles prefixes domains and linkedin exclusion`() {
        assertTrue(JobTrackerRules.isHrEmail("hr@company.com"))
        assertTrue(JobTrackerRules.isHrEmail("careers@hotel.ae"))
        assertTrue(JobTrackerRules.isHrEmail("recrutamento@yarn.pt"))
        assertTrue(JobTrackerRules.isHrEmail("selezione@textile.it"))
        assertTrue(JobTrackerRules.isHrEmail("talent@acme.workday.com"))
        assertFalse(JobTrackerRules.isHrEmail("jobs-noreply@linkedin.com"))
        assertFalse(JobTrackerRules.isHrEmail("info@logistics.ae"))
    }

    @Test
    fun `auto cv suggestion chooses Hotel CV and Textile CV correctly`() {
        assertEquals("Hotel CV", JobTrackerRules.suggestCvFile("Room Attendant", "Al Bustan"))
        assertEquals("Textile CV", JobTrackerRules.suggestCvFile("Machine Operator", "Filati Co"))
        assertEquals("", JobTrackerRules.suggestCvFile("Warehouse Assistant", "Logistics FZE"))
    }

    @Test
    fun `follow up due and new application windows work properly`() {
        val today = "2026-11-23"
        assertTrue(JobTrackerRules.isFollowUpDue(ApplicationStatus.APPLIED, "2026-11-23", today))
        assertTrue(JobTrackerRules.isFollowUpDue(ApplicationStatus.APPLIED, "2026-11-10", today))
        assertFalse(JobTrackerRules.isFollowUpDue(ApplicationStatus.FOLLOWUP, "2026-11-10", today))
        assertFalse(JobTrackerRules.isFollowUpDue(ApplicationStatus.APPLIED, "2026-12-01", today))

        assertTrue(JobTrackerRules.isNewApplication("2026-11-15", today))
        assertFalse(JobTrackerRules.isNewApplication("2026-11-01", today))
    }

    @Test
    fun `website normalization adds https and rejects unsafe schemes`() {
        assertEquals("https://example.com", JobTrackerRules.normalizeWebsite("example.com").getOrThrow())
        assertEquals("http://example.com", JobTrackerRules.normalizeWebsite("http://example.com").getOrThrow())
        assertTrue(JobTrackerRules.normalizeWebsite("javascript:alert(1)").isFailure)
    }

    @Test
    fun `duplicate protection detects matching company or email`() {
        val existing = listOf(
            JobApplication(id = "1", company = "Demo Textile Co.", role = "Operator", email = "hr@demo.example"),
        )
        assertNotNull(JobTrackerRules.findDuplicate(existing, "demo textile co.", "other@test.com"))
        assertNotNull(JobTrackerRules.findDuplicate(existing, "New Co", "HR@demo.example"))
        assertNull(JobTrackerRules.findDuplicate(existing, "New Co", "new@test.com"))
    }
}
