package com.informatlux.test

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // --- Add Logic to Individual Preferences ---

        // Theme Preference Logic
        val themePreference: ListPreference? = findPreference("key_theme")
        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            when (newValue as String) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            true
        }

        // Clear Cache Preference Logic
        val clearCachePreference: Preference? = findPreference("key_clear_cache")
        clearCachePreference?.setOnPreferenceClickListener {
            showClearCacheConfirmationDialog()
            true
        }

        // Rate Us Preference Logic
        val rateUsPreference: Preference? = findPreference("key_rate_us")
        rateUsPreference?.setOnPreferenceClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireContext().packageName}"))
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to web browser if Play Store is not available
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}"))
                startActivity(intent)
            }
            true
        }

        // Set App Version Programmatically
        val appVersionPreference: Preference? = findPreference("key_app_version")
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val version = pInfo.versionName
            appVersionPreference?.summary = version
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showClearCacheConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Cache")
            .setMessage("Are you sure you want to clear all temporary data? This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                // TODO: Add your actual cache clearing logic here
                // For example: context.cacheDir.deleteRecursively()
                Toast.makeText(requireContext(), "Cache cleared successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}