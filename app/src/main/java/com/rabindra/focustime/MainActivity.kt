package com.rabindra.focustime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.rabindra.focustime.ui.FocusTimeApp
import com.rabindra.focustime.ui.theme.FocusTimeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: FocusTimeViewModel by viewModels {
        FocusTimeViewModel.factory(application as FocusTimeApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusTimeTheme {
                FocusTimeApp(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as FocusTimeApplication).container.controller.ensureServiceRunning()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDndAccess()
    }
}
