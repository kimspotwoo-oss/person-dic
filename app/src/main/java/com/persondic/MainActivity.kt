package com.persondic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.persondic.ui.nav.PersonDicNavHost
import com.persondic.ui.theme.PersonDicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonDicTheme {
                PersonDicNavHost()
            }
        }
    }
}
