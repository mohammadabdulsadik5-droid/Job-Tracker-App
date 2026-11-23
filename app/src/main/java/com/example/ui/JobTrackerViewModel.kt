package com.example.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AiApplicationExtractor
import com.example.data.AiExtractionResult
import com.example.data.JobApplicationRepository
import com.example.data.UserPreferencesRepository
import com.example.domain.ApplicationStatus
import com.example.domain.CvSet
import com.example.domain.DashboardStats
import com.example.domain.GeographyCatalog
import com.example.domain.JobApplication
import com.example.domain.JobTrackerRules
import com.example.domain.Region
import com.example.domain.StatFilter
import com.example.domain.StatusFilterOption
import com.example.domain.UserAccount
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val bnLabel: String) {
    HOME("হোম"),
    APPLICATIONS("আবেদন"),
    FOLLOWUPS("ফলো-আপ"),
    SETTINGS("সেটিংস"),
}

data class ChipCount(
    val label: String,
    val count: Int,
)

private sealed interface ApplicationsStreamState {
    data object Loading : ApplicationsStreamState
    data class Success(val items: List<JobApplication>) : ApplicationsStreamState
    data class Error(val message: String) : ApplicationsStreamState
}

data class DashboardUiState(
    val currentUser: UserAccount? = null,
    val isDarkTheme: Boolean = false,
    val isLoading: Boolean = true,
    val streamErrorMessage: String? = null,
    val selectedTab: AppTab = AppTab.HOME,
    val allApplications: List<JobApplication> = emptyList(),
    val filteredApplications: List<JobApplication> = emptyList(),
    val dueApplications: List<JobApplication> = emptyList(),
    val stats: DashboardStats = DashboardStats(),
    val activeStatFilter: StatFilter = StatFilter.ALL,
    val selectedRegion: Region = Region.ALL,
    val selectedCountry: String? = null,
    val selectedCity: String? = null,
    val countryChips: List<ChipCount> = emptyList(),
    val cityChips: List<ChipCount> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: StatusFilterOption = StatusFilterOption.ALL,
    val hasDemoData: Boolean = false,
)

data class FormSheetState(
    val isOpen: Boolean = false,
    val isSaving: Boolean = false,
    val editingId: String? = null,
    val editingCreatedAt: Long = 0L,
    val editingIsDemo: Boolean = false,
    val region: Region = Region.ASIA,
    val country: String = "UAE",
    val city: String = "Dubai",
    val customCityMode: Boolean = false,
    val customCityInput: String = "",
    val company: String = "",
    val role: String = "",
    val website: String = "",
    val email: String = "",
    val confirmCode: String = "",
    val date: String = JobTrackerRules.todayIso(),
    val followDate: String = JobTrackerRules.addDaysIso(JobTrackerRules.todayIso(), 15),
    val followDateManuallyEdited: Boolean = false,
    val cvSet: CvSet = CvSet.DUBAI,
    val cvFile: String = "",
    val cvFileManuallyEdited: Boolean = false,
    val status: ApplicationStatus = ApplicationStatus.APPLIED,
    val notes: String = "",
    val errorMessage: String? = null,
)

data class AiDialogState(
    val isOpen: Boolean = false,
    val inputText: String = "",
    val selectedImageLabel: String? = null,
    val isAnalyzing: Boolean = false,
    val result: AiExtractionResult? = null,
    val errorMessage: String? = null,
)

