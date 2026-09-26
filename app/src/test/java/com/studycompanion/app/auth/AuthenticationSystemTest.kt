package com.studycompanion.app.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.studycompanion.app.core.database.AppDatabase
import com.studycompanion.app.core.datastore.UserSessionDataStore
import com.studycompanion.app.data.repository.AuthRepositoryImpl
import com.studycompanion.app.data.repository.ProfileRepositoryImpl
import com.studycompanion.app.data.repository.StudyAppRepositoryImpl
import com.studycompanion.app.data.repository.TargetRepositoryImpl
import com.studycompanion.app.domain.repository.AuthState
import com.studycompanion.app.feature.onboarding.OnboardingStep
import com.studycompanion.app.feature.onboarding.OnboardingViewModel
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthenticationSystemTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var sessionDataStore: UserSessionDataStore
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var profileRepository: ProfileRepositoryImpl
    private lateinit var targetRepository: TargetRepositoryImpl
    private lateinit var studyAppRepository: StudyAppRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(testDispatcher.asExecutor())
            .setTransactionExecutor(testDispatcher.asExecutor())
            .build()
        sessionDataStore = UserSessionDataStore(context)
        authRepository = AuthRepositoryImpl(db.userDao(), sessionDataStore, null, db)
        profileRepository = ProfileRepositoryImpl(db.profileDao(), db.profileSettingsDao(), sessionDataStore)
        targetRepository = TargetRepositoryImpl(db.dailyTargetDao())
        studyAppRepository = StudyAppRepositoryImpl(db.studyAppDao(), context)
    }

    @After
    fun tearDown() = runBlocking {
        Dispatchers.resetMain()
        sessionDataStore.clearSession()
        db.close()
    }

    @Test
    fun `Test 1 - signup with new valid account succeeds`() = runTest(testDispatcher) {
        val email = "student@studycompanion.app"
        val password = "StrongPassword123!"

        val result = authRepository.signUp(email, password)
        assertTrue("Signup must succeed for valid credentials", result.isSuccess)

        val user = result.getOrThrow()
        assertEquals(email.lowercase(), user.email)

        // Verify account exists in local database with hashed password (no plaintext)
        val dbUser = db.userDao().getUserByEmail(email)
        assertNotNull("User must be persisted in database", dbUser)
        assertTrue("Salt must not be blank", dbUser!!.passwordSalt.isNotBlank())
        assertTrue("Password hash must not be blank", dbUser.passwordHash.isNotBlank())
        assertFalse("Plaintext password must not be stored", dbUser.passwordHash == password)

        // Verify AuthState is Authenticated
        val state = authRepository.authState.first()
        assertTrue("AuthState must be Authenticated", state is AuthState.Authenticated)
        assertEquals(user.id, (state as AuthState.Authenticated).user.id)
    }

    @Test
    fun `Test 2 - signup with duplicate email fails with clear error`() = runTest(testDispatcher) {
        val email = "duplicate@studycompanion.app"
        val password = "InitialPassword123"

        val firstSignUp = authRepository.signUp(email, password)
        assertTrue("First signup must succeed", firstSignUp.isSuccess)

        // Attempt second signup with exact same email (even with different case)
        val duplicateSignUp = authRepository.signUp("DUPLICATE@studycompanion.app", "DifferentPassword456")
        assertTrue("Duplicate signup must fail", duplicateSignUp.isFailure)

        val exception = duplicateSignUp.exceptionOrNull()
        assertNotNull(exception)
        assertEquals("An account with this email already exists.", exception?.message)

        // Verify the original account remains intact and was NOT overwritten
        val dbUser = db.userDao().getUserByEmail(email)
        assertNotNull(dbUser)
        assertEquals(firstSignUp.getOrThrow().id, dbUser?.id)
    }

    @Test
    fun `Test 3 - login with correct credentials succeeds`() = runTest(testDispatcher) {
        val email = "loginuser@studycompanion.app"
        val password = "ValidPassword789"

        authRepository.signUp(email, password)
        authRepository.logout()

        val loginResult = authRepository.login(email, password)
        assertTrue("Login must succeed with correct credentials", loginResult.isSuccess)

        val loggedInUser = loginResult.getOrThrow()
        assertEquals(email, loggedInUser.email)

        val authState = authRepository.authState.first()
        assertTrue("AuthState must be Authenticated after login", authState is AuthState.Authenticated)
        assertEquals(loggedInUser.id, (authState as AuthState.Authenticated).user.id)
    }

    @Test
    fun `Test 4 - login with nonexistent email fails and remains unauthenticated`() = runTest(testDispatcher) {
        val nonexistentEmail = "ghost@studycompanion.app"
        val password = "AnyPassword123"

        val loginResult = authRepository.login(nonexistentEmail, password)
        assertTrue("Login with nonexistent email must fail", loginResult.isFailure)

        val authState = authRepository.authState.first()
        assertTrue("AuthState must remain Unauthenticated", authState is AuthState.Unauthenticated)
    }

    @Test
    fun `Test 5 - login with incorrect password fails and remains unauthenticated`() = runTest(testDispatcher) {
        val email = "secureaccount@studycompanion.app"
        val correctPassword = "CorrectPassword123"
        val wrongPassword = "WrongPassword999"

        authRepository.signUp(email, correctPassword)
        authRepository.logout()

        val loginResult = authRepository.login(email, wrongPassword)
        assertTrue("Login with incorrect password must fail", loginResult.isFailure)
        assertEquals("Invalid email or password", loginResult.exceptionOrNull()?.message)

        val authState = authRepository.authState.first()
        assertTrue("AuthState must remain Unauthenticated", authState is AuthState.Unauthenticated)
    }

    @Test
    fun `Test 6 - logout transitions to unauthenticated without deleting account`() = runTest(testDispatcher) {
        val email = "persisting@studycompanion.app"
        val password = "PersistPassword123"

        val signUpResult = authRepository.signUp(email, password)
        val userId = signUpResult.getOrThrow().id

        // Create profile
        profileRepository.createProfile(userId, "Primary", "1234")

        // Perform logout
        val logoutResult = authRepository.logout()
        assertTrue(logoutResult.isSuccess)

        // Verify AuthState is Unauthenticated
        val authState = authRepository.authState.first()
        assertTrue("AuthState must be Unauthenticated after logout", authState is AuthState.Unauthenticated)

        // CRITICAL CHECK: Account and Profile MUST NOT be deleted from database!
        val dbUser = db.userDao().getUserById(userId)
        assertNotNull("User record must survive logout", dbUser)
        assertEquals(email, dbUser?.email)

        val profiles = db.profileDao().getProfilesForUser(userId)
        assertEquals("Profile record must survive logout", 1, profiles.size)
    }

    @Test
    fun `Test 7 - same account can log in again after logout`() = runTest(testDispatcher) {
        val email = "relogin@studycompanion.app"
        val password = "Password12345"

        authRepository.signUp(email, password)
        authRepository.logout()

        // Log in again
        val reloginResult = authRepository.login(email, password)
        assertTrue("User must be able to log in again with same credentials", reloginResult.isSuccess)

        val authState = authRepository.authState.first()
        assertTrue("AuthState must be Authenticated after relogin", authState is AuthState.Authenticated)
    }

    @Test
    fun `Test 8 - authenticated state and active profile survive process recreation`() = runTest(testDispatcher) {
        val email = "survivor@studycompanion.app"
        val password = "SurvivorPassword123"

        val user = authRepository.signUp(email, password).getOrThrow()
        val createdProfile = profileRepository.createProfile(user.id, "StudyHero", "4321").getOrThrow()

        // Simulate app kill / recreation by creating new repository instances with same persistent DB and DataStore
        val newAuthRepo = AuthRepositoryImpl(db.userDao(), sessionDataStore)
        val newProfileRepo = ProfileRepositoryImpl(db.profileDao(), db.profileSettingsDao(), sessionDataStore)

        val restoredAuthState = newAuthRepo.authState.first()
        assertTrue("Restored AuthState must be Authenticated", restoredAuthState is AuthState.Authenticated)
        assertEquals(user.id, (restoredAuthState as AuthState.Authenticated).user.id)

        val restoredActiveProfile = newProfileRepo.getActiveProfile().first { it != null }
        assertNotNull("Active profile must be restored across process recreation", restoredActiveProfile)
        assertEquals(createdProfile.id, restoredActiveProfile?.id)
        assertEquals("StudyHero", restoredActiveProfile?.name)
    }

    @Test
    fun `Test 9 - login in OnboardingViewModel activates profile and enables authenticated state`() = runTest(testDispatcher) {
        val email = "viewmodel@studycompanion.app"
        val password = "ViewModelPassword123"

        val user = authRepository.signUp(email, password).getOrThrow()
        val profile = profileRepository.createProfile(user.id, "ExistingStudent", "9999").getOrThrow()
        authRepository.logout()

        val viewModel = OnboardingViewModel(authRepository, profileRepository, targetRepository, studyAppRepository)
        advanceUntilIdle()

        assertEquals("Initially at AUTH step", OnboardingStep.AUTH, viewModel.uiState.value.currentStep)
        assertTrue(viewModel.uiState.value.authState is AuthState.Unauthenticated)
        assertNull(viewModel.uiState.value.activeProfile)

        // Perform login through ViewModel
        viewModel.login(email, password)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.authState is AuthState.Authenticated && it.activeProfile != null }
        assertTrue("AuthState must be Authenticated", state.authState is AuthState.Authenticated)
        assertNotNull("Active profile must be automatically restored on login", state.activeProfile)
        assertEquals(profile.id, state.activeProfile?.id)
        assertEquals("Logged in successfully", state.successMessage)
        assertNull(state.errorMessage)

        // Condition for MainActivity: isAuthenticated = authState is Authenticated && activeProfile != null
        val isAuthenticated = state.authState is AuthState.Authenticated && state.activeProfile != null
        assertTrue("MainActivity isAuthenticated condition MUST be TRUE after login", isAuthenticated)
    }

    @Test
    fun `Test 10 - login failure in OnboardingViewModel never enters authenticated state`() = runTest(testDispatcher) {
        val viewModel = OnboardingViewModel(authRepository, profileRepository, targetRepository, studyAppRepository)
        advanceUntilIdle()

        // Wrong password login
        viewModel.login("nonexistent@studycompanion.app", "wrongPass")
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.errorMessage != null }
        assertTrue("AuthState must remain Unauthenticated on failure", state.authState is AuthState.Unauthenticated)
        assertNull("Active profile must remain null on failure", state.activeProfile)
        assertEquals("Invalid email or password", state.errorMessage)
        assertNull("Success message must be null", state.successMessage)

        val isAuthenticated = state.authState is AuthState.Authenticated && state.activeProfile != null
        assertFalse("isAuthenticated condition MUST be FALSE on login failure", isAuthenticated)
    }

    @Test
    fun `Test 11 - deleteAccount deletes user, wipes all tables, clears session, and leaves system unauthenticated`() = runTest(testDispatcher) {
        val email = "delete_me@studycompanion.app"
        val password = "DeletePassword123!"

        val user = authRepository.signUp(email, password).getOrThrow()
        profileRepository.createProfile(user.id, "ProfileToDelete", "1234").getOrThrow()
        advanceUntilIdle()

        assertNotNull(db.userDao().getUserById(user.id))
        assertTrue(authRepository.authState.first() is AuthState.Authenticated)

        val deleteResult = authRepository.deleteAccount()
        assertTrue("deleteAccount must succeed", deleteResult.isSuccess)
        advanceUntilIdle()

        assertNull("User must be deleted from database", db.userDao().getUserById(user.id))
        assertTrue("AuthState must transition to Unauthenticated", authRepository.authState.first() is AuthState.Unauthenticated)
        assertNull("Session DataStore must be cleared", sessionDataStore.userIdFlow.first())
        assertNull("Session Token must be cleared", sessionDataStore.sessionTokenFlow.first())
    }
}
