package com.simplesms.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.simplesms.app.ui.screens.MainScreen
import com.simplesms.app.ui.theme.SimpleSMSTheme
import com.simplesms.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SimpleSMSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (com.simplesms.app.remote.WebGatewayManager.isGatewayEnabled(this)) {
            com.simplesms.app.remote.WebGatewayManager.startGateway(this)
        }
    }
}

@Composable
fun AppContent(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasAllPermissions by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val telephonyGranted = (results[Manifest.permission.SEND_SMS] == true) &&
                (results[Manifest.permission.READ_PHONE_STATE] == true)
        val contactsGranted = results[Manifest.permission.READ_CONTACTS] == true
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            results[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true

        hasAllPermissions = telephonyGranted && contactsGranted && notifGranted
        viewModel.refreshData()
        if (com.simplesms.app.remote.WebGatewayManager.isGatewayEnabled(context)) {
            com.simplesms.app.remote.WebGatewayManager.startGateway(context)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions)
    }

    MainScreen(
        viewModel = viewModel,
        onRequestPermissions = { permissionLauncher.launch(requiredPermissions) },
        hasAllPermissions = hasAllPermissions
    )
}
