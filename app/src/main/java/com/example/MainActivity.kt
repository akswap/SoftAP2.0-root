package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.HotspotRepository
import com.example.ui.HotspotViewModel
import com.example.ui.MainHotspotScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: HotspotViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local database and repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = HotspotRepository(database)

        // Instantiate ViewModel
        val factory = HotspotViewModel.Factory(application, repository)
        viewModel = ViewModelProvider(this, factory)[HotspotViewModel::class.java]

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            MyApplicationTheme {
                MainHotspotScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.checkAndSyncHotspotState()
        }
    }
}
