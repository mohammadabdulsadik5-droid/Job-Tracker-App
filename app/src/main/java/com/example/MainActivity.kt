package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AiApplicationExtractor
import com.example.data.FirebaseAiApplicationExtractor
import com.example.data.JobApplicationRepository
import com.example.data.UserPreferencesRepository
import com.example.data.configureAppCheck
import com.example.domain.UserAccount
import com.example.ui.AiExtractDialog
import com.example.ui.ApplicationFormSheet
import com.example.ui.DashboardScreen
import com.example.ui.JobTrackerViewModel
import com.example.ui.LoginScreen
import com.example.ui.theme.JobTrackerTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        val firestoreDbId = applicationContext.getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(firestoreDbId)
        val prefs = UserPreferencesRepository(applicationContext)
        val repository = JobApplicationRepository(firestore, prefs)
        val aiExtractor: AiApplicationExtractor = FirebaseAiApplicationExtractor()

        setContent {
            val isDarkTheme by prefs.isDarkTheme.collectAsStateWithLifecycle(initialValue = false)

            JobTrackerTheme(darkTheme = isDarkTheme) {
                JobTrackerRoot(
                    repository = repository,
                    prefs = prefs,
                    aiExtractor = aiExtractor,
                )
            }
        }
    }
}

@Composable
private fun JobTrackerRoot(
    repository: JobApplicationRepository,
    prefs: UserPreferencesRepository,
    aiExtractor: AiApplicationExtractor,
) {
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val activeUser = currentUser
    if (activeUser == null) {
        LoginScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
        )
    } else {
        key(activeUser.uid) {
            AuthenticatedJobTrackerContent(
                firebaseUser = activeUser,
                repository = repository,
                prefs = prefs,
                aiExtractor = aiExtractor,
            )
        }
    }
}

@Composable
private fun AuthenticatedJobTrackerContent(
    firebaseUser: FirebaseUser,
    repository: JobApplicationRepository,
    prefs: UserPreferencesRepository,
    aiExtractor: AiApplicationExtractor,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val userAccount = remember(firebaseUser.uid, firebaseUser.displayName, firebaseUser.email) {
        UserAccount(
            uid = firebaseUser.uid,
            displayName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                ?: firebaseUser.email?.substringBefore('@')
                ?: "ব্যবহারকারী",
            email = firebaseUser.email.orEmpty(),
        )
    }

    val viewModel: JobTrackerViewModel = viewModel(
        key = firebaseUser.uid,
        factory = JobTrackerViewModel.Factory(
            userAccount = userAccount,
            repository = repository,
            prefs = prefs,
            aiExtractor = aiExtractor,
        ),
    )

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val aiDialogState by viewModel.aiDialogState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.toastMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            try {
                val json = viewModel.buildExportJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                viewModel.notifyMessage("ব্যাকআপ JSON সেভ হয়েছে ✅")
            } catch (e: Exception) {
                viewModel.notifyMessage("এক্সপোর্ট ব্যর্থ: ${e.localizedMessage ?: "Error"}")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                }.orEmpty()
                viewModel.importJson(text)
            } catch (e: Exception) {
                viewModel.notifyMessage("ফাইল পড়া যায়নি: ${e.localizedMessage ?: "Error"}")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DashboardScreen(
            uiState = dashboardState,
            onSelectTab = viewModel::selectTab,
            onOpenStatInApplications = viewModel::openStatInApplications,
            onToggleTheme = viewModel::toggleTheme,
            onExportJson = { exportLauncher.launch("job-applications-backup.json") },
            onImportJson = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onSignOut = {
                scope.launch {
                    withContext(NonCancellable) {
                        try {
                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                        } catch (e: Exception) {
                            Log.w("Auth", "Failed to clear credential state: ${e.message}")
                        } finally {
                            Firebase.auth.signOut()
                        }
                    }
                }
            },
            onDeleteDemoData = viewModel::deleteDemoData,
            onSelectStat = viewModel::selectStatFilter,
            onSelectRegion = viewModel::selectRegion,
            onSelectCountry = viewModel::selectCountry,
            onSelectCity = viewModel::selectCity,
            onSearchChange = viewModel::updateSearchQuery,
            onSelectStatusFilter = viewModel::selectStatusFilter,
            onStatusChanged = viewModel::updateStatus,
            onMarkFollowUpSent = viewModel::markFollowUpSent,
            onEditApplication = viewModel::openEditForm,
            onDeleteApplication = viewModel::deleteApplication,
            onOpenAddForm = { viewModel.openAddForm() },
            onOpenAiDialog = viewModel::openAiDialog,
            onResetFilters = viewModel::resetFilters,
            onCopied = viewModel::notifyCopied,
            onMessage = viewModel::notifyMessage,
        )

        if (formState.isOpen) {
            ApplicationFormSheet(
                state = formState,
                onDismiss = viewModel::closeForm,
                onRegionChange = viewModel::onFormRegionChanged,
                onCountryChange = viewModel::onFormCountryChanged,
                onFormUpdate = viewModel::updateForm,
                onCompanyRoleChange = viewModel::onFormCompanyOrRoleChanged,
                onDateChange = viewModel::onFormDateChanged,
                onSave = viewModel::saveForm,
            )
        }

        if (aiDialogState.isOpen) {
            AiExtractDialog(
                state = aiDialogState,
                onDismiss = viewModel::closeAiDialog,
                onTextChange = viewModel::updateAiInputText,
                onAnalyzeText = viewModel::analyzeEmailText,
                onAnalyzeBitmap = viewModel::analyzeScreenshotBitmap,
                onApplyToForm = viewModel::applyAiResultToForm,
                onApplyStatusUpdate = viewModel::applyAiStatusUpdate,
                onImageError = viewModel::notifyMessage,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        )
    }
}
