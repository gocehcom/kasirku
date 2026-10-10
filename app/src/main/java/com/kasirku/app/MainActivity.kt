package com.kasirku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kasirku.app.ui.navigation.KasirKuNav
import com.kasirku.app.ui.theme.KasirKuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KasirKuTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KasirKuNav()
                }
            }
        }
    }
}
