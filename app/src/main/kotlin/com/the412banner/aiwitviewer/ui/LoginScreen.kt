package com.the412banner.aiwitviewer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    initialEmail: String = "",
    isWorking: Boolean,
    errorText: String?,
    onSubmit: (email: String, password: String) -> Unit,
) {
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("AIWIT Viewer", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Log in with the same email + password you use in the AIWIT (Kemo Pro) app.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPwd = !showPwd }) {
                    Icon(
                        if (showPwd) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showPwd) "Hide password" else "Show password",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (errorText != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onSubmit(email.trim(), password) },
            enabled = !isWorking && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isWorking) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text("Log in")
            }
        }
    }
}
