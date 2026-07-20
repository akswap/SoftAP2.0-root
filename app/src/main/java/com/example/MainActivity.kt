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

        setContent {
            MyApplicationTheme {
                MainHotspotScreen(viewModel = viewModel)
            }
        }
    }
}
