package com.terminalindex.gitclock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun SetupScreen(
    initialUsername: String? = null,
    initialToken: String? = null,
    firstLaunch: Boolean,
    onSave: (String, String) -> Unit,
    onDismissFirstLaunch: () -> Unit
) {
    var username by remember { mutableStateOf(initialUsername ?: "") }
    var token by remember { mutableStateOf(initialToken ?: "") }
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { }
    )
    
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (firstLaunch) {
        AlertDialog(
            onDismissRequest = onDismissFirstLaunch,
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("Welcome to GitClock!") },
            text = {
                Text("Turn your device into a beautiful GitHub desk clock.\n\n" +
                     "See your contribution graph, track PRs and Issues, and keep time in style.\n\n" +
                     "New: Enable Remote Config Server in Settings to edit layout from your Desktop (Port 8080).\n\n" +
                     "Please ensure you grant necessary permissions for the best experience.")
            },
            confirmButton = {
                TextButton(onClick = onDismissFirstLaunch) {
                    Text("Let's Go")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "GitClock",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Your GitHub Contribution Dashboard",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                if (token.isBlank()) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("GitHub Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("OAuth Token (Optional)") },
                    placeholder = { Text("Required for private repos") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
                
                if (token.isNotBlank()) {
                     Text(
                        text = "Username will be auto-detected from token",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { onSave(username, token) },
                    enabled = username.isNotBlank() || token.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(0.4f)
                ) {
                    Text("Start Clock")
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/sponsors/terminal-index"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text("Support 🤎")
                }

                TextButton(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/terminal-index"))
                        context.startActivity(intent)
                    }
                ) {
                    Text(
                        text = "Made with 🤎 by Terminal-Index",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
