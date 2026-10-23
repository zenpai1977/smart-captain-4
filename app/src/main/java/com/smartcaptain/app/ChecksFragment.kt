package com.smartcaptain.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class ChecksFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_checks, container, false)
        val prefs = requireContext().getSharedPreferences("SmartCaptainPrefs", Context.MODE_PRIVATE)

        val btnMonitor = view.findViewById<Button>(R.id.btnModeMonitor)
        val btnActive = view.findViewById<Button>(R.id.btnModeActive)
        val txtAccountCheck = view.findViewById<TextView>(R.id.txtCheckAccountStatus)
        val btnGoToAccount = view.findViewById<Button>(R.id.btnGoToAccount)
        val btnOpenAccess = view.findViewById<Button>(R.id.btnOpenAccessibilitySettings)

        val savedMobile = prefs.getString("mobile_number", "") ?: ""
        if (savedMobile.length == 10) {
            txtAccountCheck.text = "Account Active (+91 $savedMobile)"
        }

        btnMonitor.setOnClickListener {
            prefs.edit().putBoolean("engine_active", false).apply()
            Toast.makeText(requireContext(), "Switched to Monitor Mode", Toast.LENGTH_SHORT).show()
        }

        btnActive.setOnClickListener {
            prefs.edit().putBoolean("engine_active", true).apply()
            Toast.makeText(requireContext(), "Switched to Active Mode", Toast.LENGTH_SHORT).show()
        }

        btnGoToAccount.setOnClickListener {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav.selectedItemId = R.id.nav_account
        }

        btnOpenAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        return view
    }
}
