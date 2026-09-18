package com.langrunner.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.langrunner.app.databinding.ActivityMainBinding
import com.langrunner.app.ui.ImportFragment
import com.langrunner.app.ui.SettingsFragment
import com.langrunner.app.ui.TerminalFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val terminalFragment = TerminalFragment()
    private val importFragment = ImportFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            showFragment(terminalFragment, "terminal")
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_terminal -> showFragment(terminalFragment, "terminal")
                R.id.nav_import -> showFragment(importFragment, "import")
                R.id.nav_settings -> showFragment(settingsFragment, "settings")
            }
            true
        }
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .commit()
    }

    fun switchToTerminalTab() {
        binding.bottomNav.selectedItemId = R.id.nav_terminal
    }
}
