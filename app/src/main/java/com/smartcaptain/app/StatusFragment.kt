package com.smartcaptain.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment

class StatusFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_status, container, false)
        val prefs = requireContext().getSharedPreferences("SmartCaptainPrefs", Context.MODE_PRIVATE)

        val btnGrant = view.findViewById<Button>(R.id.btnGrantAccessibility)
        val txtAccepted = view.findViewById<TextView>(R.id.txtAcceptedCount)
        val txtDeclined = view.findViewById<TextView>(R.id.txtDeclinedCount)
        val switchEngine = view.findViewById<Switch>(R.id.switchEngine)

        txtAccepted.text = prefs.getInt("accepted_count", 0).toString()
        txtDeclined.text = prefs.getInt("declined_count", 0).toString()
        switchEngine.isChecked = prefs.getBoolean("engine_active", false)

        switchEngine.setOnCheckedChangeListener { buttonView: android.widget.CompoundButton, isChecked: Boolean ->
            val editor = prefs.edit()
            editor.putBoolean("engine_active", isChecked)
            editor.apply()
        }

        btnGrant.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        return view
    }
}
