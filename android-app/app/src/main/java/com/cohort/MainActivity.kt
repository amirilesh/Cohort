package com.cohort

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cohort.navigation.AppNavigation
import com.cohort.ui.theme.CohortAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CohortAndroidTheme {
                AppNavigation()
            }
        }
    }
}