package com.mentra.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.mentra.app.utils.UsagePermissionHelper
import com.mentra.telemetry.TelemetryService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            var showPermissionDialog by remember { mutableStateOf(!UsagePermissionHelper.hasUsageStatsPermission(this)) }
            
            // Show permission dialog if needed
            if (showPermissionDialog) {
                UsageAccessPermissionDialog(
                    onConfirm = {
                        startActivity(UsagePermissionHelper.createUsageAccessSettingsIntent())
                        showPermissionDialog = false
                    },
                    onDismiss = { showPermissionDialog = false }
                )
            }
            
            // Start telemetry service if we have permission
            if (UsagePermissionHelper.hasUsageStatsPermission(this)) {
                startService(TelemetryService.createIntent(this))
            }
            
            // TODO: Add main app content here
        }
    }
}

@Composable
fun UsageAccessPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.usage_access_required)) },
        text = { Text(stringResource(R.string.usage_access_explanation)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
} 