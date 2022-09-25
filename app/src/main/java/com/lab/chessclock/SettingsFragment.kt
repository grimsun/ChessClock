package com.lab.chessclock

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        val listPreference: ListPreference? = findPreference("orientation")
        listPreference?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
    }
}