@OptIn(FlowPreview::class)
class JobTrackerViewModel(
    private val currentUserAccount: UserAccount,
    private val repository: JobApplicationRepository,
    private val prefs: UserPreferencesRepository,
    private val aiExtractor: AiApplicationExtractor,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _debouncedQuery = _searchQuery.debounce(200L)

    private val _selectedTab = MutableStateFlow(AppTab.HOME)
    private val _activeStatFilter = MutableStateFlow(StatFilter.ALL)
    private val _selectedRegion = MutableStateFlow(Region.ALL)
    private val _selectedCountry = MutableStateFlow<String?>(null)
    private val _selectedCity = MutableStateFlow<String?>(null)
    private val _statusFilter = MutableStateFlow(StatusFilterOption.ALL)

    private val _formState = MutableStateFlow(FormSheetState())
    val formState: StateFlow<FormSheetState> = _formState.asStateFlow()

    private val _aiDialogState = MutableStateFlow(AiDialogState())
    val aiDialogState: StateFlow<AiDialogState> = _aiDialogState.asStateFlow()

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessages: SharedFlow<String> = _toastMessages.asSharedFlow()

    val isDarkTheme: StateFlow<Boolean> = prefs.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    private val applicationsStream: StateFlow<ApplicationsStreamState> =
        repository.observeApplications(currentUserAccount.uid)
            .map<List<JobApplication>, ApplicationsStreamState> { ApplicationsStreamState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                emit(
                    ApplicationsStreamState.Error(
                        rootError.message ?: "ফায়ারস্টোর থেকে ডেটা লোড করা যায়নি",
                    ),
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = ApplicationsStreamState.Loading,
            )

    init {
        viewModelScope.launch {
            repository.ensureDemoDataForNewUser(currentUserAccount.uid)
                .onFailure { err ->
                    _toastMessages.tryEmit(err.localizedMessage ?: "ডেমো ডেটা সেভ করা যায়নি")
                }
        }
    }

    private data class FilterParams(
        val tab: AppTab,
        val query: String,
        val statFilter: StatFilter,
        val region: Region,
        val country: String?,
        val city: String?,
        val statusOption: StatusFilterOption,
    )

    private val filterParamsFlow = combine(
        combine(_selectedTab, _debouncedQuery, _activeStatFilter) { tab, q, stat ->
            Triple(tab, q, stat)
        },
        combine(_selectedRegion, _selectedCountry, _selectedCity) { region, country, city ->
            Triple(region, country, city)
        },
        _statusFilter,
    ) { first, second, status ->
        FilterParams(
            tab = first.first,
            query = first.second,
            statFilter = first.third,
            region = second.first,
            country = second.second,
            city = second.third,
            statusOption = status,
        )
    }

    val dashboardState: StateFlow<DashboardUiState> = combine(
        isDarkTheme,
        applicationsStream,
        _searchQuery,
        filterParamsFlow,
    ) { dark, streamState, rawQuery, params ->
        val apps = when (streamState) {
            is ApplicationsStreamState.Success -> streamState.items
            else -> emptyList()
        }
        val isLoading = streamState is ApplicationsStreamState.Loading
        val streamError = (streamState as? ApplicationsStreamState.Error)?.message

        val today = JobTrackerRules.todayIso()
        val dueList = apps.filter { JobTrackerRules.isFollowUpDue(it.status, it.followDate, today) }
        val stats = DashboardStats(
            total = apps.size,
            interview = apps.count { it.status == ApplicationStatus.INTERVIEW },
            due = dueList.size,
            followup = apps.count { it.status == ApplicationStatus.FOLLOWUP },
            newApps = apps.count { JobTrackerRules.isNewApplication(it.date, today) },
            hrEmails = apps.count { JobTrackerRules.isHrEmail(it.email) },
        )

        val regionApps = if (params.region == Region.ALL) {
            apps
        } else {
            apps.filter { it.region == params.region }
        }

        val countryChips = if (params.region == Region.ALL) {
            emptyList()
        } else {
            val countsByCountry = regionApps.groupingBy { it.country.trim() }.eachCount()
            val catalogCountries = GeographyCatalog.countriesForRegion(params.region)
            val allCountryNames = (catalogCountries + countsByCountry.keys.filter { it.isNotBlank() }).distinct()
            val withApps = allCountryNames.filter { (countsByCountry[it] ?: 0) > 0 }
            val withoutApps = allCountryNames.filter { (countsByCountry[it] ?: 0) == 0 }
            (withApps + withoutApps).map { name ->
                ChipCount(label = name, count = countsByCountry[name] ?: 0)
            }
        }

        val cityChips = if (params.country.isNullOrBlank()) {
            emptyList()
        } else {
            val countryApps = regionApps.filter { it.country.equals(params.country, ignoreCase = true) }
            val countsByCity = countryApps.groupingBy { it.city.trim() }.eachCount()
            val catalogCities = GeographyCatalog.citiesForCountry(params.country)
            val allCities = (catalogCities + countsByCity.keys.filter { it.isNotBlank() }).distinct()
            val withApps = allCities.filter { (countsByCity[it] ?: 0) > 0 }
            val withoutApps = allCities.filter { (countsByCity[it] ?: 0) == 0 }
            (withApps + withoutApps).map { name ->
                ChipCount(label = name, count = countsByCity[name] ?: 0)
            }
        }

        val q = params.query.trim().lowercase()
        val filtered = apps.filter { app ->
            val matchesStat = when (params.statFilter) {
                StatFilter.ALL -> true
                StatFilter.INTERVIEW -> app.status == ApplicationStatus.INTERVIEW
                StatFilter.DUE -> JobTrackerRules.isFollowUpDue(app.status, app.followDate, today)
                StatFilter.FOLLOWUP -> app.status == ApplicationStatus.FOLLOWUP
                StatFilter.NEW -> JobTrackerRules.isNewApplication(app.date, today)
                StatFilter.HR -> JobTrackerRules.isHrEmail(app.email)
            }
            val matchesRegion = params.region == Region.ALL || app.region == params.region
            val matchesCountry = params.country.isNullOrBlank() ||
                app.country.equals(params.country, ignoreCase = true)
            val matchesCity = params.city.isNullOrBlank() ||
                app.city.equals(params.city, ignoreCase = true)
            val matchesStatusDropdown = when (params.statusOption) {
                StatusFilterOption.ALL -> true
                StatusFilterOption.NEW -> JobTrackerRules.isNewApplication(app.date, today)
                StatusFilterOption.HR -> JobTrackerRules.isHrEmail(app.email)
                StatusFilterOption.DUE -> JobTrackerRules.isFollowUpDue(app.status, app.followDate, today)
                StatusFilterOption.APPLIED -> app.status == ApplicationStatus.APPLIED
                StatusFilterOption.FOLLOWUP -> app.status == ApplicationStatus.FOLLOWUP
                StatusFilterOption.INTERVIEW -> app.status == ApplicationStatus.INTERVIEW
                StatusFilterOption.REJECTED -> app.status == ApplicationStatus.REJECTED
                StatusFilterOption.OFFER -> app.status == ApplicationStatus.OFFER
            }
            val matchesSearch = q.isEmpty() ||
                app.company.lowercase().contains(q) ||
                app.role.lowercase().contains(q) ||
                app.country.lowercase().contains(q) ||
                app.city.lowercase().contains(q) ||
                app.email.lowercase().contains(q) ||
                app.confirmCode.lowercase().contains(q)

            matchesStat && matchesRegion && matchesCountry && matchesCity && matchesStatusDropdown && matchesSearch
        }

        DashboardUiState(
            currentUser = currentUserAccount,
            isDarkTheme = dark,
            isLoading = isLoading,
            streamErrorMessage = streamError,
            selectedTab = params.tab,
            allApplications = apps,
            filteredApplications = filtered,
            dueApplications = dueList,
            stats = stats,
            activeStatFilter = params.statFilter,
            selectedRegion = params.region,
            selectedCountry = params.country,
            selectedCity = params.city,
            countryChips = countryChips,
            cityChips = cityChips,
            searchQuery = rawQuery,
            statusFilter = params.statusOption,
            hasDemoData = apps.any { it.isDemo },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DashboardUiState(currentUser = currentUserAccount),
    )

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun openStatInApplications(stat: StatFilter) {
        _activeStatFilter.value = stat
        _selectedTab.value = AppTab.APPLICATIONS
    }

    fun toggleTheme() {
        viewModelScope.launch {
            prefs.setDarkTheme(!isDarkTheme.value)
        }
    }

    fun selectStatFilter(stat: StatFilter) {
        _activeStatFilter.value = if (_activeStatFilter.value == stat && stat != StatFilter.ALL) {
            StatFilter.ALL
        } else {
            stat
        }
    }

    fun selectRegion(region: Region) {
        _selectedRegion.value = region
        _selectedCountry.value = null
        _selectedCity.value = null
    }

    fun selectCountry(country: String?) {
        _selectedCountry.value = if (_selectedCountry.value == country) null else country
        _selectedCity.value = null
    }

    fun selectCity(city: String?) {
        _selectedCity.value = if (_selectedCity.value == city) null else city
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectStatusFilter(option: StatusFilterOption) {
        _statusFilter.value = option
    }

    fun resetFilters() {
        _activeStatFilter.value = StatFilter.ALL
        _selectedRegion.value = Region.ALL
        _selectedCountry.value = null
        _selectedCity.value = null
        _searchQuery.value = ""
        _statusFilter.value = StatusFilterOption.ALL
    }

    fun deleteDemoData() {
        viewModelScope.launch {
            repository.deleteDemoApplications(currentUserAccount.uid)
                .onSuccess {
                    _toastMessages.tryEmit("ডেমো ডেটা মুছে ফেলা হয়েছে")
                }
                .onFailure { err ->
                    _toastMessages.tryEmit(err.message ?: "ডেমো ডেটা মুছতে সমস্যা হয়েছে")
                }
        }
    }

    fun updateStatus(app: JobApplication, newStatus: ApplicationStatus) {
        viewModelScope.launch {
            repository.updateStatus(currentUserAccount.uid, app.id, newStatus)
                .onSuccess {
                    _toastMessages.tryEmit("স্ট্যাটাস আপডেট: ${newStatus.bnLabel}")
                }
                .onFailure { err ->
                    _toastMessages.tryEmit(err.message ?: "স্ট্যাটাস আপডেট করা যায়নি")
                }
        }
    }

    fun markFollowUpSent(app: JobApplication) {
        viewModelScope.launch {
            repository.updateStatus(currentUserAccount.uid, app.id, ApplicationStatus.FOLLOWUP)
                .onSuccess {
                    _toastMessages.tryEmit("ফলো-আপ পাঠানো হিসেবে সেভ হয়েছে ✅")
                }
                .onFailure { err ->
                    _toastMessages.tryEmit(err.message ?: "আপডেট করা যায়নি")
                }
        }
    }

    fun deleteApplication(app: JobApplication) {
        viewModelScope.launch {
            repository.deleteApplication(currentUserAccount.uid, app.id)
                .onSuccess {
                    _toastMessages.tryEmit("\"${app.company}\" মুছে ফেলা হয়েছে")
                }
                .onFailure { err ->
                    _toastMessages.tryEmit(err.message ?: "মুছতে সমস্যা হয়েছে")
                }
        }
    }

    fun notifyCopied(label: String) {
        _toastMessages.tryEmit("$label কপি হয়েছে ✅")
    }

    fun notifyMessage(message: String) {
        _toastMessages.tryEmit(message)
    }

    fun buildExportJson(): String {
        return repository.exportToJson(dashboardState.value.allApplications)
    }

    fun importJson(rawJson: String) {
        viewModelScope.launch {
            val result = repository.importFromJson(currentUserAccount.uid, rawJson)
            result.onSuccess { summary ->
                val msg = buildString {
                    append("${JobTrackerRules.toBengaliDigits(summary.importedCount)}টি আবেদন ইমপোর্ট হয়েছে")
                    if (summary.skippedDuplicates > 0) {
                        append(" (${JobTrackerRules.toBengaliDigits(summary.skippedDuplicates)}টি ডুপ্লিকেট বাদ দেওয়া হয়েছে)")
                    }
                }
                _toastMessages.tryEmit(msg)
            }.onFailure { err ->
                _toastMessages.tryEmit(err.message ?: "ইমপোর্ট ব্যর্থ হয়েছে")
            }
        }
    }

    // --- Form Sheet Operations ---

    fun openAddForm(prefill: JobApplication? = null) {
        val today = JobTrackerRules.todayIso()
        if (prefill != null) {
            val knownCities = GeographyCatalog.citiesForCountry(prefill.country)
            val isCustomCity = prefill.city.isNotBlank() && knownCities.none {
                it.equals(prefill.city, ignoreCase = true)
            }
            _formState.value = FormSheetState(
                isOpen = true,
                region = prefill.region,
                country = prefill.country.ifBlank {
                    GeographyCatalog.countriesForRegion(prefill.region).firstOrNull().orEmpty()
                },
                city = if (isCustomCity) "" else prefill.city,
                customCityMode = isCustomCity,
                customCityInput = if (isCustomCity) prefill.city else "",
                company = prefill.company,
                role = prefill.role,
                website = prefill.website,
                email = prefill.email,
                confirmCode = prefill.confirmCode,
                date = prefill.date.ifBlank { today },
                followDate = prefill.followDate.ifBlank { JobTrackerRules.addDaysIso(today, 15) },
                followDateManuallyEdited = false,
                cvSet = prefill.cvSet,
                cvFile = prefill.cvFile,
                cvFileManuallyEdited = prefill.cvFile.isNotBlank(),
                status = prefill.status,
                notes = prefill.notes,
            )
        } else {
            val defaultRegion = if (_selectedRegion.value == Region.ALL) Region.ASIA else _selectedRegion.value
            val defaultCountry = _selectedCountry.value
                ?: GeographyCatalog.countriesForRegion(defaultRegion).firstOrNull().orEmpty()
            val defaultCity = _selectedCity.value
                ?: GeographyCatalog.citiesForCountry(defaultCountry).firstOrNull().orEmpty()
            val defaultCvSet = when (defaultRegion) {
                Region.EUROPE -> CvSet.EUROPE
                Region.ASIA -> if (defaultCountry == "UAE") CvSet.DUBAI else CvSet.OTHER
                else -> CvSet.OTHER
            }
            _formState.value = FormSheetState(
                isOpen = true,
                region = defaultRegion,
                country = defaultCountry,
                city = defaultCity,
                date = today,
                followDate = JobTrackerRules.addDaysIso(today, 15),
                cvSet = defaultCvSet,
            )
        }
    }

    fun openEditForm(app: JobApplication) {
        val knownCities = GeographyCatalog.citiesForCountry(app.country)
        val isCustomCity = app.city.isNotBlank() && knownCities.none {
            it.equals(app.city, ignoreCase = true)
        }
        _formState.value = FormSheetState(
            isOpen = true,
            editingId = app.id,
            editingCreatedAt = app.createdAt,
            editingIsDemo = app.isDemo,
            region = app.region,
            country = app.country,
            city = if (isCustomCity) "" else app.city,
            customCityMode = isCustomCity,
            customCityInput = if (isCustomCity) app.city else "",
            company = app.company,
            role = app.role,
            website = app.website,
            email = app.email,
            confirmCode = app.confirmCode,
            date = app.date,
            followDate = app.followDate,
            followDateManuallyEdited = true,
            cvSet = app.cvSet,
            cvFile = app.cvFile,
            cvFileManuallyEdited = app.cvFile.isNotBlank(),
            status = app.status,
            notes = app.notes,
        )
    }

    fun closeForm() {
        _formState.value = FormSheetState(isOpen = false)
    }

    fun updateForm(transform: (FormSheetState) -> FormSheetState) {
        _formState.value = transform(_formState.value).copy(errorMessage = null)
    }

    fun onFormRegionChanged(region: Region) {
        val countries = GeographyCatalog.countriesForRegion(region)
        val newCountry = countries.firstOrNull().orEmpty()
        val newCity = GeographyCatalog.citiesForCountry(newCountry).firstOrNull().orEmpty()
        val newCvSet = when (region) {
            Region.EUROPE -> CvSet.EUROPE
            Region.ASIA -> if (newCountry == "UAE") CvSet.DUBAI else CvSet.OTHER
            else -> CvSet.OTHER
        }
        updateForm {
            it.copy(
                region = region,
                country = newCountry,
                city = newCity,
                customCityMode = newCity.isEmpty(),
                customCityInput = "",
                cvSet = if (it.editingId == null) newCvSet else it.cvSet,
            )
        }
    }

    fun onFormCountryChanged(country: String) {
        val cities = GeographyCatalog.citiesForCountry(country)
        val firstCity = cities.firstOrNull().orEmpty()
        updateForm {
            it.copy(
                country = country,
                city = firstCity,
                customCityMode = cities.isEmpty(),
                customCityInput = "",
            )
        }
    }

    fun onFormCompanyOrRoleChanged(company: String, role: String) {
        updateForm { state ->
            val suggested = if (!state.cvFileManuallyEdited) {
                JobTrackerRules.suggestCvFile(role, company).ifBlank { state.cvFile }
            } else {
                state.cvFile
            }
            state.copy(company = company, role = role, cvFile = suggested)
        }
    }

    fun onFormDateChanged(newDateIso: String) {
        updateForm { state ->
            val newFollow = if (!state.followDateManuallyEdited) {
                JobTrackerRules.addDaysIso(newDateIso, 15)
            } else {
                state.followDate
            }
            state.copy(date = newDateIso, followDate = newFollow)
        }
    }

    fun saveForm() {
        val state = _formState.value
        if (state.isSaving) return
        if (state.company.isBlank()) {
            _formState.value = state.copy(errorMessage = "কোম্পানির নাম অবশ্যই লিখতে হবে")
            return
        }
        if (state.role.isBlank()) {
            _formState.value = state.copy(errorMessage = "পদের নাম (Role) অবশ্যই লিখতে হবে")
            return
        }
        val finalCity = if (state.customCityMode) state.customCityInput.trim() else state.city.trim()
        val app = JobApplication(
            id = state.editingId.orEmpty(),
            ownerUid = currentUserAccount.uid,
            company = state.company.trim(),
            role = state.role.trim(),
            region = state.region,
            country = state.country.trim(),
            city = finalCity,
            date = state.date.trim().ifBlank { JobTrackerRules.todayIso() },
            followDate = state.followDate.trim(),
            cvSet = state.cvSet,
            cvFile = state.cvFile.trim(),
            website = state.website.trim(),
            email = state.email.trim(),
            confirmCode = state.confirmCode.trim(),
            status = state.status,
            notes = state.notes.trim(),
            isDemo = state.editingIsDemo,
            createdAt = if (state.editingId != null) state.editingCreatedAt else System.currentTimeMillis(),
        )

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isSaving = true, errorMessage = null)
            val result = if (state.editingId == null) {
                repository.addApplication(app).map { Unit }
            } else {
                repository.updateApplication(app)
            }
            result.onSuccess {
                val msg = if (state.editingId == null) "নতুন আবেদন সেভ হয়েছে ✅" else "আবেদন আপডেট হয়েছে ✅"
                closeForm()
                _toastMessages.tryEmit(msg)
            }.onFailure { err ->
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    errorMessage = err.message ?: "সেভ করা যায়নি",
                )
            }
        }
    }

    // --- AI Dialog Operations ---

    fun openAiDialog() {
        _aiDialogState.value = AiDialogState(isOpen = true)
    }

    fun closeAiDialog() {
        _aiDialogState.value = AiDialogState(isOpen = false)
    }

    fun updateAiInputText(text: String) {
        _aiDialogState.value = _aiDialogState.value.copy(
            inputText = text,
            errorMessage = null,
        )
    }

    fun analyzeEmailText() {
        val text = _aiDialogState.value.inputText
        if (text.isBlank()) {
            _toastMessages.tryEmit("ইমেইল টেক্সট পেস্ট করুন")
            return
        }
        viewModelScope.launch {
            _aiDialogState.value = _aiDialogState.value.copy(
                isAnalyzing = true,
                errorMessage = null,
                result = null,
            )
            val result = aiExtractor.extractFromEmailText(
                ownerUid = currentUserAccount.uid,
                emailText = text,
                existingApps = dashboardState.value.allApplications,
            )
            result.onSuccess { extraction ->
                _aiDialogState.value = _aiDialogState.value.copy(
                    isAnalyzing = false,
                    result = extraction,
                    errorMessage = null,
                )
            }.onFailure { err ->
                _aiDialogState.value = _aiDialogState.value.copy(
                    isAnalyzing = false,
                    errorMessage = err.message ?: "AI অ্যানালাইসিস ব্যর্থ হয়েছে।",
                )
            }
        }
    }

    fun analyzeScreenshotBitmap(bitmap: Bitmap, label: String) {
        viewModelScope.launch {
            _aiDialogState.value = _aiDialogState.value.copy(
                selectedImageLabel = label,
                isAnalyzing = true,
                errorMessage = null,
                result = null,
            )
            val result = aiExtractor.extractFromScreenshot(
                ownerUid = currentUserAccount.uid,
                bitmap = bitmap,
                existingApps = dashboardState.value.allApplications,
            )
            result.onSuccess { extraction ->
                _aiDialogState.value = _aiDialogState.value.copy(
                    isAnalyzing = false,
                    result = extraction,
                    errorMessage = null,
                )
            }.onFailure { err ->
                _aiDialogState.value = _aiDialogState.value.copy(
                    isAnalyzing = false,
                    errorMessage = err.message ?: "স্ক্রিনশট অ্যানালাইসিস ব্যর্থ হয়েছে।",
                )
            }
        }
    }

    fun applyAiResultToForm() {
        val res = _aiDialogState.value.result ?: return
        closeAiDialog()
        openAddForm(prefill = res.extractedApp)
    }

    fun applyAiStatusUpdate() {
        val res = _aiDialogState.value.result ?: return
        val target = res.suggestedStatusUpdateTarget ?: return
        val newStatus = res.extractedApp.status
        viewModelScope.launch {
            repository.updateStatus(currentUserAccount.uid, target.id, newStatus)
                .onSuccess {
                    closeAiDialog()
                    _toastMessages.tryEmit("\"${target.company}\" এর স্ট্যাটাস '${newStatus.bnLabel}' করা হয়েছে ✅")
                }
                .onFailure { err ->
                    _aiDialogState.value = _aiDialogState.value.copy(
                        errorMessage = err.message ?: "স্ট্যাটাস আপডেট করা যায়নি",
                    )
                }
        }
    }

    class Factory(
        private val userAccount: UserAccount,
        private val repository: JobApplicationRepository,
        private val prefs: UserPreferencesRepository,
        private val aiExtractor: AiApplicationExtractor,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return JobTrackerViewModel(userAccount, repository, prefs, aiExtractor) as T
        }
    }
}
