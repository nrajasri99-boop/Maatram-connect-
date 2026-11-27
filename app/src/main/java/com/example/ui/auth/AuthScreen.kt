package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.UserRole
import com.example.ui.theme.MaatramBlue
import com.example.ui.theme.MaatramBlueContainer
import com.example.ui.theme.MaatramBlueSoft
import com.example.ui.theme.MaatramNavy
import com.example.ui.theme.MaatramOutline
import com.example.ui.theme.MaatramRed
import com.example.ui.theme.MaatramRedSoft
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class LoginRoleOption(val label: String, val userRole: UserRole) {
    STUDENT("Student", UserRole.VOLUNTEER),
    OFFICE_TEAM("Office Team", UserRole.OFFICE_TEAM),
    ADMIN("Admin", UserRole.ADMIN)
}

private const val STUDENT_SAMPLE_EMAIL = "student@maatram.org"
private const val STUDENT_SAMPLE_PASSWORD = "student123"
private const val OFFICE_SHARED_EMAIL = "office@maatram.org"
private const val OFFICE_SHARED_PASSWORD = "office123"
private const val ADMIN_EMAIL = "nrajasri99@gmail.com"
private const val ADMIN_PASSWORD = "admin123"

private fun defaultCredentialsFor(role: LoginRoleOption): Pair<String, String> = when (role) {
    LoginRoleOption.STUDENT -> STUDENT_SAMPLE_EMAIL to STUDENT_SAMPLE_PASSWORD
    LoginRoleOption.OFFICE_TEAM -> OFFICE_SHARED_EMAIL to OFFICE_SHARED_PASSWORD
    LoginRoleOption.ADMIN -> ADMIN_EMAIL to ADMIN_PASSWORD
}

fun ensureFirebaseAuth(context: Context): FirebaseAuth? {
    return try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        if (FirebaseApp.getApps(context).isNotEmpty()) Firebase.auth else null
    } catch (e: Exception) {
        Log.w("Auth", "Firebase initialization unavailable: ${e.message}")
        null
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val auth = ensureFirebaseAuth(context)
    if (auth == null) {
        onUnauthenticated()
        return
    }
    if (auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val activity = context as? Activity
    if (activity == null) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val auth = ensureFirebaseAuth(context)
    if (auth == null) {
        onAuthError("Google Sign-In is unavailable on this device. Please sign in with Email ID and Password above.")
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In is unavailable on this device. Please sign in with Email ID and Password above.")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Google Sign-In requires an Activity context.")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError("Could not complete Google Sign-In on this device. Please sign in with Email ID and Password above.")
        }
    }
}

fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                try {
                    Firebase.auth.signOut()
                } catch (_: Exception) {
                }
                onSignOutComplete()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onEmailPasswordSignIn: (name: String, email: String, district: String, role: UserRole) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedRole by remember { mutableStateOf(LoginRoleOption.STUDENT) }
    var emailInput by remember { mutableStateOf(STUDENT_SAMPLE_EMAIL) }
    var passwordInput by remember { mutableStateOf(STUDENT_SAMPLE_PASSWORD) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MaatramBlueSoft, Color.White, Color.White)
                )
            )
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        val isCompact = maxWidth < 360.dp

        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isCompact) 16.dp else 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaatramBlue,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "MC",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "MAATRAM CONNECT",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaatramNavy
                )
                Text(
                    text = "Simple Sign In",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaatramOutline),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Select Role",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaatramNavy
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LoginRoleOption.entries.forEach { roleOption ->
                            FilterChip(
                                selected = selectedRole == roleOption,
                                onClick = {
                                    selectedRole = roleOption
                                    val (sampleEmail, samplePass) = defaultCredentialsFor(roleOption)
                                    emailInput = sampleEmail
                                    passwordInput = samplePass
                                    errorMessage = null
                                },
                                label = {
                                    Text(
                                        text = roleOption.label,
                                        fontWeight = if (selectedRole == roleOption) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaatramBlueContainer,
                                    selectedLabelColor = MaatramNavy
                                ),
                                modifier = Modifier.testTag("login_role_${roleOption.name.lowercase()}")
                            )
                        }
                    }

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedBorderColor = MaatramBlue,
                        unfocusedBorderColor = MaatramOutline,
                        focusedLabelColor = MaatramBlue,
                        unfocusedLabelColor = Color.DarkGray
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            errorMessage = null
                        },
                        label = { Text("Email ID") },
                        placeholder = {
                            Text(
                                when (selectedRole) {
                                    LoginRoleOption.STUDENT -> STUDENT_SAMPLE_EMAIL
                                    LoginRoleOption.OFFICE_TEAM -> OFFICE_SHARED_EMAIL
                                    LoginRoleOption.ADMIN -> ADMIN_EMAIL
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = MaatramBlue)
                        },
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input")
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            errorMessage = null
                        },
                        label = { Text("Password") },
                        placeholder = { Text("Enter password") },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaatramBlue)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        colors = fieldColors,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    Surface(
                        color = MaatramBlueSoft,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = when (selectedRole) {
                                LoginRoleOption.STUDENT ->
                                    "Demo Student Login: $STUDENT_SAMPLE_EMAIL  /  $STUDENT_SAMPLE_PASSWORD (or enter your email)"
                                LoginRoleOption.OFFICE_TEAM ->
                                    "Office Team Shared Login: $OFFICE_SHARED_EMAIL  /  $OFFICE_SHARED_PASSWORD"
                                LoginRoleOption.ADMIN ->
                                    "Admin Login: $ADMIN_EMAIL  /  $ADMIN_PASSWORD"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaatramNavy,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = MaatramRedSoft,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaatramRed,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val trimmedEmail = emailInput.trim()
                            val pass = passwordInput
                            when (selectedRole) {
                                LoginRoleOption.STUDENT -> {
                                    if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
                                        errorMessage = "Please enter a valid student Email ID."
                                    } else if (pass.length < 4) {
                                        errorMessage = "Password must be at least 4 characters."
                                    } else {
                                        val rawName = trimmedEmail.substringBefore("@")
                                            .replace('.', ' ')
                                            .replace('_', ' ')
                                            .trim()
                                            .split(" ")
                                            .filter { it.isNotBlank() }
                                            .joinToString(" ") { part ->
                                                part.replaceFirstChar { ch -> ch.uppercase() }
                                            }
                                            .ifBlank { "Maatram Student" }
                                        onEmailPasswordSignIn(
                                            rawName,
                                            trimmedEmail,
                                            "Tiruchirappalli",
                                            UserRole.VOLUNTEER
                                        )
                                    }
                                }
                                LoginRoleOption.OFFICE_TEAM -> {
                                    if (!trimmedEmail.equals(OFFICE_SHARED_EMAIL, ignoreCase = true) || pass != OFFICE_SHARED_PASSWORD) {
                                        errorMessage = "Invalid Office Team credentials. Use $OFFICE_SHARED_EMAIL and $OFFICE_SHARED_PASSWORD."
                                    } else {
                                        onEmailPasswordSignIn(
                                            "Maatram Office Team",
                                            OFFICE_SHARED_EMAIL,
                                            "Tiruchirappalli",
                                            UserRole.OFFICE_TEAM
                                        )
                                    }
                                }
                                LoginRoleOption.ADMIN -> {
                                    if (!trimmedEmail.equals(ADMIN_EMAIL, ignoreCase = true) || pass != ADMIN_PASSWORD) {
                                        errorMessage = "Invalid Admin credentials. Use $ADMIN_EMAIL and $ADMIN_PASSWORD."
                                    } else {
                                        onEmailPasswordSignIn(
                                            "Maatram Admin",
                                            ADMIN_EMAIL,
                                            "Chennai",
                                            UserRole.ADMIN
                                        )
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaatramBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_submit_button")
                    ) {
                        Text(
                            text = "Sign In",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaatramOutline)
                        Text(
                            text = "or",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaatramOutline)
                    }

                    OutlinedButton(
                        onClick = {
                            errorMessage = null
                            isGoogleLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isGoogleLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isGoogleLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isGoogleLoading = false
                                    errorMessage = "Google Sign-In cancelled."
                                }
                            )
                        },
                        enabled = !isGoogleLoading,
                        border = BorderStroke(1.5.dp, MaatramBlue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaatramBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaatramBlue,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.sign_in_google),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
