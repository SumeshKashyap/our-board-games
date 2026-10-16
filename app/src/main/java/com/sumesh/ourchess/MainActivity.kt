package com.sumesh.ourchess

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sumesh.ourchess.engine.Engines
import com.sumesh.ourchess.ui.App
import com.sumesh.ourchess.ui.theme.OurChessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Engines.init(applicationContext)
        // Don't let the screen dim while someone is thinking about their move.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            OurChessTheme {
                App()
            }
        }
    }
}
