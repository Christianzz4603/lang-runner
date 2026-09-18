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

        // Always start fresh on the Terminal tab. Deliberately ignoring
        // savedInstanceState here: if the app previously crashed while a
        // different tab was open, letting FragmentManager auto-restore that
        // tab would immediately re-trigger the same crash on every relaunch.
        supportFragmentManager.fragments.forEach {
            supportFragmentManager.beginTransaction().remove(it).commitNowAllowingStateLoss()
        }
        showFragment(terminalFragment, "terminal", "Terminal")

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_terminal -> showFragment(terminalFragment, "terminal", "Terminal")
                R.id.nav_import -> showFragment(importFragment, "import", "Import")
                R.id.nav_settings -> showFragment(settingsFragment, "settings", "Settings")
            }
            true
        }
    }

    private fun showFragment(fragment: Fragment, tag: String, title: String) {
        binding.topAppBar.title = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .commit()
    }

    fun switchToTerminalTab() {
        binding.bottomNav.selectedItemId = R.id.nav_terminal
    }
}
