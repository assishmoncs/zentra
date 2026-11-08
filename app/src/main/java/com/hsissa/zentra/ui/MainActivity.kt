package com.hsissa.zentra.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hsissa.zentra.core.SettingsManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hsissa.zentra.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val settings = SettingsManager.getInstance(applicationContext)
        settings.getDailyGoal()
        if (settings.usingFallbackPreferences && savedInstanceState == null) {
            MaterialAlertDialogBuilder(this).setMessage(R.string.settings_recovery_warning)
                .setPositiveButton(android.R.string.ok, null).show()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val navView: BottomNavigationView = findViewById(R.id.bottom_nav)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        navView.setupWithNavController(navController)
    }
}
